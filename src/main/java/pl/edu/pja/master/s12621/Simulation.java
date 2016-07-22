package pl.edu.pja.master.s12621;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.ExponentialGenerator;
import pl.edu.pja.master.s12621.config.SimulationConfig;

import javax.annotation.PostConstruct;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by bartosz.drabik
 */
public class Simulation implements InitializingBean, SimulationMBean {

	private static final Logger LOG = LoggerFactory.getLogger(Simulation.class);

	private static final String NUMBER_FORMAT = "%6d";
	private static final String DELTA_FORMAT = "%+4d";

	@Autowired
	private DataSource connectionPool;

	private final String configPropertiesFile = "/config/app.simulation.properties";

	@Autowired
	private SimulationConfig simulationConfig;


	@PostConstruct
	private void postConstruct(){
		this.simulationConfig.setPropertiesPath(Paths.get(configPropertiesFile));
	}

	private ExponentialGenerator expGen;

	private ScheduledThreadPoolExecutor executor;

	private volatile long successfulRequests = 0;
	private volatile long failedRequests = 0;
	private volatile long droppedRequests = 0;
	private volatile long leakedConnections = 0;
	private String lastException;

	public void afterPropertiesSet() throws Exception {
		NumberGenerator<Double> mean = new NumberGenerator<Double>() {
			public Double nextValue() {
				return simulationConfig.readProperty("requestsPerSecond", Double.class);
			}
		};
		expGen = new ExponentialGenerator(mean, new Random());
		simulationConfig.registerConfigurationChangesListener(configPropertiesFile, this);
		registerMbean();
		start();
	}

	private void start() {
		LOG.info("Starting simulation..."+connectionPool.getClass());

		executor = new ScheduledThreadPoolExecutor(simulationConfig.readProperty("executorThreadPoolCoreSize", Integer.class), new RejectedExecutionHandler() {
			public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
				LOG.error("Thread limit exceeded.");
			}
		});

		// logging
		executor.execute(new Runnable() {

			public void run() {
				long lastSuccess = 0;
				long lastFailed = 0;
				long successDelta = 0;
				long failedDelta = 0;
				while (!Thread.interrupted()) {
					synchronized (Simulation.class) {
						successDelta = successfulRequests - lastSuccess;
						failedDelta = failedRequests - lastFailed;
						LOG.info("Success : " + String.format(NUMBER_FORMAT, successfulRequests) + " ("
								+ String.format(DELTA_FORMAT, successDelta) + ") | Failed : " + String.format(NUMBER_FORMAT, failedRequests)
								+ " (" + String.format(DELTA_FORMAT, failedDelta) + ") | Queued : "
								+ String.format("%5d", executor.getQueue().size()) + " | Active : "
								+ String.format("%5d", executor.getActiveCount()) + " | Dropped : "
								+ String.format(NUMBER_FORMAT, droppedRequests));
						lastSuccess = successfulRequests;
						lastFailed = failedRequests;
						if (lastException != null) {
							LOG.warn("Last Exception : " + lastException);
							lastException = null;
						}
					}
					try {
						Thread.sleep(simulationConfig.readProperty("logIntervalSeconds", Integer.class) * 1000);
					}
					catch (InterruptedException e) {
						LOG.info("Thread interrupted, stopping periodic logging...");
					}
				}
			}
		});

		Random rand = new Random();
		double deviation;
		double responseTime;

		// generate DB queries
		while (!Thread.interrupted()) {
			deviation = rand.nextGaussian() * simulationConfig.readProperty("responseTimeStandardDeviation", Double.class);
			responseTime = simulationConfig.readProperty("responseTimeAverage", Double.class) + deviation;
			// take out possible extreme values
			if (responseTime < 0.00001 || responseTime > 10) {
				responseTime = simulationConfig.readProperty("responseTimeAverage", Double.class);
			}

			if (executor.getQueue().size() < simulationConfig.readProperty("dropRequestsThreshold", Integer.class)) {
				executor.execute(new DatabaseRequest(responseTime));
			} else {
				droppedRequests++;
			}

			double waitTime = expGen.nextValue();
			try {
				LOG.debug("Waiting " + waitTime + " seconds until next event.");
				Thread.sleep(Math.round(waitTime * 1000));
			}
			catch (InterruptedException e) {
				LOG.info("Simulation thread interrupted, terminating...");
				return;
			}
		}
	}

	private class DatabaseRequest implements Runnable {

		final double responseTime;

		public DatabaseRequest(double responseTime) {
			super();
			this.responseTime = responseTime;
		}

		public void run() {

			Connection conn = null;
			// request a connection from the pool and perform a DB request
			try {
				LOG.debug("Getting a connection");
				conn = connectionPool.getConnection();

				LOG.debug(String.format("Performing a request that should take %s seconds", responseTime));
				CallableStatement call = conn.prepareCall("call DBMS_LOCK.SLEEP(?)");
				call.setDouble(1, responseTime);
				call.execute();
				call.close();
				successfulRequests++;
				LOG.debug("Number of successful requests: " + successfulRequests);
			}
			catch (SQLException e) {
				failedRequests++;
				lastException = e.getMessage();
				LOG.error("SQL exception while performing a db request: ", e);
			}
			finally {
				if (conn != null) {
					try {
						conn.close();
					}
					catch (Exception ignore) {
						LOG.info("Exception while closing db connection in final block");
						LOG.info(ignore.getMessage());
					}
				}
			}
		}
	}

	public void leakConnection() {
		LOG.info("Simulating leaking a connection");
		executor.execute(new Runnable() {

			public void run() {
				try {
					connectionPool.getConnection();
					leakedConnections++;
					// sleep 10 hours
					Thread.sleep(10 * 60 * 60 * 1000);
				} catch (SQLException e) {
					LOG.info("Leaked connection received SQLException", e);
				} catch (InterruptedException e) {
					LOG.info("Leaked connection thread interrupted");
				}
				leakedConnections--;
			}
		});
	}

	private void registerMbean() throws Exception {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		ObjectName name = new ObjectName("pl.edux.pjwstk.it.platform.test.jdbc.pool:type=Simulation");
		mbs.registerMBean(this, name);
	}

	public long getNumSuccess() {
		return successfulRequests;
	}

	public long getNumFailed() {
		return failedRequests;
	}

	public long getNumQueued() {
		return executor.getQueue().size();
	}

	public long getNumDropped() {
		return droppedRequests;
	}

	public long getNumLeaked() {
		return leakedConnections;
	}

	public long getNumActive() {
		return executor.getActiveCount();
	}

}
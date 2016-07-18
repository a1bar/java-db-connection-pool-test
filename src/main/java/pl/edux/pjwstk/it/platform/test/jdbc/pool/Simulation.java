package pl.edux.pjwstk.it.platform.test.jdbc.pool;

import java.lang.management.ManagementFactory;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import pl.edux.pjwstk.it.platform.test.jdbc.pool.config.SimulationConfig;
import pl.edux.pjwstk.it.platform.test.jdbc.pool.config.SimulationConfigImpl;
import pl.edux.pjwstk.it.platform.test.jdbc.pool.config.SimulationConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.uncommons.maths.number.NumberGenerator;
import org.uncommons.maths.random.ExponentialGenerator;

// this simulation requires a running oracle database
// ie : docker run -d -p 1521:1521 wnameless/oracle-xe-11g
public class Simulation implements InitializingBean, SimulationMBean, SimulationConfigListener {

	private static final Logger logger = LoggerFactory.getLogger(Simulation.class);

	private static final int MAX_THREADS = 400;

	private static final String NUMBER_FORMAT = "%6d";
	private static final String DELTA_FORMAT = "%+4d";

	@Autowired
	private DataSource connectionPool;

	private SimulationConfig config = new SimulationConfigImpl();

	// for Poisson distribution simulation
	private NumberGenerator<Double> mean;
	private ExponentialGenerator expGen;

	private ScheduledThreadPoolExecutor executor;

	private volatile long successfulRequests = 0;
	private volatile long failedRequests = 0;
	private volatile long droppedRequests = 0;
	private volatile long leakedConnections = 0;
	private String lastException;

	public void afterPropertiesSet() throws Exception {
		mean = new NumberGenerator<Double>() {
			public Double nextValue() {
				return config.readProperty("app.simulation", "requestsPerSecond", Double.class);
			}
		};
		expGen = new ExponentialGenerator(mean, new Random());
		config.registerConfigurationChangesListener("app.simulation", this);
		registerMbean();
		start();
	}

	private void start() {
		logger.info("Starting simulation...");

		executor = new ScheduledThreadPoolExecutor(MAX_THREADS, new RejectedExecutionHandler() {

			public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
				logger.error("Thread limit exceeded.");
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
						logger.info("Succes : " + String.format(NUMBER_FORMAT, successfulRequests) + " ("
								+ String.format(DELTA_FORMAT, successDelta) + ") | Failed : " + String.format(NUMBER_FORMAT, failedRequests)
								+ " (" + String.format(DELTA_FORMAT, failedDelta) + ") | Queued : "
								+ String.format("%5d", executor.getQueue().size()) + " | Active : "
								+ String.format("%5d", executor.getActiveCount()) + " | Dropped : "
								+ String.format(NUMBER_FORMAT, droppedRequests));
						lastSuccess = successfulRequests;
						lastFailed = failedRequests;
						if (lastException != null) {
							logger.warn("Last Exception : " + lastException);
							lastException = null;
						}
					}
					try {
						Thread.sleep(config.readProperty("app.simulation", "logIntervalSeconds", Integer.class) * 1000);
					}
					catch (InterruptedException e) {
						logger.info("Thread interrupted, stopping periodic logging...");
					}
				}
			}
		});

		Random rand = new Random();
		double deviation;
		double responseTime;

		// generate DB queries
		while (!Thread.interrupted()) {
			deviation = rand.nextGaussian() * config.readProperty("app.simulation", "responseTimeStandardDeviation", Double.class);
			responseTime = config.readProperty("app.simulation", "responseTimeAverage", Double.class) + deviation;
			// take out possible extreme values
			if (responseTime < 0.00001 || responseTime > 10) {
				responseTime = config.readProperty("app.simulation", "responseTimeAverage", Double.class);
			}

			if (executor.getQueue().size() < config.readProperty("app.simulation", "dropRequestsTreshold", Integer.class)) {
				executor.execute(new databaseRequest(responseTime));
			} else {
				droppedRequests++;
			}

			double waitTime = expGen.nextValue();
			try {
				logger.debug("Waiting " + waitTime + " seconds until next event.");
				Thread.sleep(Math.round(waitTime * 1000));
			}
			catch (InterruptedException e) {
				logger.info("Simulation thread interrupted, terminating...");
				return;
			}
		}
	}

	private class databaseRequest implements Runnable {

		double responseTime;

		public databaseRequest(double responseTime) {
			super();
			this.responseTime = responseTime;
		}

		public void run() {

			Connection conn = null;
			// request a connection from the pool and perform a DB request
			try {
				logger.debug("getting a connection");
				conn = connectionPool.getConnection();

				logger.debug("doing request that should take " + responseTime + " seconds");
				CallableStatement call = conn.prepareCall("call DBMS_LOCK.SLEEP(?)");
				call.setDouble(1, responseTime);
				call.execute();
				call.close();
				successfulRequests++;
				logger.debug("requests done : " + successfulRequests);
			}
			catch (SQLException e) {
				failedRequests++;
				lastException = e.getMessage();
				logger.debug("SQL EXCEPTION : ", e);
			}
			finally {
				if (conn != null) {
					try {
						conn.close();
					}
					catch (Exception ignore) {
						logger.info("Exception while closing db connection in final block");
						logger.info(ignore.getMessage());
					}
				}
			}
		}
	}

	public void onAppConfigUpdated() {
		int threadPoolSize = config.readProperty("app.simulation", "executorThreadPoolCoreSize", Integer.class);
		if (executor.getCorePoolSize() != threadPoolSize) {
			executor.setCorePoolSize(threadPoolSize);
		}
	}

	public void leakConnection() {
		logger.info("leaking a connection");
		executor.execute(new Runnable() {

			public void run() {
				try {
					connectionPool.getConnection();
					leakedConnections++;
					// sleep 10 hours
					Thread.sleep(10 * 60 * 60 * 1000);
				}
				catch (SQLException e) {
					logger.info("Leaked connection received SQLException");
				}
				catch (InterruptedException e) {
					logger.info("Leaked connection thread interrupted");
				}
				leakedConnections--;
			}
		});
	}

	public void registerMbean() throws Exception {
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

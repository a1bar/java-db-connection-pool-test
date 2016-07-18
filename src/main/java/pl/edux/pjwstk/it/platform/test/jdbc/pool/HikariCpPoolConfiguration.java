package pl.edux.pjwstk.it.platform.test.jdbc.pool;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class HikariCpPoolConfiguration {

	@Value("${datasource.test.common > maxPoolSize}")
	public int maxPoolSize;

	@Value("${datasource.test.common > urls}")
	public String url;

	@Value("${datasource.test.common > username}")
	public String username;

	@Value("${datasource.test.common > password}")
	public String password;

	@Value("${datasource.test.common > getConnectionTimeout}")
	public long connectionTimeout;

	public Properties getConnectionProperties() {
		Properties props = new Properties();
		//props.setProperty("oracle.jdbc.ReadTimeout", "1000");
		//props.put("oracle.net.CONNECT_TIMEOUT", 1001);
		return props;
	}

	public HikariConfig getHikariConfig() {
		HikariConfig config = new HikariConfig();

		config.setPoolName("datasource.test.hikari");
		config.setDataSourceClassName("oracle.jdbc.pool.OracleDataSource");
		config.setMaximumPoolSize(maxPoolSize);
		config.setConnectionTimeout(connectionTimeout);
		config.setConnectionTestQuery("SELECT 1 from dual");
		config.setInitializationFailFast(true);
		config.setLeakDetectionThreshold(10000);
		config.setRegisterMbeans(true);
		config.addDataSourceProperty("connectionProperties", getConnectionProperties());
		//config.addDataSourceProperty(OracleConnection.CONNECTION_PROPERTY_THIN_NET_CONNECT_TIMEOUT, 1001);
		config.addDataSourceProperty("url", url);
		config.addDataSourceProperty("user", username);
		config.addDataSourceProperty("password", password);
		config.addDataSourceProperty("loginTimeout", "1000");

		return config;
	}

	@Bean
	public HikariDataSource getHikariDataSource() {
		return new HikariDataSource(getHikariConfig());
	}

}

package pl.edu.pja.master.s12621;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Created by bartosz.drabik
 */
@Configuration
public class HikariCpPoolConfiguration {

	@Value("${datasource.maxPoolSize}")
	public int maxPoolSize;

	@Value("${datasource.url}")
	public String url;

	@Value("${datasource.username}")
	public String username;

	@Value("${datasource.password}")
	public String password;

	@Value("${datasource.connectionTimeout}")
	public long connectionTimeout;

	public HikariConfig getHikariConfig() {
		HikariConfig config = new HikariConfig();

		config.setPoolName("datasource.test.hikari");
		config.setDataSourceClassName("oracle.jdbc.pool.OracleDataSource");
		config.setMaximumPoolSize(maxPoolSize);
		config.setConnectionTimeout(connectionTimeout);
		config.setConnectionTestQuery("SELECT 1 from dual");
		config.setInitializationFailFast(true);
		config.setRegisterMbeans(true);
		config.addDataSourceProperty("url", url);
		config.addDataSourceProperty("user", username);
		config.addDataSourceProperty("password", password);
		return config;
	}

	@Bean
	public HikariDataSource getHikariDataSource() {
		return new HikariDataSource(getHikariConfig());
	}

}

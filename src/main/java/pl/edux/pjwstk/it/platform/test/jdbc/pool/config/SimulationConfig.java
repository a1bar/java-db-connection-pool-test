package pl.edux.pjwstk.it.platform.test.jdbc.pool.config;

import pl.edux.pjwstk.it.platform.test.jdbc.pool.Simulation;

/**
 * Created by admin on 08/02/16.
 */
public interface SimulationConfig {

     <T> T readProperty(String s, String requestsPerSecond, Class<T> clazz);

    void registerConfigurationChangesListener(String s, Simulation simulation);
}

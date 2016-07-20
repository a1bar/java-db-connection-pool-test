package pl.edux.pjwstk.it.platform.test.jdbc.pool.config;

import pl.edux.pjwstk.it.platform.test.jdbc.pool.Simulation;

import java.nio.file.Path;

/**
 * Created by admin on 08/02/16.
 */
public interface SimulationConfig {

    void setPropertiesPath(Path propertiesPath);

    <T> T readProperty(String property, Class<T> clazz);

    void registerConfigurationChangesListener(String s, Simulation simulation);
}

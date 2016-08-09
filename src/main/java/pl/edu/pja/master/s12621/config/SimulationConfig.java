package pl.edu.pja.master.s12621.config;

import java.nio.file.Path;

/**
 * Created by bartosz.drabik
 */
public interface SimulationConfig {

    void setPropertiesPath(Path propertiesPath);

    <T> T readProperty(String property, Class<T> clazz);
}

package pl.edu.pja.master.s12621.config;

import pl.edu.pja.master.s12621.Simulation;

import java.nio.file.Path;

/**
 * Created by bartosz.drabik
 */
public interface SimulationConfig {

    void setPropertiesPath(Path propertiesPath);

    <T> T readProperty(String property, Class<T> clazz);

    void registerConfigurationChangesListener(String s, Simulation simulation);
}

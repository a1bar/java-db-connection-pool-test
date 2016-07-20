package pl.edux.pjwstk.it.platform.test.jdbc.pool.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.edux.pjwstk.it.platform.test.jdbc.pool.Simulation;
import pl.edux.pjwstk.it.platform.test.jdbc.pool.utils.ObjectConverter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Created by admin on 08/02/16.
 */
@Component
public class SimulationConfigImpl implements SimulationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SimulationConfigImpl.class);
    private Path propertiesPath;
    private Properties properties;


    public void setPropertiesPath(Path propertiesPath) {
        this.propertiesPath = propertiesPath;
        initProperties();
    }

    private void initProperties() {
        this.properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream(propertiesPath.toString()));
        } catch (IOException e) {
            LOG.error("Exception while reading properties from given path!", e);
        }
    }



    public <T> T readProperty(String property, Class<T> clazz) {
        return ObjectConverter.convert(properties.getProperty(property),clazz);
    }

    public void registerConfigurationChangesListener(String s, Simulation simulation) {
//TODO?
    }
}

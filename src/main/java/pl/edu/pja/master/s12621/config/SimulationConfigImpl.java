package pl.edu.pja.master.s12621.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.edu.pja.master.s12621.utils.ObjectConverter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Created by bartosz.drabik
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
}

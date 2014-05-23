package input;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;

public class PropLoader {
    public static Properties loadFromFile(String propsPath) {
        return loadFromFile(propsPath, null);
    }

    public static Properties loadFromFile(String propsPath, Logger logger) {
        Properties props = new Properties();
        BufferedReader propReader = null;
        try {
            propReader = new BufferedReader(new FileReader(propsPath));
            props.load(propReader);
        } catch (IOException io) {
            if (logger != null) {
                logger.warn(io);
            }
        } finally {
            if(propReader != null) try {
                propReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return props;
    }
}

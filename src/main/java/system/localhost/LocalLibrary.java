package system.localhost;

import interstitial.Library;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class LocalLibrary extends Library {
    public static final int port = 8000;
    public static final String jshome = "src/javascript";

    public LocalLibrary(Properties properties) {
        super.props = properties;
    }

    public LocalLibrary(String propertiesURL) {
        if (propertiesURL == null)
            init();
        else {
            try {
                super.props = new Properties();
                super.props.load(new FileReader(propertiesURL));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public LocalLibrary(){
        super.props = new Properties();
        init();
    }

    @Override
    public String getActionForm() {
        return "";
    }

    @Override
    public void init() {
        try{
            super.props.load(new FileReader(Library.PARAMS));
        }catch(IOException io){
            LOGGER.fatal(io);
            System.err.println(io.getMessage());
            System.exit(1);
        }
    }
}

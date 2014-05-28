package system.localhost;

import interstitial.Library;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class LocalLibrary extends Library {
    public static final int port = 8000;
    public static final String jshome = "src/javascript";

    public LocalLibrary(Properties properties) {
        super();
        super.props = properties;
    }

    public LocalLibrary(){
        super();
        init();
    }

    @Override
    public String getActionForm() {
        return "";
    }

    @Override
    public void init() {
        try{
            super.props.load(new FileReader(super.PARAMS));
        }catch(IOException io){
            LOGGER.fatal(io);
            System.err.println(io.getMessage());
            System.exit(1);
        }
    }
}

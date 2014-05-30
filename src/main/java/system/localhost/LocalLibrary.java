package system.localhost;

import interstitial.Library;
import util.Printer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class LocalLibrary extends Library {
    public static final int port = 8000;
    public static final String jshome = "src/javascript";

    public LocalLibrary(String propertiesURL) {
        if (propertiesURL == null || propertiesURL.equals(""))
            init();
        else {
            try {
                super.props = new Properties();
                super.props.load(new FileReader(propertiesURL));
            } catch (FileNotFoundException e) {
                LOGGER.warn(e);
                Printer.println(e.getLocalizedMessage()+"\nUsing default value instead...");
                init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getActionForm() {
        return "";
    }

    @Override
    public void init() {
        try{
            super.props = new Properties();
            super.props.load(new FileReader(Library.PARAMS));
        }catch(IOException io){
            LOGGER.fatal(io);
            System.err.println(io.getMessage());
            System.exit(1);
        }
    }
}

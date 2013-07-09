package system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Library {

    public static Properties props = new Properties();

    public static final String fileSep = File.separator;
    public static final String DIR = System.getProperty("user.home") + fileSep + ".surveyman";
    public static final String CONFIG = DIR + fileSep + "config";
    public static final String OUTDIR = DIR + fileSep + "output";
    public static final String PARAMS = DIR + fileSep + "params.properties";

    static {
        try {
            // load up the properties file
            props.load(new BufferedReader(new FileReader(PARAMS)));
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
    }
}
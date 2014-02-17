package system;

import java.io.*;
import java.util.Properties;
import org.apache.log4j.Logger;

public class Library {

    public enum JobStatus { CANCELLED, INTERRUPTED, COMPLETED; }

    public Properties props = new Properties();
    private static final Logger LOGGER = Logger.getLogger("system");

    public static final String fileSep = File.separator;

    // local configuration information
    public static final String DIR = System.getProperty("user.home") + fileSep + "surveyman";
    public static final String CONFIG = DIR + fileSep + "config";
    public static final String OUTDIR = "output";
    public static final String PARAMS = DIR + fileSep + "params.properties";

    // resources
    public static final String HTMLSKELETON = String.format("resources%sHTMLSkeleton.html", fileSep);
    public static final String JSSKELETON = String.format("resources%sJSSkeleton.js", fileSep);
    public static final String QUOTS = String.format("resources%squots", fileSep);
    public static final String XMLSKELETON = String.format("resources%sXMLSkeleton.xml", fileSep);

    // state/session/job information
    public static final String UNFINISHED_JOB_FILE = Library.DIR + Library.fileSep + ".unfinished";
    public static final String TIME = String.valueOf(System.currentTimeMillis());
    public static final String STATEDATADIR = String.format("%1$s%2$s.data", DIR, fileSep);


    public Library() {
        try {
            File dir = new File(DIR);
            if (! new File(OUTDIR).exists())
                new File(OUTDIR).mkdir();
            if (! (dir.exists() && new File(CONFIG).exists())) {
                LOGGER.fatal("ERROR: You have not yet set up the surveyman directory nor AWS keys. Please see the project website for instructions.");
            } else {
                if (! new File(STATEDATADIR).exists())
                    new File(STATEDATADIR).mkdir();
                if (! new File(UNFINISHED_JOB_FILE).exists())
                    new File(UNFINISHED_JOB_FILE).createNewFile();
                // load up the properties file
                this.props.load(new BufferedReader(new FileReader(this.PARAMS)));
                // make sure we have both names for the access keys in the config file
                Properties config = new Properties();
                config.load(new FileInputStream(CONFIG));
                if (config.containsKey("AWSAccessKeyId") && config.containsKey("AWSSecretKey")) {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(CONFIG, true));
                    bw.newLine();
                    if (! config.containsKey("access_key")) {
                        bw.write("access_key=" + config.getProperty("AWSAccessKeyId"));
                        bw.newLine();
                    }
                    if (! config.containsKey("secret_key")) {
                        bw.write("secret_key=" + config.getProperty("AWSSecretKey"));
                        bw.newLine();
                    }
                    bw.close();
                } else if (config.containsKey("access_key") && config.containsKey("secret_key")) {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(CONFIG, true));
                    bw.newLine();
                    if (! config.containsKey("AWSAccessKeyId")) {
                        bw.write("AWSAccessKeyId="+config.getProperty("access_key"));
                        bw.newLine();
                    }
                    if (! config.containsKey("AWSSecretKey")) {
                        bw.write("AWSSecretKey="+config.getProperty("secret_key"));
                        bw.newLine();
                    }
                    bw.close();
                }
            }
        } catch (IOException ex) {
            LOGGER.fatal(ex);
        }
    }

}
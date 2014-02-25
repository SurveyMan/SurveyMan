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
    public static final String HTMLSKELETON = "HTMLSkeleton.html";
    public static final String JSSKELETON = DIR + fileSep + "custom.js";
    public static final String QUOTS = "quots";
    public static final String XMLSKELETON = "XMLSkeleton.xml";
    public static final String CUSTOMCSS = DIR + fileSep + "custom.css";

    // state/session/job information
    public static final String UNFINISHED_JOB_FILE = Library.DIR + Library.fileSep + ".unfinished";
    public static final String TIME = String.valueOf(System.currentTimeMillis());
    public static final String STATEDATADIR = String.format("%1$s%2$s.data", DIR, fileSep);

    public String getActionForm() {
        return "";
    }


    public Library() {
        try {
            File dir = new File(DIR);
            if (! new File(OUTDIR).exists())
                new File(OUTDIR).mkdir();
            if (! (dir.exists() && new File(CONFIG).exists())) {
                LOGGER.warn("ERROR: You have not yet set up the surveyman directory nor AWS keys. Please see the project website for instructions.");
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
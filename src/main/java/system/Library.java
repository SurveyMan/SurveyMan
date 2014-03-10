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
            if (! new File(STATEDATADIR).exists())
                new File(STATEDATADIR).mkdir();
            if (! new File(UNFINISHED_JOB_FILE).exists())
                new File(UNFINISHED_JOB_FILE).createNewFile();
        } catch (IOException ex) {
            LOGGER.fatal(ex);
        }
    }

    public void init() {
        return;
    }

}
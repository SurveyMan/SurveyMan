package interstitial;

import org.apache.log4j.Logger;
import survey.Survey;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Library {

    public Properties props;
    private static final Logger LOGGER = Logger.getLogger("system");
    public static final String fileSep = File.separator;

    // local configuration information
    public static final String DIR = System.getProperty("user.home") + fileSep + "surveyman";
    public static final String OUTDIR = "output";
    public static final String PARAMS = DIR + fileSep + "params.properties";    // default location if not specified

    // resources
    public static final String HTMLSKELETON = "HTMLSkeleton.html";
    public static final String JSSKELETON = DIR + fileSep + "custom.js";
    public static final String QUOTS = "quots";
    public static final String XMLSKELETON = "XMLSkeleton.xml";
    public static final String CUSTOMCSS = DIR + fileSep + "custom.css";

    // state/session/job information
    public static final String UNFINISHED_JOB_FILE = Library.DIR + Library.fileSep + ".unfinished";
    public static final String BONUS_DATA = Library.DIR + Library.fileSep + ".bonuses";
    public static final String TIME = String.valueOf(System.currentTimeMillis());
    public static final String STATEDATADIR = String.format("%1$s%2$s.data", DIR, fileSep);
    public static final double FEDMINWAGE = 7.25;
    public static double timePerQuestionInSeconds = 10;

    static {
        try {
            if (! new File(DIR).exists())
                new File(DIR).mkdir();
            if (! new File(OUTDIR).exists())
                new File(OUTDIR).mkdir();
            if (! new File(STATEDATADIR).exists())
                new File(STATEDATADIR).mkdir();
            if (! new File(UNFINISHED_JOB_FILE).exists())
                new File(UNFINISHED_JOB_FILE).createNewFile();
            if (! new File(BONUS_DATA).exists())
                new File(BONUS_DATA).createNewFile();
        } catch (IOException ex) {
            LOGGER.fatal(ex);
        }
    }

    public String getActionForm() {
        return "";
    }

    public Library(Properties user_props) {
        if (user_props == null) {
            props = input.PropLoader.loadFromFile(PARAMS);
        } else {
            props = user_props;
        }
    }

    public Library() { this(null); }

    public void init() {
        return;
    }

}
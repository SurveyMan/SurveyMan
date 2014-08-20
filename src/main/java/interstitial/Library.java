package interstitial;

import org.apache.log4j.Logger;
import util.Slurpie;

import java.io.*;
import java.util.Properties;

public abstract class Library {

    public Properties props;
    protected static final Logger LOGGER = Logger.getLogger("SurveyMan");
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
    public static final String BONUS_DATA = Library.DIR + Library.fileSep + ".bonuses";
    public static final String TIME = String.valueOf(System.currentTimeMillis());
    public static final String STATEDATADIR = String.format("%1$s%2$s.data", DIR, fileSep);
    public static final double FEDMINWAGE = 7.25;
    public static double timePerQuestionInSeconds = 10;

    // schemata
    public static final String OUTPUT_SCHEMA = "https://surveyman.github.io/Schemata/survey_output.json";

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

    public abstract String getActionForm();
    public abstract void init();

    public void updateProperties(String filename) throws IOException {
        String foo = Slurpie.slurp(filename);
        props.load(new StringReader(foo));
    }
}
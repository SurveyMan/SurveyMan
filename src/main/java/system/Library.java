package system;

import org.apache.log4j.Logger;
import qc.QCMetrics;
import survey.Survey;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Library {

    public Library() {
    }

    public enum JobStatus { CANCELLED, INTERRUPTED, COMPLETED; }

    public Properties props;
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
    public static final double FEDMINWAGE = 7.25;
    public static double timePerQuestionInSeconds = 10;

    public String getActionForm() {
        return "";
    }

    public Library(Survey survey) {
        try {
            if (! new File(DIR).exists())
                new File(DIR).mkdir();
            if (! new File(OUTDIR).exists())
                new File(OUTDIR).mkdir();
            if (! new File(STATEDATADIR).exists())
                new File(STATEDATADIR).mkdir();
            if (! new File(UNFINISHED_JOB_FILE).exists())
                new File(UNFINISHED_JOB_FILE).createNewFile();
        } catch (IOException ex) {
            LOGGER.fatal(ex);
        }
        props = new Properties();
        BufferedReader propReader = null;
        try {
            propReader = new BufferedReader(new FileReader(PARAMS));
            props.load(propReader);
            if (!props.containsKey("reward"))
                props.setProperty("reward", Double.toString(QCMetrics.getBasePay(survey)));
        } catch (IOException io) {
            LOGGER.warn(io);
        } finally {
            if(propReader != null) try {
                propReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void init() {
        return;
    }

}
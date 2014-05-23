package system.mturk;

import org.apache.log4j.Logger;
import qc.IQCMetrics;
import survey.Survey;
import interstitial.Library;

import java.io.*;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Properties;

public class MturkLibrary extends Library {

    public static class MturkNumberFormat extends NumberFormat {
        final Long minvalue;
        final Long maxvalue;
        public MturkNumberFormat(int minvalue, int maxvalue) {
            super();
            this.minvalue = Long.valueOf(minvalue);
            this.maxvalue = Long.valueOf(maxvalue);
        }
        public Number parse(String source, ParsePosition parsePosition){
            Long me;
            try {
                me = (Long) NumberFormat.getIntegerInstance().parse(source, parsePosition);
            } catch (ClassCastException cce) {
                return maxvalue;
            }
            if (me < minvalue)
                me = minvalue;
            else if (me > maxvalue)
                me = maxvalue;
            return me;
        }
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos){
            return NumberFormat.getIntegerInstance().format(number, toAppendTo, pos);
        }
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos){
            return NumberFormat.getIntegerInstance().format(number, toAppendTo, pos);
        }
    }

    public static final Logger LOGGER = Logger.getLogger("system.mturk");

    public static final String CONFIG = DIR + fileSep + "mturk_config"; // default location if not specified

    private static final String MTURK_SANDBOX_URL = "https://mechanicalturk.sandbox.amazonaws.com?Service=AWSMechanicalTurkRequester";
    private static final String MTURK_PROD_URL = "https://mechanicalturk.amazonaws.com?Service=AWSMechanicalTurkRequester";
    private static final String MTURK_SANDBOX_EXTERNAL_HIT = "https://workersandbox.mturk.com/mturk/externalSubmit";
    private static final String MTURK_PROD_EXTERNAL_HIT = "https://www.mturk.com/mturk/externalSubmit";

    public String MTURK_URL;
    public String EXTERNAL_HIT;
    public static final int mintime = 30;
    public static final int maxtime = 31536000;
    public static final NumberFormat duration_formatter = new MturkNumberFormat(mintime, maxtime);
    public static final NumberFormat lifetime_formatter = new MturkNumberFormat(mintime, maxtime);
    public IQCMetrics qcMetrics;

    private Properties config; // a Properties config file

    public String getActionForm() {
        return EXTERNAL_HIT;
    }
    // editable stuff gets copied

    public MturkLibrary(Properties properties, Survey survey) {
        super(properties);
        this.props.setProperty("reward", Double.toString(qcMetrics.getBasePay(survey)));
        init();
    }

    public MturkLibrary(Properties surveyProps, Properties mtConfig) {
        super(surveyProps);
        config = mtConfig;
        init();
    }

    public MturkLibrary(Properties surveyProps) {
        super(surveyProps);
        init();
    }

    public MturkLibrary(){
        super();
        init();
    }

    public void init() {
        try {
            qcMetrics = (IQCMetrics) Class.forName("qc.Metrics").newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        boolean sandbox = Boolean.parseBoolean(this.props.getProperty("sandbox"));
        if (sandbox) {
            MTURK_URL = MTURK_SANDBOX_URL;
            EXTERNAL_HIT = MTURK_SANDBOX_EXTERNAL_HIT;
        } else {
            MTURK_URL = MTURK_PROD_URL;
            EXTERNAL_HIT = MTURK_PROD_EXTERNAL_HIT;
        }

        if (config == null) {
            File cfile = new File(CONFIG);
            File alt = new File(CONFIG + ".csv");
            if (! cfile.exists() ) {
                // TODO: This seems dangerous; why just use the alternate config instead of renaming?
                // TODO: Also, it's not really a CSV file, right?
                if (alt.exists()) {
                    alt.renameTo(cfile);
                } else {
                    LOGGER.warn("ERROR: You have not yet set up the surveyman directory nor AWS keys. Please see the project website for instructions.");
                }
            }
        }

        // parse config
        try {
            // load up the properties, if needed
            if (props == null) {
                this.props.load(new BufferedReader(new FileReader(PARAMS)));
            }
            if (config == null) {
                this.config.load(new FileInputStream(CONFIG));
            }

            // make sure we have both names for the access keys in the config file
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
        } catch (IOException io){
            LOGGER.trace(io);
        }
    }
}



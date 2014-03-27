package system.mturk;

import system.Library;

import java.io.*;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Properties;

import org.apache.log4j.Logger;

public class MturkLibrary extends Library {

    public static class MturkNumberFormat extends NumberFormat {
        final Long minvalue;
        final Long maxvalue;
        public MturkNumberFormat(int minvalue, int maxvalue) {
            super();
            this.minvalue = new Long(minvalue);
            this.maxvalue = new Long(maxvalue);
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

    public static final String CONFIG = DIR + fileSep + "mturk_config";

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

    public String getActionForm() {
        return EXTERNAL_HIT;
    }
    // editable stuff gets copied

    public MturkLibrary(Properties properties) {
        super();
        this.props = properties;
        init();
    }

    public MturkLibrary(){
        super();
        init();
    }

    public void init() {
        boolean sandbox = Boolean.parseBoolean(this.props.getProperty("sandbox"));
        if (sandbox) {
            MTURK_URL = MTURK_SANDBOX_URL;
            EXTERNAL_HIT = MTURK_SANDBOX_EXTERNAL_HIT;
        } else {
            MTURK_URL = MTURK_PROD_URL;
            EXTERNAL_HIT = MTURK_PROD_EXTERNAL_HIT;
        }
        if (! new File(CONFIG).exists() ) {
            LOGGER.warn("ERROR: You have not yet set up the surveyman directory nor AWS keys. Please see the project website for instructions.");
        } else {
            try {
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
            } catch (IOException io){
                LOGGER.trace(io);
            }
        }
    }
}



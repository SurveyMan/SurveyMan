package system.mturk;

import system.Library;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

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

    public static final String HTMLSKELETON = String.format("%1$s%2$s.metadata%2$sHTMLSkeleton.html", DIR, fileSep);
    public static final String JSSKELETON = String.format("%1$s%2$s.metadata%2$sJSSkeleton.js", DIR, fileSep);
    public static final String QUOTS = String.format("%1$s%2$s.metadata%2$squots", DIR, fileSep);
    public static final String XMLSKELETON = String.format("%1$s%2$s.metadata%2$sXMLSkeleton.xml", DIR, fileSep);

    private static final String MTURK_SANDBOX_URL = "https://mechanicalturk.sandbox.amazonaws.com?Service=AWSMechanicalTurkRequester";
    private static final String MTURK_PROD_URL = "https://mechanicalturk.amazonaws.com?Service=AWSMechanicalTurkRequester";
    private static final String MTURK_SANDBOX_EXTERNAL_HIT = "https://workersandbox.mturk.com/mturk/externalSubmit";
    private static final String MTURK_PROD_EXTERNAL_HIT = "https://www.mturk.com/mturk/externalSubmit";

    public static String MTURK_URL;
    public static String EXTERNAL_HIT;
    public static final int mintime = 30;
    public static final int maxtime = 31536000;
    public static final NumberFormat duration_formatter = new MturkNumberFormat(mintime, maxtime);
    public static final NumberFormat lifetime_formatter = new MturkNumberFormat(mintime, maxtime);

    // editable stuff gets copied

    public static void updateURL(){
        if (Boolean.parseBoolean(props.getProperty("sandbox"))) {
            MTURK_URL = MTURK_SANDBOX_URL;
            EXTERNAL_HIT = MTURK_SANDBOX_EXTERNAL_HIT;
        } else {
            MTURK_URL = MTURK_PROD_URL;
            EXTERNAL_HIT = MTURK_PROD_EXTERNAL_HIT;
        }
    }

    public static void init() {
        Library.init();
        try {
            copyIfChanged(HTMLSKELETON, ".metadata" + fileSep + "HTMLSkeleton.html");
            copyIfChanged(JSSKELETON, ".metadata" + fileSep + "JSSkeleton.js");
            copyIfChanged(QUOTS, ".metadata" + fileSep + "quots");
            copyIfChanged(XMLSKELETON, ".metadata" + fileSep + "XMLSkeleton.xml");
            updateURL();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.fatal(e.getMessage());
        }
    }

}



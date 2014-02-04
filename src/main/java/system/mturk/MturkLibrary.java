package system.mturk;

import scala.Tuple2;
import survey.Survey;
import survey.SurveyException;
import system.Library;

import java.io.*;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;

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

    public static final String HTMLSKELETON = String.format("resources%sHTMLSkeleton.html", fileSep);
    public static final String JSSKELETON = String.format("resources%sJSSkeleton.js", fileSep);
    public static final String QUOTS = String.format("resources%squots", fileSep);
    public static final String XMLSKELETON = String.format("resources%sXMLSkeleton.xml", fileSep);

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

    // editable stuff gets copied

    public MturkLibrary(){
        super();
        boolean sandbox = Boolean.parseBoolean(this.props.getProperty("sandbox"));
        if (sandbox) {
            MTURK_URL = MTURK_SANDBOX_URL;
            EXTERNAL_HIT = MTURK_SANDBOX_EXTERNAL_HIT;
        } else {
            MTURK_URL = MTURK_PROD_URL;
            EXTERNAL_HIT = MTURK_PROD_EXTERNAL_HIT;
        }
    }

}



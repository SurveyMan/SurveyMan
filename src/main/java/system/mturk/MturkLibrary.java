package system.mturk;

import system.Library;
import java.io.IOException;
import org.apache.log4j.Logger;

public class MturkLibrary extends Library {

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

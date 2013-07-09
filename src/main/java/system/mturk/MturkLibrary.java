package system.mturk;

import system.Library;
import utils.Slurpie;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MturkLibrary extends Library {

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

    private static boolean unchanged(String f1, String f2) throws FileNotFoundException, IOException{
        MessageDigest md = null;
        try{
            md = MessageDigest.getInstance("MD5");
        }catch (NoSuchAlgorithmException e) {
            try {
                md = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException ee) {
                System.out.print("Neither MD5 nor SHA found; implement string compare?");
                System.exit(-1);
            }
        }
        return MessageDigest.isEqual(md.digest(Slurpie.slurp(f1).getBytes()), md.digest(Slurpie.slurp(f2).getBytes()));
    }

    private static void copyIfChanged(String src, String dest) throws IOException {
         File f = new File(dest);
         if (! (f.exists() && unchanged(src, dest))) {
            FileWriter writer = new FileWriter(f);
            writer.write(Slurpie.slurp(src));
            writer.close();
         }
    }

    // editable stuff gets copied
    static {
        File dir = new File(DIR);
        if (! (dir.exists() && new File(DIR + fileSep + "config").exists())) {
            System.err.println("Please see the project website for instructions on setting up the .surveyman directory and AWS keys.");
            System.exit(-1);
        } else {
            try {
                if (! new File(DIR + fileSep + ".metadata").exists())
                    new File(DIR + fileSep + ".metadata").mkdir();
                if (! new File(DIR + fileSep + "output").exists())
                    new File(DIR + fileSep + "output").mkdir();
                copyIfChanged(HTMLSKELETON, ".metadata" + fileSep + "HTMLSkeleton.html");
                copyIfChanged(JSSKELETON, ".metadata" + fileSep + "JSSkeleton.js");
                copyIfChanged(QUOTS, ".metadata" + fileSep + "quots");
                copyIfChanged(XMLSKELETON, ".metadata" + fileSep + "XMLSkeleton.xml");
                copyIfChanged(PARAMS, "params.properties");
            } catch (IOException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
        if (Boolean.parseBoolean(props.getProperty("sandbox"))) {
            MTURK_URL = MTURK_SANDBOX_URL;
            EXTERNAL_HIT = MTURK_SANDBOX_EXTERNAL_HIT;
        } else {
            MTURK_URL = MTURK_PROD_URL;
            EXTERNAL_HIT = MTURK_PROD_EXTERNAL_HIT;
        }
    }

}

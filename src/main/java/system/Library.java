package system;

import utils.Slurpie;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Library {

    private static final String fileSep = System.getProperty("file.separator");
    public static final String DIR = System.getProperty("user.home") + fileSep + ".surveyman";
    public static final String CONFIG = DIR + fileSep + "config";
    public static final String OUTDIR = DIR + fileSep + "output";

    public static final String HTMLSKELETON = String.format("%1$s%2$s.metadata%2$sHTMLSkeleton.html", DIR, fileSep);
    public static final String JSSKELETON = String.format("%1$s%2$s.metadata%2$sJSSkeleton.js", DIR, fileSep);
    public static final String QUOTS = String.format("%1$s%2$s.metadata%2$squots", DIR, fileSep);
    public static final String XMLSKELETON = String.format("%1$s%2$s.metadata%2$sXMLSkeleton.xml", DIR, fileSep);
    public static final String PARAMS = DIR + fileSep + "params.properties";

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
                File f = new File(HTMLSKELETON);
                if (! f.exists()) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp(".metadata" + fileSep + "HTMLSkeleton.html"));
                    writer.close();
                }
                f = new File(JSSKELETON);
                if (! f.exists()) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp(".metadata" + fileSep + "JSSkeleton.js"));
                    writer.close();
                }
                f = new File(QUOTS);
                if (! f.exists()) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp(".metadata" + fileSep + "quots"));
                    writer.close();
                }
                f = new File(XMLSKELETON);
                if (! f.exists()) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp(".metadata" + fileSep + "XMLSkeleton.xml"));
                    writer.close();
                }
                f = new File(PARAMS);
                if (! f.exists()) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp("params.properties"));
                    writer.close();
                }
            } catch (IOException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("filesep:"+fileSep);
        System.out.println("DIR:"+DIR);
    }

}

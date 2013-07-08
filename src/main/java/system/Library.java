package system;

import java.io.BufferedReader;
import utils.Slurpie;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Library {
    
    public static Properties props = new Properties();

    public static final String fileSep = System.getProperty("file.separator");
    public static final String DIR = System.getProperty("user.home") + fileSep + ".surveyman";
    public static final String CONFIG = DIR + fileSep + "config";
    public static final String OUTDIR = DIR + fileSep + "output";

    public static final String HTMLSKELETON = String.format("%1$s%2$s.metadata%2$sHTMLSkeleton.html", DIR, fileSep);
    public static final String JSSKELETON = String.format("%1$s%2$s.metadata%2$sJSSkeleton.js", DIR, fileSep);
    public static final String QUOTS = String.format("%1$s%2$s.metadata%2$squots", DIR, fileSep);
    public static final String XMLSKELETON = String.format("%1$s%2$s.metadata%2$sXMLSkeleton.xml", DIR, fileSep);
    public static final String PARAMS = DIR + fileSep + "params.properties";

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
                String srcF = ".metadata" + fileSep + "HTMLSkeleton.html";
                if (! (f.exists() && unchanged(HTMLSKELETON, srcF))) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp(srcF));
                    writer.close();
                }
                f = new File(JSSKELETON);
                srcF = ".metadata" + fileSep + "JSSkeleton.js";
                if (! (f.exists() && unchanged(JSSKELETON, srcF))) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp(srcF));
                    writer.close();
                }
                f = new File(QUOTS);
                srcF = ".metadata" + fileSep + "quots";
                if (! (f.exists() && unchanged(QUOTS, srcF))) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp(srcF));
                    writer.close();
                }
                f = new File(XMLSKELETON);
                srcF = ".metadata" + fileSep + "XMLSkeleton.xml";
                if (! (f.exists() && unchanged(XMLSKELETON, srcF))) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp(srcF));
                    writer.close();
                }
                f = new File(PARAMS);
                srcF = "params.properties";
                if (! (f.exists() && unchanged(PARAMS, srcF))) {
                    FileWriter writer = new FileWriter(f);
                    writer.write(Slurpie.slurp(srcF));
                    writer.close();
                }
            } catch (IOException e){
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
        try {
            // load up the properties file
            props.load(new BufferedReader(new FileReader(PARAMS)));
        } catch (IOException ex) {
            Logger.getLogger(Library.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
    }

    public static void main(String[] args) {
        System.out.println("filesep:"+fileSep);
        System.out.println("DIR:"+DIR);
    }

}

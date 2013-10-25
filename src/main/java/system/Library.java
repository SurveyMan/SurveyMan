package system;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import org.apache.log4j.Logger;

public class Library {

    public enum JobStatus { CANCELLED, INTERRUPTED, COMPLETED; }

    public static Properties props = new Properties();
    private static final Logger LOGGER = Logger.getLogger("system");

    public static final String fileSep = File.separator;
    public static final String DIR = System.getProperty("user.home") + fileSep + "surveyman";
    public static final String CONFIG = DIR + fileSep + "config";
    public static final String OUTDIR = "output";
    public static final String PARAMS = DIR + fileSep + "params.properties";
    public static final String TIME = String.valueOf(System.currentTimeMillis());
    public static final String STATEDATADIR = String.format("%1$s%2$sdata", DIR, fileSep);
    public static final String JOBDATAFILE = STATEDATADIR+fileSep+"jobs.csv";

    public static void writeJobInfo(String csvName, String paramsName, String logFile, JobStatus status) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(JOBDATAFILE));
        writer.write(String.format("%s,%s,%s", csvName, paramsName, logFile, status.name()));
        writer.close();
    }

    protected static void copyIfChanged(String dest, String src) throws IOException {
        File f = new File(dest);
        if (! (f.exists() && unchanged(src, dest))) {
            LOGGER.info(src+"\t"+dest);
            FileWriter writer = new FileWriter(f);
            writer.write(Slurpie.slurp(src));
            writer.close();
        }
    }

    private static boolean unchanged(String f1, String f2) throws FileNotFoundException, IOException{
        MessageDigest md = null;
        try{
            md = MessageDigest.getInstance("MD5");
        }catch (NoSuchAlgorithmException e) {
            try {
                md = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException ee) {
                LOGGER.fatal("Neither MD5 nor SHA found; implement string compare?");
                System.exit(-1);
            }
        }
        //return MessageDigest.isEqual(md.digest(Slurpie.slurp(f1).getBytes()), md.digest(Slurpie.slurp(f2).getBytes()));
        return true;
    }

    public static void init(){
        try {
            File dir = new File(DIR);
            if (! new File(OUTDIR).exists())
                new File(OUTDIR).mkdir();
            if (! (dir.exists() && new File(CONFIG).exists())) {
                LOGGER.fatal("ERROR: You have not yet set up the surveyman directory nor AWS keys. Please see the project website for instructions.");
            } else {
                if (! new File(DIR + fileSep + ".metadata").exists())
                    new File(DIR + fileSep + ".metadata").mkdir();
                if (! new File(STATEDATADIR).exists())
                    new File(STATEDATADIR).mkdir();
                if (! new File(DIR + fileSep + ".unfinished").exists())
                    new File(DIR + fileSep + ".unfinished").createNewFile();
                if (! new File(JOBDATAFILE).exists())
                    new File(JOBDATAFILE).createNewFile();
                // load up the properties file
                copyIfChanged(PARAMS, "params.properties");
                props.load(new BufferedReader(new FileReader(PARAMS)));
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
            }
        } catch (IOException ex) {
            LOGGER.fatal(ex);
        }
    }

}
package gui;

import com.amazonaws.mturk.service.exception.AccessKeyException;
import gui.actionmanager.FindAccessKeysAction;
import gui.display.Experiment;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.*;
import system.Library;
import system.mturk.MturkLibrary;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import javax.swing.*;

/**
 * I would like to acknowledge StackOverflow and the liberal copying I employed to make this Swing crap work.
 */

public class SurveyMan {


    public static final Logger LOGGER = Logger.getRootLogger();
    private static FileAppender txtHandler;
    static {
        LOGGER.setLevel(Level.DEBUG);
        try {
            txtHandler = new FileAppender(new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n"), "logs/SurveyMan.log");
            txtHandler.setAppend(false);
            LOGGER.addAppender(txtHandler);
        }
        catch (IOException io) {
            System.err.println(io.getMessage());
            //System.exit(-1);
        }
    }

    public static void flushOldLogs() {
        long MILLIS_SEC = 1000
                , SEC_MIN = 60
                , MIN_HR = 60
                , HR_DAY = 24
                , DAY_MON = 30;
        long today = System.currentTimeMillis();
        long thirtyDays = MILLIS_SEC * SEC_MIN * MIN_HR * HR_DAY * DAY_MON;
        long monthAgo = today - thirtyDays;
        for (File f : (new File("logs")).listFiles())
            if (f.lastModified() < monthAgo)
                f.delete();
    }

    public static boolean setup() {
        return new File(Library.DIR).isDirectory()
                && new File(MturkLibrary.CONFIG).isFile()
                && new File(MturkLibrary.PARAMS).isFile()
                && new File(MturkLibrary.DIR+MturkLibrary.fileSep+".metadata").isDirectory();
    }

    static {
        // hack to get rid of log4j warnings from libraries (https://github.com/etosch/SurveyMan/issues/157)
        PrintStream err = System.err;
        System.setErr(new PrintStream(new NullOutputStream()));
        try {
            //MturkSurveyPoster.init();
        } catch (Exception e) {
            // this will happen when the
            try {
                getAccessKeys();
            } catch (URISyntaxException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        System.setErr(err);
    }


    private static boolean moveMetadata(){
        // if anything goes wrong, delete the surveyman directory
        try{
            // move metadata and skeletons to the surveyman folder
            File metadata = new File(".metadata");
            File params = new File("params.properties");

            if (!(metadata.isDirectory() && params.isFile())) {
                // load lib
                //MturkLibrary.init();
                return false;
            } else {
                metadata.renameTo(new File(Library.DIR+Library.fileSep+".metadata"));
                params.renameTo(new File(Library.PARAMS));
                // load lib
                //MturkLibrary.init();
                return true;
            }
        } catch (Exception e) {
            (new File(Library.DIR)).delete();
            SurveyMan.LOGGER.fatal(e);
        }
        return false;
    }

    private static void getAccessKeys() throws URISyntaxException, IOException {
        // make directory for access keys
        File home = new File(Library.DIR);
        if (!home.exists())
            home.mkdir();
        // prompt for keys
        Desktop.getDesktop().browse(new URI("https://console.aws.amazon.com/iam/home?#security_credential"));
        JButton findKeys = new JButton("Find access keys");
        FindAccessKeysAction accessKeysAction = new FindAccessKeysAction(findKeys);
        findKeys.addActionListener(accessKeysAction);
        JPanel show = new JPanel(new GridLayout(2, 1));
        show.add(new JLabel("There is a problem with your access keys. Please generate new ones."));
        show.add(findKeys);
        JOptionPane.showMessageDialog(null, show, "Setup error.", JOptionPane.OK_OPTION);
        while (!accessKeysAction.used) {}
        //MturkSurveyPoster.init();
    }

    public static void main(String[] args) {
        try {
            flushOldLogs();
            Experiment.run();
        } catch (AccessKeyException e) {
            LOGGER.trace(e);
            try {
                getAccessKeys();
            } catch (URISyntaxException uri) {
                LOGGER.fatal(uri);
                System.exit(-1);
            } catch (IOException io) {
                LOGGER.fatal(io);
                System.exit(-1);
            }
            LOGGER.warn(e);
    }
    }

  public static void LOGGER(ParseException pe) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
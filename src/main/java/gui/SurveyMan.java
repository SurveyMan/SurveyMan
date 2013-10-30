package gui;

import gui.display.Experiment;
import gui.display.Setup;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.*;
import system.Library;
import system.mturk.MturkLibrary;
import system.mturk.SurveyPoster;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import system.mturk.ResponseManager;

/**
 * I would like to acknowledge StackOverflow and the liberal copying I employed to make this Swing crap work.
 */

public class SurveyMan {


    public static final Logger LOGGER = Logger.getRootLogger();
    public static final String UNFINISHED_JOB_FILE = MturkLibrary.DIR+MturkLibrary.fileSep+".unfinished";
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
            System.exit(-1);
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
        SurveyPoster.init();
        System.setErr(err);
    }

    public static void main(String[] args) {
      ResponseManager.touch();
        try {
            if (!setup()) {
                Setup.run();
            } else {
                flushOldLogs();
                Experiment.run();
            }
        } catch (Exception e) {
            LOGGER.fatal(e);
            e.printStackTrace();
        }
    }
}
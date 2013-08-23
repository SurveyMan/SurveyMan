package gui;

import csv.CSVLexer;
import gui.display.Experiment;
import gui.display.Setup;
import org.apache.log4j.*;
import survey.Survey;
import system.Library;
import system.mturk.MturkLibrary;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * I would like to acknowledge StackOverflow and the liberal copying I employed to make this Swing crap work.
 */

public class SurveyMan {


    public static final Logger LOGGER = Logger.getLogger(Survey.class);
    private static FileAppender txtHandler;
    static {
        LOGGER.setLevel(Level.ALL);
        try {
            txtHandler = new FileAppender(new SimpleLayout(), "logs/SurveyMan.log");
            txtHandler.setEncoding(CSVLexer.encoding);
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

    public static void main(String[] args) {
        if (!(new File(Library.DIR)).isDirectory()) {
            Setup.run();
        } else {
            flushOldLogs();
            Experiment.run();
        }
    }
}

import csv.CSVEntry;
import static csv.CSVEntry.sort;
import static csv.CSVLexer.*;

import csv.CSVLexer;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import csv.CSVParser;
import org.apache.log4j.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Assert;
import scala.Tuple2;
import survey.Survey;
import survey.SurveyException;

/**
 * Tests functions of the classes in the CSV package.
 * @author etosch
 */

@RunWith(JUnit4.class)
public class CSVTest {
     
    private static final Logger LOGGER = Logger.getRootLogger();
    private static FileAppender txtHandler;
    static {
        LOGGER.setLevel(Level.ALL);
        try {
            txtHandler = new FileAppender(new SimpleLayout(), "logs/CSVTest.log");
            txtHandler.setEncoding(CSVLexer.encoding);
            txtHandler.setAppend(false);
            LOGGER.addAppender(txtHandler);
        }
        catch (IOException io) {
            System.err.println(io.getMessage());
            System.exit(-1);
        }
    }
    
    public static Tuple2[] tests = { new Tuple2("data/linguistics/test3.csv", ":")
            , new Tuple2("data/linguistics/test2.csv", "\t")
            , new Tuple2("data/linguistics/test1.csv", ",")
    };
    
    @Test
    public void testSort(){
        ArrayList<CSVEntry> testSort = new ArrayList<CSVEntry>();
        testSort.add(new CSVEntry("", 3, 0));
        testSort.add(new CSVEntry("", 2, 0));
        testSort.add(new CSVEntry("", 5, 0));
        testSort.add(new CSVEntry("", 1, 0));
        testSort.add(new CSVEntry("", 4, 0));
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n before: ");
        for (CSVEntry entry : testSort) 
            sb.append(entry.toString());
        sort(testSort);
        sb.append("\r\n after: ");
        for(int i = 0 ; i < testSort.size() ; i++) {
            if (i!=0)
                Assert.assertTrue(testSort.get(i-1).lineNo < testSort.get(i).lineNo);
            sb.append(testSort.get(i).toString());
        }
        LOGGER.info(sb.toString());
    }
    
    @Test
    public void testLex() throws Exception {
        try{
            for (Tuple2<String, String> test : tests) {
                CSVLexer.separator = test._2().codePointAt(0);
                HashMap<String, ArrayList<CSVEntry>> entries;
                entries = CSVLexer.lex(test._1());
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, ArrayList<CSVEntry>> entry : entries.entrySet())
                    sb.append(String.format(" %s : %s ... %s\r\n"
                            , entry.getKey()
                            , entry.getValue().get(0).toString()
                            , entry.getValue().get(entry.getValue().size() -1).toString()));
                LOGGER.info(sb.toString());
            }
        } catch (SurveyException se) {
            LOGGER.warn(se);
        }
    }

    @Test
    public void testParse() throws Exception {
        HashMap<String, ArrayList<CSVEntry>> entries;
        try{
            for (Tuple2<String, String> test : tests) {
                CSVLexer.separator = test._2().codePointAt(0);
                entries = lex(test._1());
                Survey survey = CSVParser.parse(entries);
                LOGGER.log(Level.DEBUG, " parsed survey: "+survey.toString());
                headers = null;
            }
        } catch (SurveyException se) {
            LOGGER.warn(se);
        }
    }
}

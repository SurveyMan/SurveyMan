
import csv.CSVEntry;
import static csv.CSVEntry.sort;
import csv.CSVLexer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.junit.Assert;
import scala.Tuple2;

/**
 * Tests functions of the classes in the CSV package.
 * @author etosch
 */

@RunWith(JUnit4.class)
public class CSVTest {
     
    private static FileHandler txtHandler;
    static {
        Logger.getGlobal().setLevel(Level.FINEST);
        try {
            txtHandler = new FileHandler("CSVTest.log");
            txtHandler.setFormatter(new SimpleFormatter());
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
        Logger.getGlobal().log(Level.INFO, sb.toString());
    }
    
    @Test
    public void testLex() throws IOException {
        for (Tuple2<String, String> test : tests) {
            CSVLexer.separator = test._2.codePointAt(0);
            HashMap<String, ArrayList<CSVEntry>> entries = CSVLexer.lex(test._1);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, ArrayList<CSVEntry>> entry : entries.entrySet())
                sb.append(String.format(" %s : %s ... %s\r\n"
                        , entry.getKey()
                        , entry.getValue().get(0).toString()
                        , entry.getValue().get(entry.getValue().size() -1).toString()));
            Logger.getGlobal().log(Level.FINEST, sb.toString());
        }
    }
    
    public void testParse() {
        
    }
}
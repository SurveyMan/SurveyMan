
import csv.CSVEntry;
import static csv.CSVEntry.sort;
import csv.CSVLexer;
import java.util.ArrayList;
import java.util.Map;

import csv.CSVParser;
import org.apache.log4j.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Assert;
import survey.Survey;
import survey.SurveyException;
import system.Rules;

/**
 * Tests functions of the classes in the CSV package.
 * @author etosch
 */

@RunWith(JUnit4.class)
public class CSVTest extends TestLog {
     
    public CSVTest(){
        super.init(this.getClass());
    }

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
            for (int i = 0 ; i < testsFiles.length ; i++) {
                CSVLexer lexer = new CSVLexer(testsFiles[i], String.valueOf(separators[i]));
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, ArrayList<CSVEntry>> entry : lexer.entries.entrySet())
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
        try{
            for ( int i = 0 ; i < testsFiles.length ; i++ ) {
                CSVLexer lexer = new CSVLexer(testsFiles[i], String.valueOf(separators[i]));
                CSVParser parser = new CSVParser(lexer);
                Survey survey = parser.parse();
                Rules.ensureBranchForward(survey, parser);
                Rules.ensureBranchParadigms(survey, parser);
                Rules.ensureCompactness(survey);
                Rules.ensureNoDupes(survey);
                Rules.ensureBranchConsistency(survey, parser);
                LOGGER.log(Level.DEBUG, " parsed survey: " + survey.toString());
            }
        } catch (SurveyException se) {
            LOGGER.warn(se);
        }
    }
}

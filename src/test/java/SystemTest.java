import csv.CSVLexer;
import csv.CSVParser;
import org.apache.derby.tools.sysinfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import qc.RandomRespondent;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;
import system.generators.HTML;
import system.mturk.generators.XML;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class SystemTest extends TestLog {

    public SystemTest() {
        super.init(this.getClass());
    }

    @Test
    public void testMturkHTMLGenerator() throws Exception {
        try{
            for ( int i = 0 ; i < testsFiles.length ; i++ ) {
                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
                HTML.getHTMLString(csvParser.parse(), new system.mturk.generators.HTML());
                LOGGER.info(testsFiles[i]+" generated HTML successfully.");
            }
        } catch (SurveyException se) {
            LOGGER.warn(se);
        }
    }

    @Test
    public void testXMLGenerator() throws Exception {
        try{
            for (int i = 0 ; i < testsFiles.length ; i++) {
                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
                XML.getXMLString(csvParser.parse());
                LOGGER.info(testsFiles[i]+" generated HTML successfully.");
            }
        } catch (SurveyException se) {
            LOGGER.warn(se);
        }
    }

    @Test
    public void testSurveyPoster() throws Exception {
        /*
        try{
            for (Tuple2<String, String> test : tests) {
                CSVLexer.headers = null;
                CSVLexer.separator = test._2().codePointAt(0);
                Survey survey = CSVParser.parse(CSVLexer.lex(test._1()));
                MturkSurveyPoster.postSurvey(survey);
                MturkSurveyPoster.expireOldHITs();
            }
        } catch (SurveyException se) {
             LOGGER.warn(se);
        } catch (ServiceException se) {
            LOGGER.fatal(se);
        }
        */
    }

    @Test
    public void testCorrelatedPipeline() throws Exception {
        for (int i = 0 ; i < testsFiles.length ; i++) {
            String[] headers = (new BufferedReader(new FileReader(testsFiles[i]))).readLine().split(String.valueOf(separators[i]));
            CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
            Survey survey = csvParser.parse();
            RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
            String output = rr.response.outputResponse(survey, ",");

        }
    }

    public void testOptionRandomization() throws Exception {

    }
}
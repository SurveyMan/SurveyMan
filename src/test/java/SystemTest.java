import input.csv.CSVLexer;
import input.csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import qc.RandomRespondent;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;
import system.generators.HTML;
import system.mturk.generators.XML;

import java.io.StringReader;

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
                LOGGER.info(testsFiles[i]+" generated IHTML successfully.");
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
                LOGGER.info(testsFiles[i]+" generated IHTML successfully.");
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
    public void testColumnPipeline() throws Exception {
        for (int i = 0 ; i < testsFiles.length ; i++) {
            CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
            Survey survey = csvParser.parse();
            RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
            String headers = SurveyResponse.outputHeaders(survey);
            System.out.println(headers);
            String output = rr.response.outputResponse(survey, ",");
            System.out.println(output);
            SurveyResponse.readSurveyResponses(survey, new StringReader(headers + output));
        }
    }

    @Test
    public void testCorrelatedPipeline() throws Exception {
        for (int i = 0 ; i < testsFiles.length ; i++) {
            CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
            Survey survey = csvParser.parse();
            if (!survey.correlationMap.isEmpty()) {
                System.out.println("input specifies correlations");
                RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
                String headerString = SurveyResponse.outputHeaders(survey);
                assert(headerString.contains(Survey.CORRELATION));
                String[] headers = headerString.split(",");
                // write a function to actually parse in the correlations and check against correlationMap
            }
        }
    }

    public void testOptionRandomization() throws Exception {

    }
}
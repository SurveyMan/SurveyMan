import csv.CSVLexer;
import csv.CSVParser;
import org.apache.derby.tools.sysinfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import survey.SurveyException;
import system.generators.HTML;
import system.mturk.generators.XML;

@RunWith(JUnit4.class)
public class SystemTest extends TestLog {

    public SystemTest(){
        super.init(this.getClass());
    }

    @Test
    public void testMturkHTMLGenerator() throws Exception {
        try{
            for ( int i = 0 ; i < testsFiles.length ; i++ ) {
                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[0], String.valueOf(separators[0])));
                system.generators.HTML.getHTMLString(csvParser.parse(), new system.mturk.generators.HTML());
                LOGGER.info(testsFiles[0]+" generated HTML successfully.");
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
                SurveyPoster.postSurvey(survey);
                SurveyPoster.expireOldHITs();
            }
        } catch (SurveyException se) {
             LOGGER.warn(se);
        } catch (ServiceException se) {
            LOGGER.fatal(se);
        }
        */
    }

    public void testOptionRandomization() throws Exception {

    }
}
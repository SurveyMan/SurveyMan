import csv.CSVLexer;
import csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import scala.Tuple2;
import survey.Survey;
import survey.SurveyException;
import system.mturk.generators.HTML;
import system.mturk.generators.XML;

@RunWith(JUnit4.class)
public class SystemTest extends TestLog {

    public SystemTest(){
        super.init(this.getClass());
    }

    @Test
    public void testHTMLGenerator() throws Exception {
        try{
            for (Tuple2<String, String> test : tests) {
                CSVLexer.separator = test._2().codePointAt(0);
                Survey survey = CSVParser.parse(CSVLexer.lex(test._1()));
                HTML.getHTMLString(survey);
                LOGGER.info(test._1()+" generated HTML successfully.");
            }
        } catch (SurveyException se) {
            LOGGER.warn(se);
        }
    }

    @Test
    public void testXMLGenerator() throws Exception {
        try{
            for (Tuple2<String, String> test : tests) {
                CSVLexer.separator = test._2().codePointAt(0);
                Survey survey = CSVParser.parse(CSVLexer.lex(test._1()));
                XML.getXMLString(survey);
                LOGGER.info(test._1()+" generated HTML successfully.");
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
}
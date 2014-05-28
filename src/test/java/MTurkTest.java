import com.amazonaws.mturk.service.exception.AccessKeyException;
import input.csv.CSVLexer;
import input.csv.CSVParser;
import input.exceptions.SyntaxException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import survey.Survey;
import survey.exceptions.SurveyException;
import interstitial.BackendType;
import interstitial.Library;
import interstitial.Record;
import interstitial.AbstractResponseManager;
import interstitial.ISurveyPoster;
import interstitial.ITask;
import system.mturk.MturkLibrary;
import system.mturk.MturkResponseManager;
import system.mturk.MturkSurveyPoster;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.List;

@RunWith(JUnit4.class)
public class MTurkTest extends TestLog {

    static class SurveyTasksTuple {
        public Survey s;
        public ITask hits;
        public SurveyTasksTuple(Survey s, ITask hits) {
            this.s = s; this.hits = hits;
        }
    }

    public MTurkTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    private SurveyTasksTuple sendSurvey(int i)
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException, InstantiationException {
        CSVParser parser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
        Survey survey = parser.parse();
        MturkLibrary lib = new MturkLibrary();
        Record record = new Record(survey, lib, BackendType.MTURK);
        AbstractResponseManager responseManager = new MturkResponseManager(lib);
        ISurveyPoster surveyPoster = new MturkSurveyPoster();
        record.library.props.setProperty("hitlifetime", "3000");
        record.library.props.setProperty("sandbox", "true");
        MturkResponseManager.putRecord(survey, record);
        ITask hits = surveyPoster.postSurvey(responseManager, record);
        assert (hits!=null);
        return new SurveyTasksTuple(survey, hits);
    }

    @Test
    public void testRenew()
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
            , ParseException, InstantiationException {
        try {
            SurveyTasksTuple stuff  = sendSurvey(1);
            Survey survey = stuff.s;
            ITask hit = stuff.hits;
            AbstractResponseManager responseManager = new MturkResponseManager(new MturkLibrary());
            responseManager.makeTaskUnavailable(hit);
            if (! ((MturkResponseManager) responseManager).renewIfExpired(hit.getTaskId(), survey))
                throw new RuntimeException("Didn't renew.");
            responseManager.makeTaskAvailable(hit.getTaskId(), responseManager.getRecord(survey));
            responseManager.makeTaskUnavailable(hit);
          }catch(AccessKeyException aws) {
            LOGGER.warn(aws);
            System.out.println(aws);
            return;
          } catch (SurveyException se){
            if (outcome[1])
                throw se;
            else return;
        }
    }

}

import com.amazonaws.mturk.service.exception.AccessKeyException;
import input.csv.CSVLexer;
import input.csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import survey.Survey;
import survey.SurveyException;
import system.BackendType;
import system.Library;
import system.Record;
import system.interfaces.AbstractResponseManager;
import system.interfaces.ISurveyPoster;
import system.interfaces.ITask;
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
        public List<ITask> hits;
        public SurveyTasksTuple(Survey s, List<ITask> hits) {
            this.s = s; this.hits = hits;
        }
    }

    public MTurkTest(){
        super.init(this.getClass());
    }

    private SurveyTasksTuple sendSurvey()
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException, InstantiationException {
        CSVParser parser = new CSVParser(new CSVLexer(testsFiles[1], String.valueOf(separators[1])));
        Survey survey = parser.parse();
        Library lib = new MturkLibrary();
        Record record = new Record(survey, lib, BackendType.MTURK);
        AbstractResponseManager responseManager = new MturkResponseManager();
        ISurveyPoster surveyPoster = new MturkSurveyPoster();
        record.library.props.setProperty("hitlifetime", "3000");
        record.library.props.setProperty("sandbox", "true");
        MturkResponseManager.putRecord(survey, record);
        List<ITask> hits = surveyPoster.postSurvey(responseManager, record);
        return new SurveyTasksTuple(survey, hits);
    }

    @Test
    public void testRenew()
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException, InstantiationException {
        try {
            SurveyTasksTuple stuff  = sendSurvey();
            Survey survey = stuff.s;
            List<ITask> hits = stuff.hits;
            AbstractResponseManager responseManager = new MturkResponseManager();
            for (ITask hit : hits)
                responseManager.makeTaskUnavailable(hit);
            for (ITask hit : hits)
                if (((MturkResponseManager) responseManager).renewIfExpired(hit.getTaskId(), survey))
                continue;
            else throw new RuntimeException("Didn't renew.");
        for (ITask hit : hits)
            responseManager.makeTaskUnavailable(hit);
      }catch(AccessKeyException aws) {
        LOGGER.warn(aws);
        return;
      }
    }

    @Test
    public void testRecordCopy()
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException {
//        Tuple2<Survey, List<HIT>> stuff1  = sendSurvey();
//        Tuple2<Survey, List<HIT>> stuff2 = sendSurvey();
//        Survey survey = stuff1._1();
//        Record original = MturkResponseManager.manager.get(survey.sid);
//        original.addNewTask((HIT)stuff2._2().get(0));
//        Record copy = MturkResponseManager.getRecord(survey);
//        assert original!=copy;
//        assert original.getAllHITs().length > 1;
//        assert original.getAllHITs()[0] == copy.getAllHITs()[0];
//        for (HIT hit : stuff1._2())
//            MturkResponseManager.expireHIT(hit);
//        for (HIT hit : stuff2._2())
//            MturkResponseManager.expireHIT(hit);
    }

}

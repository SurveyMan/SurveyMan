import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.exception.AccessKeyException;
import csv.CSVLexer;
import csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import survey.Survey;
import survey.SurveyException;
import system.mturk.Record;
import system.mturk.ResponseManager;
import system.mturk.SurveyPoster;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.List;

@RunWith(JUnit4.class)
public class MTurkTest extends TestLog{

    static class SurveyHITsTuple {
        public Survey s;
        public List<HIT> hits;
        public SurveyHITsTuple(Survey s, List<HIT> hits) {
            this.s = s; this.hits = hits;
        }
    }

    public MTurkTest(){
        super.init(this.getClass());
    }

    private SurveyHITsTuple sendSurvey()
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException {
        CSVParser parser = new CSVParser(new CSVLexer(testsFiles[1], String.valueOf(separators[1])));
        Survey survey = parser.parse();
        Record record = new Record(survey);
        record.library.props.setProperty("hitlifetime", "3000");
        record.library.props.setProperty("sandbox", "true");
        ResponseManager.addRecord(record);
        List<HIT> hits = SurveyPoster.postSurvey(record);
        return new SurveyHITsTuple(survey, hits);
    }

    @Test
    public void testRenew()
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException {
      try {
        SurveyHITsTuple stuff  = sendSurvey();
        Survey survey = stuff.s;
        List<HIT> hits = stuff.hits;
        for (HIT hit : hits)
            ResponseManager.expireHIT(hit);
        for (HIT hit : hits)
            if (ResponseManager.renewIfExpired(hit.getHITId(), ResponseManager.getRecord(survey).library.props))
                continue;
            else throw new RuntimeException("Didn't renew.");
        for (HIT hit : hits)
            ResponseManager.expireHIT(hit);
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
//        Record original = ResponseManager.manager.get(survey.sid);
//        original.addNewHIT((HIT)stuff2._2().get(0));
//        Record copy = ResponseManager.getRecord(survey);
//        assert original!=copy;
//        assert original.getAllHITs().length > 1;
//        assert original.getAllHITs()[0] == copy.getAllHITs()[0];
//        for (HIT hit : stuff1._2())
//            ResponseManager.expireHIT(hit);
//        for (HIT hit : stuff2._2())
//            ResponseManager.expireHIT(hit);
    }

}

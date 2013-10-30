import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.exception.AccessKeyException;
import csv.CSVLexer;
import csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import scala.Tuple2;
import survey.Survey;
import survey.SurveyException;
import system.mturk.MturkLibrary;
import system.mturk.Record;
import system.mturk.ResponseManager;
import system.mturk.SurveyPoster;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;

@RunWith(JUnit4.class)
public class MTurkTest extends TestLog{

    public MTurkTest(){
        super.init(this.getClass());
    }

    private Tuple2<Survey, List<HIT>> sendSurvey()
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException {
        SurveyPoster.init();
        MturkLibrary.props.setProperty("hitlifetime", "3000");
        MturkLibrary.props.setProperty("sandbox", "true");
        SurveyPoster.updateProperties();
        CSVParser parser = new CSVParser(new CSVLexer((String)tests[1]._1(), (String)tests[1]._2()));
        Survey survey = parser.parse();
        List<HIT> hits = SurveyPoster.postSurvey(survey);
        return new Tuple2<Survey, List<HIT>>(survey, hits);
    }

    @Test
    public void testRenew()
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException {
      try {
        Tuple2<Survey, List<HIT>> stuff  = sendSurvey();
        Survey survey = stuff._1();
        List<HIT> hits = stuff._2();
        for (HIT hit : hits)
            ResponseManager.expireHIT(hit);
        for (HIT hit : hits)
            if (ResponseManager.renewIfExpired(hit.getHITId(), ResponseManager.getRecord(survey).parameters))
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

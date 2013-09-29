import com.amazonaws.mturk.requester.HIT;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RunWith(JUnit4.class)
public class MTurkTest extends TestLog{

    public MTurkTest(){
        super.init(this.getClass());
    }

    private Tuple2<Survey, List> sendSurvey() throws IOException, SurveyException {
        SurveyPoster.init();
        MturkLibrary.props.setProperty("hitlifetime", "30");
        MturkLibrary.props.setProperty("sandbox", "true");
        SurveyPoster.updateProperties();
        Survey survey = CSVParser.parse((String)tests[1]._1(), (String)tests[1]._2());
        List<HIT> hits = SurveyPoster.postSurvey(survey, new HashMap<String, Integer>());
        return new Tuple2<Survey, List>(survey, hits);
    }

    @Test
    public void testRenew() throws IOException, SurveyException {
        Tuple2<Survey, List> stuff  = sendSurvey();
        Survey survey = stuff._1();
        List<HIT> hits = stuff._2();
        try {
            Thread.sleep(40000);
        } catch (InterruptedException e) {}
        for (HIT hit : hits){
            assert ResponseManager.renewIfExpired(hit.getHITId(), ResponseManager.getRecord(survey).parameters);
            ResponseManager.expireHIT(hit);
        }
    }

    @Test
    public void testRecordCopy() throws IOException, SurveyException {
        Tuple2<Survey, List> stuff1  = sendSurvey();
        Tuple2<Survey, List> stuff2 = sendSurvey();
        Survey survey = stuff1._1();
        Record original = ResponseManager.manager.get(survey);
        original.addNewHIT((HIT)stuff2._2().get(0));
        Record copy = ResponseManager.getRecord(survey);
        assert original!=copy;
        assert original.getAllHITs().length > 1;
        assert original.getAllHITs()[0] == copy.getAllHITs()[0];
    }

}

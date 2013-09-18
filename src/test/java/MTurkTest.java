import com.amazonaws.mturk.requester.HIT;
import csv.CSVParser;
import org.apache.log4j.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.Assert;
import survey.Survey;
import survey.SurveyException;
import system.mturk.MturkLibrary;
import system.mturk.ResponseManager;
import system.mturk.SurveyPoster;

import java.io.IOException;
import java.util.List;

@RunWith(JUnit4.class)
public class MTurkTest extends TestLog{

    public MTurkTest(){
        super.init(this.getClass());
    }

    @Test
    public void testRenew() throws IOException, SurveyException {
        SurveyPoster.init();
        MturkLibrary.props.setProperty("hitlifetime", "10000");
        MturkLibrary.props.setProperty("sandbox", "true");
        SurveyPoster.updateProperties();
        Survey survey = CSVParser.parse((String)tests[1]._1(), (String)tests[1]._2());
        List<HIT> hits = SurveyPoster.postSurvey(survey);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {}
        for (HIT hit : hits)
            assert ResponseManager.renewIfExpired(hit.getHITId(), ResponseManager.getRecord(survey).parameters);
    }

}

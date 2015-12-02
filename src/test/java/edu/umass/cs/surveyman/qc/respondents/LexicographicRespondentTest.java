package edu.umass.cs.surveyman.qc.respondents;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class LexicographicRespondentTest extends TestLog {

    public LexicographicRespondentTest() throws Exception {
        super.init(this.getClass());
    }

    @Test
    public void testSortByData() {
        List<SurveyDatum> surveyDatumList = new ArrayList<>();
        StringDatum e = new StringDatum("e");
        StringDatum w = new StringDatum("w");
        StringDatum q = new StringDatum("q");
        StringDatum b = new StringDatum("b");
        StringDatum s = new StringDatum("s");
        StringDatum r = new StringDatum("r");
        surveyDatumList.add(e);
        surveyDatumList.add(w);
        surveyDatumList.add(q);
        surveyDatumList.add(b);
        surveyDatumList.add(s);
        surveyDatumList.add(r);
        LexicographicRespondent.sortByData(surveyDatumList);
        Assert.assertArrayEquals(surveyDatumList.toArray(), new Object[]{b, e, q, r, s, w});
    }

    public void testSimulate() {

    }


}

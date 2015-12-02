package edu.umass.cs.surveyman.qc.classifiers;


import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.respondents.AbstractRespondent;
import edu.umass.cs.surveyman.qc.respondents.NoisyLexicographicRespondent;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class LPOClassifierTest extends TestLog {

    public LPOClassifierTest() throws Exception {
        super.init(this.getClass());
    }

    private List<SurveyResponse> makeNResponses(int n, AbstractRespondent respondent) throws SurveyException {
        return AbstractClassifierTest.makeNResponses(n, respondent);
    }

    @Test
    public void testLPOCalculation() throws SurveyException {
        // make 100 lexicographic responses
        SurveyDatum optionA = new StringDatum("a");
        SurveyDatum optionB = new StringDatum("b");
        SurveyDatum optionC = new StringDatum("c");
        SurveyDatum optionD = new StringDatum("d");
        Survey survey = new Survey(
                new Question("A", optionA, optionB, optionC, optionD),
                new Question("B", optionA, optionB, optionC, optionD),
                new Question("C", optionA, optionB, optionC, optionD),
                new Question("D", optionA, optionB, optionC, optionD),
                new Question("E", optionA, optionB, optionC, optionD));
        List<SurveyResponse> responses = makeNResponses(100, new NoisyLexicographicRespondent(survey, 0.1));
        AbstractClassifier classifier = new LPOClassifier(survey, responses, false, 0.05, 1);
        ((LPOClassifier) classifier).makeLPOs();
        for (Map.Entry<Question, List<SurveyDatum>> lposForQ : ((LPOClassifier) classifier).lpos.entrySet()) {
            SurveyDatum a = lposForQ.getKey().getOptByText("a");
            SurveyDatum b = lposForQ.getKey().getOptByText("b");
            SurveyDatum c = lposForQ.getKey().getOptByText("c");
            SurveyDatum d = lposForQ.getKey().getOptByText("d");
            Assert.assertFalse(lposForQ.getValue().contains(a));
            Assert.assertTrue(lposForQ.getValue().contains(b));
            Assert.assertTrue(lposForQ.getValue().contains(c));
            Assert.assertTrue(lposForQ.getValue().contains(d));
        }
    }
}
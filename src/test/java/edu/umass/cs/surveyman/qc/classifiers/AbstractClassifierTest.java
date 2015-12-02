package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.qc.respondents.AbstractRespondent;
import edu.umass.cs.surveyman.qc.respondents.LexicographicRespondent;
import edu.umass.cs.surveyman.qc.respondents.NoisyLexicographicRespondent;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class AbstractClassifierTest extends TestLog {

    public AbstractClassifierTest() throws Exception {
        super.init(this.getClass());
    }

    public static List<SurveyResponse> makeNResponses(int n, AbstractRespondent respondent) throws SurveyException {
        List<SurveyResponse> retval = new ArrayList<>();
        while (n > 0) {
            retval.add(respondent.getResponse());
            n--;
        }
        return retval;
    }

    @Test
    public void testFrequencies() throws SurveyException {
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
        AbstractClassifier classifier = new AbstractClassifier(survey, false, 0.05, 1) {
            @Override
            public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
                return 0;
            }

            @Override
            public double getScoreForResponse(SurveyResponse surveyResponse) throws SurveyException {
                return 0;
            }

            @Override
            public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {

            }

            @Override
            public boolean classifyResponse(SurveyResponse response) throws SurveyException {
                return false;
            }
        };
        List<SurveyResponse> responses;
        // First with lexicographic resondents
        responses = makeNResponses(100, new LexicographicRespondent(survey));
        classifier.makeProbabilities(responses);
        for (Map.Entry<String, HashMap<String, Integer>> entry : classifier.answerFrequencyMap.entrySet()) {
            Question q = survey.getQuestionById(entry.getKey());
            SurveyDatum a = q.getOptByText("a");
            for (Map.Entry<String, Integer> cts : entry.getValue().entrySet()) {
                if (cts.getKey().equals(a.getId())) {
                    Assert.assertEquals(cts.getValue().intValue(), 100);
                } else {
                    Assert.assertEquals(cts.getValue().intValue(), 0);
                }
            }
        }
        // Now with noise
        responses = makeNResponses(100, new NoisyLexicographicRespondent(survey, 0.2));
        classifier.makeProbabilities(responses);
        for (Map.Entry<String, HashMap<String, Integer>> entry : classifier.answerFrequencyMap.entrySet()) {
            Question q = survey.getQuestionById(entry.getKey());
            SurveyDatum a = q.getOptByText("a");
            for (Map.Entry<String, Integer> cts : entry.getValue().entrySet()) {
                int count = cts.getValue();
                if (cts.getKey().equals(optionA.getId())) {
                    Assert.assertTrue(String.format("We should have about 80 responses, have %d", count), count > 50);
                } else {
                    Assert.assertTrue(String.format("We should get at least one response in each bucket, with ~7 in each of the others: %d", count), count > 1 && count < 20);
                }
            }
        }
    }

    @Test
    public void testProbabilities() throws SurveyException {
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
        AbstractClassifier classifier = new AbstractClassifier(survey, false, 0.05, 1) {
            @Override
            public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
                return 0;
            }

            @Override
            public double getScoreForResponse(SurveyResponse surveyResponse) throws SurveyException {
                return 0;
            }

            @Override
            public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {

            }

            @Override
            public boolean classifyResponse(SurveyResponse response) throws SurveyException {
                return false;
            }
        };
        List<SurveyResponse> responses = makeNResponses(200, new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM));
        classifier.makeProbabilities(responses);
        for (Map<String, Double> m : classifier.answerProbabilityMap.values()) {
            double total = 0.0;
            for (Double d : m.values()) {
                Assert.assertTrue(String.format("Individual prob. should be near 0.25: %f", d), d > 0.22 && d < 0.28);
                total += d;
            }
            Assert.assertTrue(String.format("Total probability should be near 1.0: %f", total), total > 0.95 && total < 1.05);
        }
    }

    @Test
    public void testProbabilitiesWithSmothing() {
        //TODO: write test
    }

    @Test
    public void testGetResponseSubset() {
        //TODO: write test
    }
}

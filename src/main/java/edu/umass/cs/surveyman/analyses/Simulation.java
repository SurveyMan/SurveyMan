package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.qc.NonRandomRespondent;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.qc.RandomRespondent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.List;

public class Simulation {

    public static boolean smoothing = true;

    static class ROC {

        public final double percBots;
        public final int truePositive;
        public final int falsePositive;
        public final int trueNegative;
        public final int falseNegative;

        public ROC(double percentBots, int truePositive, int falsePositive, int trueNegative, int falseNegative) {
            this.percBots = percentBots;
            this.trueNegative = trueNegative;
            this.truePositive = truePositive;
            this.falseNegative = falseNegative;
            this.falsePositive = falsePositive;
        }
    }

    public static List<ROC> simulate(Survey survey, int totalResponses, double granularity) throws SurveyException {
        List<ROC> data = new ArrayList<ROC>();
        for (double i = 0; i < 1; i += granularity) {
            int numRandomRespondents = (int) Math.floor(totalResponses * i);
            int numRealRespondents = totalResponses - numRandomRespondents;
            List<ISurveyResponse> randomResponses = new ArrayList<ISurveyResponse>();
            List<ISurveyResponse> realResponses = new ArrayList<ISurveyResponse>();
            for (int j = 0 ; j < numRandomRespondents ; j++) {
                randomResponses.add(new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM).getResponse());
            }
            //TODO(etosch): add parameter so we can have more than one cluster
            NonRandomRespondent profile = new NonRandomRespondent(survey);
            for (int j = 0 ; j < numRealRespondents ; j++) {
                randomResponses.add(profile.getResponse());
            }
            int ctTruePositive = 0, ctTrueNegative = 0, ctFalsePositive = 0, ctFalseNegative = 0;
            List<ISurveyResponse> allResponses = new ArrayList<ISurveyResponse>();
            allResponses.addAll(randomResponses);
            allResponses.addAll(realResponses);
            assert allResponses.size() == randomResponses.size() + realResponses.size();
            for (ISurveyResponse sr : randomResponses) {
                if (QCMetrics.logLikelihoodClassification(survey, sr, allResponses, smoothing, 0.05))
                    ctTruePositive++;
                else ctFalseNegative++;
            }
            for (ISurveyResponse sr : realResponses) {
                if (QCMetrics.logLikelihoodClassification(survey, sr, allResponses, smoothing, 0.05))
                    ctFalsePositive++;
                else ctTrueNegative++;
            }
            data.add(new ROC(i, ctTruePositive, ctFalsePositive, ctTrueNegative, ctFalseNegative));
        }
        return data;
    }
}

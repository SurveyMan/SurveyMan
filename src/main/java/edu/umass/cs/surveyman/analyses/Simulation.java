package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.qc.*;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.List;

public class Simulation {

    public static boolean smoothing = false;

    static class ROC {

        public final double percBots;
        public final int truePositive;
        public final int falsePositive;
        public final int trueNegative;
        public final int falseNegative;
        public final double empiricalEntropy;

        public ROC(double percentBots,
                   int truePositive,
                   int falsePositive,
                   int trueNegative,
                   int falseNegative,
                   double empiricalEntropy) {
            this.percBots = percentBots;
            this.trueNegative = trueNegative;
            this.truePositive = truePositive;
            this.falseNegative = falseNegative;
            this.falsePositive = falsePositive;
            this.empiricalEntropy = empiricalEntropy;
        }
    }

    static class ValidityException extends SurveyException {
        public ValidityException() {
            super("Validity status must be known in simulation.");
        }
    }

    public static List<AbstractSurveyResponse> simulate(Survey survey, int totalResponses, double percRandomRespondents)
            throws SurveyException {

        List<AbstractSurveyResponse> randomResponses = new ArrayList<AbstractSurveyResponse>();
        List<AbstractSurveyResponse> realResponses = new ArrayList<AbstractSurveyResponse>();

        int numRandomRespondents = (int) Math.floor(totalResponses * percRandomRespondents);
        int numRealRespondents = totalResponses - numRandomRespondents;

        for (int j = 0 ; j < numRandomRespondents ; j++) {
            AbstractSurveyResponse r = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM).getResponse();
            assert r.getKnownValidityStatus() == KnownValidityStatus.NO : String.format(
                    "Random respondent's validity status must be NO, was %s", r.getKnownValidityStatus());
            randomResponses.add(r);
        }

        //TODO(etosch): add parameter so we can have more than one cluster
        NonRandomRespondent profile = new NonRandomRespondent(survey);
        for (int j = 0 ; j < numRealRespondents ; j++) {
            AbstractSurveyResponse r = profile.getResponse();
            assert r.getKnownValidityStatus() == KnownValidityStatus.YES : String.format(
                    "Nonrandom respondent's validity status must be YES, was %s", r.getKnownValidityStatus());
            realResponses.add(r);
        }

        List<AbstractSurveyResponse> allResponses = new ArrayList<AbstractSurveyResponse>();
        allResponses.addAll(randomResponses);
        allResponses.addAll(realResponses);
        assert allResponses.size() == randomResponses.size() + realResponses.size();

        return allResponses;
    }

    public static ROC analyze(Survey survey, List<AbstractSurveyResponse> surveyResponses, Classifier classifier)
            throws SurveyException {

        int ctKnownValid = 0, ctKnownInvalid = 0;
        int ctTruePositive = 0, ctTrueNegative = 0, ctFalsePositive = 0, ctFalseNegative = 0;
        double empiricalEntropy;

        for (AbstractSurveyResponse sr : surveyResponses) {
            assert sr.getKnownValidityStatus() != null : String.format(
                    "Survey %s response must have a known validity status", sr.getSrid());
            boolean classification;
            switch (classifier) {
                case LOG_LIKELIHOOD:
                    classification = QCMetrics.logLikelihoodClassification(survey, sr, surveyResponses, smoothing, 0.05);
                    break;
                case ENTROPY:
                    classification = QCMetrics.entropyClassification(survey, sr, surveyResponses, smoothing, 0.05);
                    break;
                default:
                    throw new RuntimeException(String.format("Unknown classification type %s.", classifier.name()));
            }
            assert sr.getKnownValidityStatus() != null : String.format(
                    "Survey %s response must have a known validity status", sr.getSrid());
            switch (sr.getKnownValidityStatus()) {
                case MAYBE:
                    throw new ValidityException();
                case NO:
                    ctKnownInvalid++;
                    if (classification)
                        // Is known to be invalid, was found to be valid.
                        ctFalsePositive++;
                    else
                        // Is known to be invalid, was found to be invalid.
                        ctTrueNegative++;
                    break;
                case YES:
                    ctKnownValid++;
                    if (classification)
                        // Is known to be valid, was found to be valid.
                        ctTruePositive++;
                    else
                        // Is known to be valid, was found to be invalid.
                        ctFalseNegative++;
                    break;
            }
        }
        empiricalEntropy = QCMetrics.surveyEntropy(survey, surveyResponses);
        assert empiricalEntropy > 0 : "Survey must have entropy greater than 0.";
        assert ctKnownInvalid + ctKnownValid == surveyResponses.size();
        return new ROC((double) ctKnownInvalid / surveyResponses.size(),
            ctTruePositive, ctFalsePositive, ctTrueNegative, ctFalseNegative, empiricalEntropy);
    }
}

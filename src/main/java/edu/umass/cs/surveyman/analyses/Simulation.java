package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.output.ClassificationStruct;
import edu.umass.cs.surveyman.output.ClassifiedRespondentsStruct;
import edu.umass.cs.surveyman.qc.*;
import edu.umass.cs.surveyman.qc.classifiers.AbstractClassifier;
import edu.umass.cs.surveyman.qc.respondents.AbstractRespondent;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.List;

public class Simulation {

    public static boolean smoothing = false;

    public static class ROC {

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

        public String toString() {
            return String.format("%f,%f,%d,%d,%d,%d\n",
                    this.percBots,
                    this.empiricalEntropy,
                    this.truePositive,
                    this.falsePositive,
                    this.trueNegative,
                    this.falseNegative);
        }
    }

    static class ValidityException extends SurveyException {
        public ValidityException() {
            super("Validity status must be known in simulation.");
        }
    }

    /**
     * Simulates responses to a survey.
     * @param survey The survey to simulate.
     * @param percentAdversaries The percentage of random actors we want to simulate.
     * @param adversaryType The type of adversary we want to test against.
     * @param profile An instance of the type of honest respondent we want to simulate.
     * @return List of simulated survey responses.
     * @throws SurveyException
     */
    public static List<SurveyResponse> simulate(Survey survey,
                                                double percentAdversaries,
                                                RandomRespondent.AdversaryType adversaryType,
                                                AbstractRespondent profile) throws SurveyException {

        QCMetrics qcMetrics = new QCMetrics(survey, null);
        long totalResponses = qcMetrics.getSampleSize().getLeft();
        SurveyMan.LOGGER.info(String.format("Simulating %d responses...", totalResponses));

        List<SurveyResponse> randomResponses = new ArrayList<>();
        List<SurveyResponse> realResponses = new ArrayList<>();

        long numRandomRespondents = (int) Math.floor(totalResponses * percentAdversaries);
        long numRealRespondents = totalResponses - numRandomRespondents;

        while (numRandomRespondents > 0) {
            SurveyResponse r = new RandomRespondent(survey, adversaryType).getResponse();
            assert r.getKnownValidityStatus() == KnownValidityStatus.NO : String.format(
                    "Random respondent's validity status must be NO, was %s", r.getKnownValidityStatus());
            randomResponses.add(r);
            numRandomRespondents--;
        }

        while (numRealRespondents > 0) {
            SurveyResponse r = profile.getResponse();
            assert r.getKnownValidityStatus() == KnownValidityStatus.YES : String.format(
                    "Nonrandom respondent's validity status must be YES, was %s", r.getKnownValidityStatus());
            realResponses.add(r);
            numRealRespondents--;
        }

        List<SurveyResponse> allResponses = new ArrayList<>();
        allResponses.addAll(randomResponses);
        allResponses.addAll(realResponses);
        assert allResponses.size() == totalResponses;
        return allResponses;
    }

    /**
     * Classifies bad actors and returns classification results for the mix of respondents provided.
     * @param survey The survey we wish to simulate.
     * @param surveyResponses The set of responses we wish to analyze. This will typically be a mix of some simulated
     *                        responses.
     * @param classifier The classification method to use.
     * @return A struct containing the classification results for this mix of bad actors and honest respondents.
     * @throws SurveyException
     */
    public static ROC analyze(Survey survey,
                              List<? extends SurveyResponse> surveyResponses,
                              AbstractClassifier classifier)
            throws SurveyException
    {

        int ctKnownValid = 0, ctKnownInvalid = 0;
        int ctTruePositive = 0, ctTrueNegative = 0, ctFalsePositive = 0, ctFalseNegative = 0;
        double empiricalEntropy;
        QCMetrics qcMetrics = new QCMetrics(survey, classifier);

        ClassifiedRespondentsStruct classifiedRespondentsStruct = qcMetrics.classifyResponses(surveyResponses);

        for (ClassificationStruct classificationStruct : classifiedRespondentsStruct) {

            SurveyResponse sr = classificationStruct.surveyResponse;
            boolean classification = classificationStruct.isValid();

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
        //assert empiricalEntropy > 0 : "Survey must have entropy greater than 0.";
        assert ctKnownInvalid + ctKnownValid == surveyResponses.size();
        return new ROC((double) ctKnownInvalid / surveyResponses.size(),
            ctTruePositive, ctFalsePositive, ctTrueNegative, ctFalseNegative, empiricalEntropy);
    }
}

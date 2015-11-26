package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.List;

public class StackedClassifier extends AbstractClassifier {

    private LPOClassifier lpoClassifier;
    private ClusterClassifier clusterClassifier;

    public StackedClassifier(Survey survey, boolean smoothing, double alpha, int numClusters) {
        super(survey, smoothing, alpha, numClusters);
        this.lpoClassifier = new LPOClassifier(survey, smoothing, alpha, numClusters);
        this.clusterClassifier = new ClusterClassifier(survey, smoothing, alpha, numClusters);
    }

    @Override
    public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
        return lpoClassifier.getScoreForResponse(responses);
    }

    @Override
    public double getScoreForResponse(SurveyResponse surveyResponse) throws SurveyException {
        return lpoClassifier.getScoreForResponse(surveyResponse);
    }

    @Override
    public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {
        lpoClassifier.computeScoresForResponses(responses);
        clusterClassifier.labelValidity(clusterClassifier.clusterResponses(responses));
    }

    @Override
    public boolean classifyResponse(SurveyResponse response) throws SurveyException {
        return ! response.getComputedValidityStatus().equals(KnownValidityStatus.NO);
    }
}

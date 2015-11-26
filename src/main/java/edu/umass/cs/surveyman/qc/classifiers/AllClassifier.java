package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.List;

public class AllClassifier extends AbstractClassifier {

    public AllClassifier(Survey survey) {
        super(survey, false, 1.0, 1);
    }

    public AllClassifier(Survey survey, boolean smoothing, double alpha, int numClusters) {
        super(survey, smoothing, alpha, numClusters);
    }

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
        return true;
    }
}

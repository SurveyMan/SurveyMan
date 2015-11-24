package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.List;

public class AllClassifier extends AbstractClassifier {
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

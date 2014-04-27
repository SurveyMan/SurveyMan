package qc;


import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;

import java.util.List;

public interface QCMetrics {

    public double surveyEntropy(Survey s, List<SurveyResponse> responses);

    public double getMaxPossibleEntropy(Survey s);

    public int minimumPathLength(Survey survey);

    public int maximumPathLength(Survey survey);

    public double averagePathLength(Survey survey) throws SurveyException;

    public double getBasePay(Survey survey);

    public boolean entropyClassification(SurveyResponse sr, List<SurveyResponse> responses);

}

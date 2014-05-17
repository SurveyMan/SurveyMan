package qc;


import interstitial.ISurveyResponse;
import survey.Survey;
import survey.exceptions.SurveyException;

import java.util.List;

public interface IQCMetrics {

    public double surveyEntropy(Survey s, List<ISurveyResponse> responses);
    public double getMaxPossibleEntropy(Survey s);
    public int minimumPathLength(Survey survey);
    public int maximumPathLength(Survey survey);
    public double averagePathLength(Survey survey) throws SurveyException;
    public double getBasePay(Survey survey);
    public boolean entropyClassification(Survey survey, ISurveyResponse sr, List<ISurveyResponse> responses);
    public double calculateBonus(ISurveyResponse sr, QC qc);
    public double getBotThresholdForSurvey(Survey s);

}

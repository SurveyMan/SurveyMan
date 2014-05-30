package qc;


import interstitial.ISurveyResponse;
import interstitial.Record;
import survey.Block;
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
    public double calculateBonus(ISurveyResponse sr, Record record);
    public double getBotThresholdForSurvey(Survey s);
    public List<List<Block>> getDag(List<Block> blockList);

}

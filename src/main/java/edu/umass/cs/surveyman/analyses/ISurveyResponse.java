package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.Reader;
import java.util.List;
import java.util.Map;

public interface ISurveyResponse {

    public List<IQuestionResponse> getResponses();
    public void setResponses(List<IQuestionResponse> responses);
    public boolean isRecorded();
    public void setRecorded(boolean recorded);
    public String getSrid();
    public void setSrid(String srid);
    public String workerId();
    public Map<String,IQuestionResponse> resultsAsMap();
    public List<ISurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException;
    public void setScore(double score);
    public double getScore();
    public void setThreshold(double pval);
    public double getThreshold();
    public boolean surveyResponseContainsAnswer(List<Component> variants);
    public KnownValidityStatus getKnownValidityStatus();
    public void setKnownValidityStatus(KnownValidityStatus validityStatus);
}
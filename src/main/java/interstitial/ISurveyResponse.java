package interstitial;

import survey.Survey;
import survey.exceptions.SurveyException;

import java.io.Reader;
import java.util.List;
import java.util.Map;

public interface ISurveyResponse {

    public List<IQuestionResponse> getResponses();
    public boolean isRecorded();
    public String srid();
    public String workerId();
    //public String outputSurveyResponse(Survey survey, String sep);
    public void setRecorded(boolean recorded);
    public Map<String,IQuestionResponse> resultsAsMap();
    public List<ISurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException;

}


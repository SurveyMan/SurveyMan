package qc;

import survey.Component;
import survey.Question;
import survey.Survey;
import survey.exceptions.SurveyException;

import java.util.List;
import java.util.Map;

public interface ISurveyResponse {

    public List<IQuestionResponse> getResponses();

    public boolean isRecorded();

    public String srid();

    public String outputResponse(Survey survey, String sep);

    public void setRecorded(boolean recorded);

}

package system.interfaces;

import survey.Survey;
import survey.SurveyException;
import system.Record;
import system.mturk.MturkResponseManager;

import java.util.List;

public interface SurveyPoster {

    public boolean getFirstPost();
    public void setFirstPost(boolean post);
    public void refresh(Record r);
    public List<Task> postSurvey(ResponseManager responseManager, Record r) throws SurveyException;
    public boolean postMore(ResponseManager mturkResponseManager, Survey survey);
}

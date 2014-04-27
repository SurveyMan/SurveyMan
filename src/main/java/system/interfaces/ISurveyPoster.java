package system.interfaces;

import survey.Survey;
import survey.exceptions.SurveyException;
import system.Record;

import java.util.List;

public interface ISurveyPoster {

    public boolean getFirstPost();
    public void setFirstPost(boolean post);
    public void refresh(Record r);
    public List<ITask> postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException;
    public boolean postMore(AbstractResponseManager mturkResponseManager, Survey survey);
    public String makeTaskURL(ITask task);
}

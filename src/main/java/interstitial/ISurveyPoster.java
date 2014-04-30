package interstitial;

import survey.exceptions.SurveyException;

import java.util.List;

public interface ISurveyPoster {

    public boolean getFirstPost();
    public void setFirstPost(boolean post);
    public void refresh(Record r);
    public List<ITask> postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException;
    public boolean postMore(AbstractResponseManager responseManager, Record r);
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt);
    public String makeTaskURL(ITask task);

}

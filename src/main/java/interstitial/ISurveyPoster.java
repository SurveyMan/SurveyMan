package interstitial;

import survey.exceptions.SurveyException;

import java.util.List;

public interface ISurveyPoster {

    public void refresh(Record r);
    public ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException;
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt);
    public String makeTaskURL(ITask task);

}

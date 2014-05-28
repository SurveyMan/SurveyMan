package interstitial;

import survey.exceptions.SurveyException;

import java.util.List;

public interface ISurveyPoster {

<<<<<<< HEAD
=======
    public void refresh(Record r);
>>>>>>> 4c70a41beeaa860bd1a36014af0c182bbfe704bd
    public ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException;
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt);
    public String makeTaskURL(ITask task);
    public void init(String config);
}

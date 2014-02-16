package system.localhost;

import survey.Survey;
import survey.SurveyException;
import system.Record;
import system.interfaces.ResponseManager;
import system.interfaces.SurveyPoster;
import system.interfaces.Task;

import java.util.List;

public class LocalSurveyPoster implements SurveyPoster{
    @Override
    public void refresh(Record r) {

    }

    @Override
    public List<Task> postSurvey(ResponseManager responseManager, Record r) throws SurveyException {
        return null;
    }

    @Override
    public boolean postMore(ResponseManager mturkResponseManager, Survey survey) {
        return false;
    }
}

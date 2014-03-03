package system.localhost;

import com.amazonaws.mturk.requester.HIT;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;
import system.Gensym;
import system.Record;
import system.interfaces.ResponseManager;
import system.interfaces.Task;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LocalResponseManager extends ResponseManager {

    private static final Gensym workerIds = new Gensym("w");

    public List<Server.IdResponseTuple> getNewAnswers() {
        return new ArrayList<Server.IdResponseTuple>();
    }

    public Server.IdResponseTuple parseJson(String responses) {
        return null;
    }

    @Override
    public int addResponses(Survey survey, Task task) throws SurveyException {
        int responsesAdded = 0;
        Record r = null;
        try {
            r = ResponseManager.getRecord(survey);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (r==null) return -1;
        List<SurveyResponse> responses = r.responses;
        System.out.println(String.format("%d responses total", responses.size()));
        List<Server.IdResponseTuple> tuples = getNewAnswers();
        return responsesAdded;
    }

    @Override
    public Task getTask(String taskid) {
        return null;
    }

    @Override
    public List<Task> listAvailableTasksForRecord(Record r) {
        return Arrays.asList(r.getAllTasks());
    }

    @Override
    public boolean makeTaskUnavailable(Task task) {
        return false;
    }

    @Override
    public boolean makeTaskAvailable(String taskId, Record r) {
        return false;
    }

}

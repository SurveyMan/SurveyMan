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
import java.util.List;

public class LocalResponseManager extends ResponseManager {

    private static final Gensym workerIds = new Gensym("w");

    public List<String> getNewAnswers() {
        return null;
    }

    public int addResponses(Survey survey, HIT hit) {
        Record record = super.manager.get(survey.sid);
        List<String> newAnswers = getNewAnswers();
        List<SurveyResponse> responsesToAdd = new ArrayList<SurveyResponse>();
        try {
            for (String xmlStringAnswer : newAnswers) {
                SurveyResponse sr = new SurveyResponse(survey
                        , workerIds.next()
                        , xmlStringAnswer
                        , record
                        , null
                );
                responsesToAdd.add(sr);
            }
        } catch (SurveyException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        record.responses.addAll(responsesToAdd);
        return responsesToAdd.size();
    }

    @Override
    public int addResponses(Survey survey, Task task) throws SurveyException {
        return 0;
    }

    @Override
    public Task getTask(String taskid) {
        return null;
    }

    @Override
    public List<Task> listAvailableTasksForRecord(Record r) {
        return null;
    }

    @Override
    public boolean makeTaskUnavailable(Task task) {
        return false;
    }

    @Override
    public boolean makeTaskAvailable(String taskId) {
        return false;
    }

    @Override
    public void addTaskToRecordByTaskId(Record r, String tid) {

    }
}

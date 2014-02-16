package system.interfaces;

import survey.Survey;
import survey.SurveyException;
import system.Record;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public abstract class ResponseManager {

    protected static class RecordNotFoundException extends SurveyException {
        public RecordNotFoundException() {
            super(String.format("Survey is currently uninitialized; try \"Preview HIT\" first."));
        }
    }

    final public static int maxwaittime = 60;
    public static HashMap<String, Record> manager = new HashMap<String, Record>();

    public static void chill(int seconds){
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {}
    }

    public abstract int addResponses(Survey survey, Task task) throws SurveyException;
    //public List<Assignment> getAllAssignmentsForHIT(HIT hit);
    public abstract Task getTask(String taskid);
    public abstract List<Task> listAvailableTasksForRecord(Record r);
    public abstract boolean makeTaskUnavailable(Task task);

    public static Record getRecord(Survey survey) throws IOException, SurveyException {
        synchronized (manager) {
            if (survey==null)
                throw new RecordNotFoundException();
            Record r = manager.get(survey.sid);
            return r;
        }
    }
}

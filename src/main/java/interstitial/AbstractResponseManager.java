package interstitial;

import org.apache.log4j.Logger;
import survey.Survey;
import survey.exceptions.SurveyException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractResponseManager {

    final protected static Logger LOGGER = Logger.getLogger(AbstractResponseManager.class);


    protected static class RecordNotFoundException extends SurveyException {
        public RecordNotFoundException() {
            super(String.format("Survey is currently uninitialized; try \"Preview HIT\" first."));
        }
    }

    final public static int maxwaittime = 60;
    private static ConcurrentHashMap<String, Record> manager = new ConcurrentHashMap<String, Record>();

    public static void chill(int seconds){
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {}
    }

    public abstract int addResponses(Survey survey, ITask task) throws SurveyException;
    //public List<Assignment> getAllAssignmentsForHIT(HIT hit);
    public abstract ITask getTask(String taskid);
    public abstract List<ITask> listAvailableTasksForRecord(Record r);
    public abstract boolean makeTaskUnavailable(ITask task);
    public abstract boolean makeTaskAvailable(String taskId, Record r);
    public abstract void awardBonus(double amount, ISurveyResponse sr, Survey survey);
    public abstract ITask makeTaskForId(Record record, String taskid);

    public abstract ISurveyResponse parseResponse (String workerId, String ansXML, Survey survey, Record r, Map<String, String> otherValues) throws SurveyException;

    public static Record getRecord(Survey survey) throws IOException, SurveyException {
        if (survey==null)
            throw new RecordNotFoundException();
        return manager.get(survey.source);
    }

    public static void putRecord(Survey survey, Record record) {
        manager.put(survey.source, record);
    }

    public static void removeRecord(Record record) {
        manager.remove(record.survey.source);
    }
}

package system.interfaces;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;
import system.BackendType;
import system.Library;
import system.Record;
import system.localhost.LocalResponseManager;
import system.mturk.MturkResponseManager;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractResponseManager {

    final private static Logger LOGGER = Logger.getLogger(AbstractResponseManager.class);


    protected static class RecordNotFoundException extends SurveyException {
        public RecordNotFoundException() {
            super(String.format("Survey is currently uninitialized; try \"Preview HIT\" first."));
        }
    }

    final public static int maxwaittime = 60;
    private static HashMap<String, Record> manager = new HashMap<String, Record>();

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

    public static Record getRecord(Survey survey) throws IOException, SurveyException {
        synchronized (manager) {
            if (survey==null)
                throw new RecordNotFoundException();
            Record r = manager.get(survey.sid);
            return r;
        }
    }

    public static boolean existsRecordForSurvey(Survey survey){
        synchronized (manager) {
            return manager.containsKey((String) survey.sid);
        }
    }

    public static void putRecord(Survey survey, Library lib, BackendType backend) throws SurveyException{
        synchronized (manager) {
            Record r = new Record(survey, lib, backend);
            manager.put(survey.sid, r);
        }
    }

    public static void putRecord(Survey survey, Record record) {
        synchronized (manager) {
            manager.put(survey.sid, record);
        }
    }

    public static void removeRecord(Record record) {
        synchronized (manager) {
            manager.remove(record.survey.sid);
        }
    }

    public static void waitOnManager() throws InterruptedException {
        manager.wait();
    }

    public static void wakeup(){
        try {
            manager.notifyAll();
        } catch (IllegalMonitorStateException imse) {
            LOGGER.warn(imse);
        }
    }

    public static boolean existsTaskForRecord(Survey survey) {
        synchronized (manager) {
            Record r = manager.get(survey.sid);
            if (r==null)
                return false;
            return r.getLastTask() != null;
        }
    }

    public static SurveyResponse parseResponse (String workerId, String ansXML, Survey survey, Record r, Map<String, String> otherValues) throws SurveyException, ParserConfigurationException, SAXException, DocumentException, IOException {
        if (otherValues==null)
            return new SurveyResponse(survey, workerId, ansXML, r, new HashMap<String, String>());
        else return new SurveyResponse(survey, workerId, ansXML, r, otherValues);
    }

    public static AbstractResponseManager makeResponseManagerForType(BackendType backendType) {
        switch (backendType) {
            case LOCALHOST:
                return new LocalResponseManager();
            case MTURK:
                return new MturkResponseManager();
            default:
                throw new RuntimeException("Unsupported backend type " + backendType.name());
        }
    }
}

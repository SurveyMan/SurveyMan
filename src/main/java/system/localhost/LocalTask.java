package system.localhost;

import survey.exceptions.SurveyException;
import util.Gensym;
import interstitial.Record;
import interstitial.ITask;

import java.io.IOException;

public class LocalTask implements ITask {

    private static Gensym gensym = new Gensym("task");

    private String taskid;
    private Record record;

    public LocalTask(Record record) throws IOException, SurveyException {
        this.taskid = gensym.next();
        this.record = record;
        record.addNewTask(this);
    }

    public LocalTask(Record record, String taskid) throws IOException, SurveyException {
        this(record);
;       this.taskid = taskid;
    }
    @Override
    public String getTaskId() {
        return taskid;
    }

    @Override
    public Record getRecord() {
        return record;
    }

    @Override
    public void setRecord(Record record) {
        this.record = record;
        this.record.addNewTask(this);
    }
}

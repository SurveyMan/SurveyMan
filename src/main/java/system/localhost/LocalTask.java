package system.localhost;

import survey.Survey;
import survey.SurveyException;
import system.Gensym;
import system.Record;
import system.interfaces.Task;

import java.io.IOException;

public class LocalTask implements Task {

    private static Gensym gensym = new Gensym("task");

    private String taskid;
    private Record record;

    public LocalTask(Record record) throws IOException, SurveyException {
        this.taskid = gensym.next();
        this.record = record;
        record.addNewTask(this);
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

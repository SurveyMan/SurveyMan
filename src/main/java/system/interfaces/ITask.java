package system.interfaces;

import system.Record;

public interface ITask {

    public String getTaskId();
    public Record getRecord();
    public void setRecord(Record record);

}

package system.interfaces;

import system.Record;

public interface Task {

    public String getTaskId();
    public Record getRecord();
    public void setRecord(Record record);

}

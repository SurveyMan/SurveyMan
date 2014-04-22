package system.mturk;

import com.amazonaws.mturk.requester.HIT;
import system.Record;
import system.interfaces.Task;

public class MturkTask implements Task {

    protected final HIT hit;
    private Record record;

    public MturkTask(HIT hit, Record record) {
        this.hit = hit;
        this.record = record;
        record.addNewTask(this);
    }

    public MturkTask(HIT hit){
        this.hit = hit;
    }

    public String getTaskId(){
        return hit.getHITId();
    }

    @Override
    public Record getRecord() {
        return record;
    }

    public void setRecord(Record r) {
        this.record = r;
        this.record.addNewTask(this);
    }
}

package system.mturk;

import com.amazonaws.mturk.requester.HIT;
import interstitial.Record;
import interstitial.ITask;

public class MturkTask implements ITask {

    public final HIT hit;
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

    @Override
    public boolean equals(Object o) {
        return hit.getHITId().equals(((MturkTask) o).getTaskId())
                && record.rid.equals(((MturkTask) o).getRecord().rid);
    }

    @Override
    public int hashCode() {
        return hit.getHITId().hashCode() ^ record.rid.hashCode();
    }
}

package system.mturk;

import com.amazonaws.mturk.requester.HIT;

public class MturkTask {

    protected final HIT hit;

    public MturkTask(HIT hit) {
        this.hit = hit;
    }

    public String getTaskId(){
        return hit.getHITId();
    }
}

package system.mturk;

import com.amazonaws.mturk.requester.HIT;
import system.interfaces.Task;

public class MturkTask implements Task {

    protected final HIT hit;

    public MturkTask(HIT hit) {
        this.hit = hit;
    }

    public String getTaskId(){
        return hit.getHITId();
    }
}

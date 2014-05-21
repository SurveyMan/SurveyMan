package interstitial;

import java.lang.reflect.Method;

public class BoxedBool {
    private boolean interrupt;
    private String reason;
    public BoxedBool(boolean interrupt){
        this.interrupt = interrupt;
    }
    public void setInterrupt(boolean bool, String reason, Method caller){
        System.out.println(String.format("Interrupt in %s: %s", caller.getName(),reason));
        this.interrupt = bool;
        this.reason = reason;
    }
    public boolean getInterrupt(){
        return interrupt;
    }
}

package interstitial;

import java.lang.reflect.Method;

public class BoxedBool {
    private boolean interrupt;
    private String reason;
    public BoxedBool(boolean interrupt){
        this.interrupt = interrupt;
    }
    public void setInterrupt(boolean bool, String reason, Method caller){
        String source = "";
        if (caller!=null)
            source = caller.getName();
        System.out.println(String.format("Interrupt in %s: %s", source, reason));
        this.interrupt = bool;
        this.reason = reason;
    }
    public void setInterrupt(boolean bool, String reason) {
        setInterrupt(bool, reason, null);
    }
    public boolean getInterrupt(){
        return interrupt;
    }
}

package interstitial;

import util.Printer;

public class BoxedBool {
    private boolean interrupt;
    public BoxedBool(){
        this.interrupt = false;
    }
    public void setInterrupt(boolean bool, String reason, StackTraceElement caller){
        String source = "";
        if (caller!=null)
            source = caller.getMethodName();
        Printer.println(String.format("Interrupt in %s: %s", source, reason));
        this.interrupt = bool;
    }
    public void setInterrupt(boolean bool, String reason) {
        setInterrupt(bool, reason, null);
    }
    public boolean getInterrupt(){
        return interrupt;
    }
}

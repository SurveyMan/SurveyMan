package interstitial;

public class BoxedBool {
    private boolean interrupt;
    public BoxedBool(boolean interrupt){
        this.interrupt = interrupt;
    }
    public void setInterrupt(boolean bool){
        System.out.println("setInterrupt!");
        this.interrupt = bool;
    }
    public boolean getInterrupt(){
        return interrupt;
    }
}

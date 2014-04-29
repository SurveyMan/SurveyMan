package interstitial;

import survey.Component;

public class OptTuple {
    public Component c;
    public Integer i;
    public OptTuple(Component c, Integer i) {
        this.c = c; this.i = i;
    }
    @Override
    public boolean equals(Object that){
        if (that instanceof OptTuple) {
            return this.c.equals(((OptTuple) that).c);
        } else return false;
    }
}

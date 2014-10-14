package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.survey.Component;

import java.util.ArrayList;
import java.util.List;

public class OptTuple {
    public Component c;
    public Integer i;
    public OptTuple(Component c, Integer i) {
        this.c = c; this.i = i;
    }

    public static List<String> getCids(List<OptTuple> optTuples){
        List<String> cids = new ArrayList<String>();
        for (OptTuple optTuple : optTuples) {
            cids.add(optTuple.c.getCid());
        }
        return cids;
    }

    @Override
    public boolean equals(Object that){
        if (that instanceof OptTuple) {
            return this.c.equals(((OptTuple) that).c);
        } else return false;
    }
}

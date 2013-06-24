package survey;

import Component;
import Response;

public class Question {

    private static final Gensym gensym = new Gensym("q");
    public final String quid = gensym.next();

    public Component data;
    public Map<String, Component> options;
    public List<int> sourceLineNos;
    public final Question branchLeft;
    public final Question branchRight;
    public final boolean exclusive;
    public final boolean ordered;
    public final boolean perturb;

    public List<Response> responses;

    public void randomize() {
        // randomizes options, if permitted
        assert checkResponses();
        return;
    }

    public void sort() {
        // sorts options
        assert checkResponses();
        return;
    }

    public List<int> histogram() {
        assert checkResponses();
        return new List<int>();
    }

    private boolean checkResponses(){
        if (responses != null){
            for (Response r : responses) {
                assert r.quid == this.quid;
                for (String oid : r.oids) {
                    assert options.containsKey(oid);
                }
            }
            return true;
        } else return true;
    }

    public static void main(String[] args){
        // write test code here
    }


}
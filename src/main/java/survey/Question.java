package survey;

import java.util.ArrayList;
import survey.Component;
import survey.Response;
import utils.Gensym;
import java.util.List;
import java.util.Map;

public class Question {

    private static final Gensym gensym = new Gensym("q");
    public final String quid = gensym.next();

    public Component data;
    public Map<String, Component> options;
    public List<Integer> sourceLineNos = new ArrayList<Integer>();
    public Block block;
    public Question branchLeft;
    public Question branchRight;
    public Boolean exclusive;
    public Boolean ordered;
    public Boolean perturb;

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

    public int[] histogram() {
        assert checkResponses();
        return new int[0];
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

    public String toString() {
        return data.toString();
    }

    public static void main(String[] args){
        // write test code here
    }


}
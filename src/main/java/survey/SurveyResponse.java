package survey;

import java.util.ArrayList;
import java.util.List;
import utils.Gensym;

public class SurveyResponse {
    public static final Gensym gensym = new Gensym("sr");
    public final String srid = gensym.next();
    // this gets filled out in surveyposter.parse
    List<Response> responses = new ArrayList<Response>();
}

class Response {

    public String sid;
    public String quid;
    public List<String> oids;
    public int indexSeen; // the index at which this question was seen.

    public static void main(String[] args){
        // write test code here
    }
}
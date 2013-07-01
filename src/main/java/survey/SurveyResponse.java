package survey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.Gensym;

public class SurveyResponse {
    public static final Gensym gensym = new Gensym("sr");
    public final String srid = gensym.next();
    // this gets filled out in surveyposter.parse
    List<QuestionResponse> responses = new ArrayList<QuestionResponse>();

    /** otherValues is a map of the key value pairs that are not necessary for QC,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    Map<String, String> otherValues = new HashMap<String, String>();
}

class QuestionResponse {

    public String sid;
    public String quid;
    public List<String> oids;
    public int indexSeen; // the index at which this question was seen.

    /** otherValues is a map of the key value pairs that are not necessary for QC,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    Map<String, String> otherValues = new HashMap<String, String>();

    public static void main(String[] args){
        // write test code here
    }
}
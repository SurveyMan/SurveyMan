package survey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import utils.Gensym;

public class SurveyResponse {
    public static final Gensym gensym = new Gensym("sr");
    public final String srid = gensym.next();
    //to differentiate real/random responses (for testing)
    public boolean real; 
    // this gets filled out in surveyposter.parse
    List<QuestionResponse> responses = new ArrayList<QuestionResponse>();
    
    /*public ArrayList<String> getResponses(){
        ArrayList<ArrayList<String>> oids = new ArrayList<>(responses.size());
        for(int x=0; x<responses.size(); x++){
            oids[x]
        }
    }*/
    
    public SurveyResponse randomResponse(Survey s){
        Random r = new Random();
        SurveyResponse sr = new SurveyResponse();
        for(Question q: s.questions){
            QuestionResponse qr = new QuestionResponse();
            String[] keys = q.options.keySet().toArray(new String[0]);
            int randIndex=r.nextInt(keys.length);
            qr.oids = new ArrayList<String>();
            qr.quid=q.quid;
            qr.oids.add(q.options.get(keys[randIndex]).oid);
            sr.responses.add(qr);
        }
        sr.real=false;
        return sr;
    }
    
    public SurveyResponse consistentResponse(Survey s){
        SurveyResponse sr = new SurveyResponse();
        for(Question q: s.questions){
            QuestionResponse qr = new QuestionResponse();
            String[] keys = q.options.keySet().toArray(new String[0]);
            /*for(String z: keys){
                System.out.println(z + ", "+q.options.get(z).getClass());
            }*/
            qr.quid=q.quid;
            qr.oids = new ArrayList<String>();
            if(keys.length>0){
                qr.oids.add(q.options.get(keys[0]).oid);
            }else{
                System.out.println("No options");
            }
            sr.responses.add(qr);
        }
        sr.real=true;
        return sr;
    }

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
package survey;

import com.amazonaws.mturk.requester.Assignment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.log4j.Logger;

import utils.Gensym;
import scalautils.AnswerParse;
import scalautils.Response;


public class SurveyResponse {

    public static final Logger LOGGER = Logger.getLogger("survey");

    public static final Gensym gensym = new Gensym("sr");
    public final String srid = gensym.next();

    public String workerId = "";
    public boolean recorded = false;
    public List<QuestionResponse> responses = new ArrayList<QuestionResponse>();
    //to differentiate real/random responses (for testing)
    public boolean real; 
    /** otherValues is a map of the key value pairs that are not necessary for QC,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    public static Map<String, String> otherValues = new HashMap<String, String>();
    
    public SurveyResponse (Survey s, Assignment a) throws SurveyException{
        this.workerId = a.getWorkerId();
        otherValues.put("acceptTime", a.getAcceptTime().toString());
        //otherValues.put("approvalTime", a.getApprovalTime().toString());
        //otherValues.put("rejectionTime", a.getRejectionTime().toString());
        //otherValues.put("requesterFeedback", a.getRequesterFeedback().toString());
        otherValues.put("submitTime", a.getSubmitTime().toString());
        ArrayList<Response> rawResponses = AnswerParse.parse(s, a);
        for (Response r : rawResponses) {
            Question q = s.getQuestionById(r.quid());
            List<Component> opts = new ArrayList<Component>();
            for (String oid : r.opts())
                if (! oid.equals(""))
                    opts.add(q.getOptById(oid));
            System.out.println("opts:"+opts);
            this.responses.add(new QuestionResponse(q, opts, r.indexSeen()));
        }
    }
    
     // constructor without all the Mechanical Turk stuff (just for testing)
    public SurveyResponse(String wID){
        workerId = wID;
    }
    
    public static String outputHeaders(Survey survey, String sep) {
        StringBuilder s = new StringBuilder();
        s.append(String.format("responseid%1$sworkerid%1$ssurveyid%1$squestionid%1$squestiontext%1$soptionid%1$soptiontext", sep));
        for (String header : survey.otherHeaders)
            s.append(String.format("%s%s", sep, header));
        Set<String> keys = otherValues.keySet();
        Collections.sort(Arrays.asList(keys.toArray(new String[keys.size()])));
        for (String key : keys)
            s.append(String.format("%s%s", sep, key));
        s.append("\r\n");
        LOGGER.info("headers:" + s.toString());
        return s.toString();
    }

    public String toString(Survey survey, String sep) {
        StringBuilder extras = new StringBuilder();
        Set<String> keys = otherValues.keySet();
        Collections.sort(Arrays.asList(keys.toArray(new String[keys.size()])));
        for(String key : keys){
            extras.append(sep);
            extras.append(otherValues.get(key));
        }
        StringBuilder retval = new StringBuilder();
        for (QuestionResponse qr : responses) {
            StringBuilder qtext = new StringBuilder();
            for (Component c : qr.q.data) {
                qtext.append("<p>"+c.toString()+"</p>");
            }
            qtext.insert(0, "\"");
            qtext.append("\"");
            for (Component opt : qr.opts) {
                String otext;
                if (opt instanceof URLComponent)
                    otext = ((URLComponent) opt).data.toString();
                else otext = ((StringComponent) opt).data.toString();
                otext = "\"" + otext + "\"";
                retval.append(String.format("%2$s%1$s" + "%3$s%1$s" + "%4$s%1$s" + "%5$s%1$s" + "%6$s%1$s" + "%7$s%1$s" + "%8$s"
                        , sep
                        , srid
                        , workerId
                        , survey.sid
                        , qr.q.quid
                        , qtext.toString()
                        , opt.cid
                        , otext));
                for (String header : survey.otherHeaders)
                    retval.append(String.format("%s%s", sep, qr.q.otherValues.get(header)));
                retval.append(String.format("%s%s\r\n", sep, extras.toString()));
            }
        }
        return retval.toString();
    }
    
    // this gets filled out in surveyposter.parse

    @Override
    public String toString() {
        String retval = "\nResponse for worker " + workerId + ":\n";
        for (QuestionResponse qr : responses)
            retval = retval + "\t" + qr.toString();
        return retval;
    }
<<<<<<< HEAD
=======
        
//    public SurveyResponse (Survey s, Assignment a) throws SurveyException {
//    // this gets filled out in surveyposter.parse
   
    /*public ArrayList<String> getResponses(){
        ArrayList<ArrayList<String>> oids = new ArrayList<>(responses.size());
        for(int x=0; x<responses.size(); x++){
            oids[x]
        }
    }*/

     public SurveyResponse (Survey s, Assignment a) throws SurveyException{
        this.workerId = a.getWorkerId();
        //otherValues.put("acceptTime", a.getAcceptTime().toString());
        //otherValues.put("approvalTime", a.getApprovalTime().toString());
        //otherValues.put("rejectionTime", a.getRejectionTime().toString());
        //otherValues.put("requesterFeedback", a.getRequesterFeedback().toString());
        //otherValues.put("submitTime", a.getSubmitTime().toString());
        ArrayList<Response> rawResponses = AnswerParse.parse(s, a);
        for (Response r : rawResponses) {
            Question q = s.getQuestionById(r.quid());
            List<Component> opts = new ArrayList<Component>();
            for (String oid : r.opts())
                if (! oid.equals(""))
                    opts.add(q.getOptById(oid));
            LOGGER.info("opts:"+opts);
            this.responses.add(new QuestionResponse(q, opts, r.indexSeen()));
        }
    }
    
     // constructor without all the Mechanical Turk stuff (just for testing)
    public SurveyResponse(String wID){
        workerId = wID;
    }
>>>>>>> e9b5d8f7f673650e3e3ed7da6c6cefb4553bde0e
    
    public SurveyResponse randomResponse(Survey s){
        int x=0;
        Random r = new Random();
        SurveyResponse sr = new SurveyResponse(""+r.nextInt(1000));
        for(Question q: s.questions){
            x++;
            String[] keys = q.options.keySet().toArray(new String[0]);
            int randIndex=r.nextInt(keys.length);
            ArrayList<Component> chosen = new ArrayList<Component>();
            chosen.add(q.options.get(keys[randIndex]));
            QuestionResponse qr = new QuestionResponse(q, chosen, x);
            sr.responses.add(qr);
        }
        sr.real=false;
        return sr;
    }
    
    public SurveyResponse consistentResponse(Survey s){
        int x=0;
        Random r = new Random();
        SurveyResponse sr = new SurveyResponse(""+r.nextInt(1000));
        for(Question q: s.questions){
            x++;
            String[] keys = q.options.keySet().toArray(new String[0]);
            /*for(String z: keys){
                System.out.println(z + ", "+q.options.get(z).getClass());
            }*/
            ArrayList<Component> chosen = new ArrayList<Component>();
            if(keys.length>0){
                chosen.add(q.options.get(keys[0]));
            }else{
                LOGGER.info("No options");
            }
            QuestionResponse qr = new QuestionResponse(q, chosen, x);
            sr.responses.add(qr);
        }
        sr.real=true;
        return sr;
    }
        
    
    public class QuestionResponse {

        public Question q;
        public List<Component> opts = new ArrayList<Component>();
        public int indexSeen; // the index at which this question was seen.
        public boolean skipped;

        /** otherValues is a map of the key value pairs that are not necessary for QC,
         *  but are returned by the service. They should be pushed through the system
         *  and spit into an output file, unaltered.
         */
        Map<String, String> otherValues = new HashMap<String, String>();
        
        public QuestionResponse(Question q, List<Component> opts, int indexSeen){
            this.q = q;
            this.opts = opts;
            this.indexSeen = indexSeen;
        }
        
        @Override
        public String toString() {
            String retval = q.data.toString();
            for (Component c : opts) 
                retval = retval + "\n\t\t" + c.toString();
            return retval;
        }
    }
}

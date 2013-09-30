package survey;

import com.amazonaws.mturk.requester.Assignment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

import scala.Tuple2;
import utils.Gensym;
import scalautils.AnswerParse;
import scalautils.Response;
import scalautils.OptData;
import system.mturk.Record;


public class SurveyResponse {

    public static final Logger LOGGER = Logger.getLogger("survey");

    public static final Gensym gensym = new Gensym("sr");
    public final String srid = gensym.next();

    public String workerId = "";
    public boolean recorded = false;
    public List<QuestionResponse> responses = new ArrayList<QuestionResponse>();
    public static String[] defaultHeaders = new String[]{"responseid", "workerid", "surveyid"
            , "questionid", "questiontext", "questionpos"
            , "optionid", "optiontext", "optionpos"};
    public Record record;
    //to differentiate real/random responses (for testing)
    public boolean real; 
    
    /** otherValues is a map of the key value pairs that are not necessary for QC,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    public static Map<String, String> otherValues = new HashMap<String, String>();
    
    public SurveyResponse (Survey s, Assignment a, Record record) 
            throws SurveyException{
        this.workerId = a.getWorkerId();
        this.record = record;
        //otherValues.put("acceptTime", a.getAcceptTime().toString());
        //otherValues.put("approvalTime", a.getApprovalTime().toString());
        //otherValues.put("rejectionTime", a.getRejectionTime().toString());
        //otherValues.put("requesterFeedback", a.getRequesterFeedback().toString());
        //otherValues.put("submitTime", a.getSubmitTime().toString());
        ArrayList<Response> rawResponses = AnswerParse.parse(s, a);
        for (Response r : rawResponses) {
            this.responses.add(new QuestionResponse(r,s));
        }
    }
    
     // constructor without all the Mechanical Turk stuff (just for testing)
    public SurveyResponse(String wID){
        workerId = wID;
    }
    
    public static String outputHeaders(Survey survey, String sep) {
        StringBuilder s = new StringBuilder();
        s.append(defaultHeaders[0]);
        for (String header : Arrays.asList(defaultHeaders).subList(1, defaultHeaders.length))
            s.append(String.format("%s%s", sep, header));
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

    public String outputResponse(Survey survey, String sep) {
        // add extra headers at the end
        StringBuilder extras = new StringBuilder();
        Set<String> keys = otherValues.keySet();
        Collections.sort(Arrays.asList(keys.toArray(new String[keys.size()])));
        for(String key : keys){
            extras.append(sep);
            extras.append(otherValues.get(key));
        }
        StringBuilder retval = new StringBuilder();
        // loop through question responses
        for (QuestionResponse qr : responses) {
            // construct actual question text
            StringBuilder qtext = new StringBuilder();
            for (Component c : qr.q.data) 
                qtext.append("<p>"+c.toString()+"</p>");
            qtext.insert(0, "\"");
            qtext.append("\"");
            // response options
            for (Tuple2<Component, Integer> opt : qr.opts) {
                String otext;
                if (opt._1() instanceof URLComponent)
                    otext = ((URLComponent) opt._1()).data.toString();
                else otext = ((StringComponent) opt._1()).data.toString();
                otext = "\"" + otext + "\"";
                StringBuilder toWrite = new StringBuilder("%1$s");
                for (int i = 1 ; i < defaultHeaders.length ; i++)
                    toWrite.append(String.format("%s%%%d$s", sep, i+1));
                System.out.println(toWrite.toString());
                retval.append(String.format(toWrite.toString()
                        , srid
                        , workerId
                        , survey.sid
                        , qr.q.quid
                        , qtext.toString()
                        , qr.indexSeen
                        , opt._1().cid
                        , otext
                        , opt._2()));
                for (String header : survey.otherHeaders)
                    retval.append(String.format("%s%s", sep, qr.q.otherValues.get(header)));
                retval.append(String.format("%s%s\n", sep, extras.toString()));
            }
        }
        return retval.toString();
    }
    
    @Override
    public String toString() {
        String retval = "\nResponse for worker " + workerId + ":\n";
        for (QuestionResponse qr : responses)
            retval = retval + "\t" + qr.toString();
        return retval;
    }

 
    /*
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
    */
    /*
    public SurveyResponse consistentResponse(Survey s){
        int x=0;
        Random r = new Random();
        SurveyResponse sr = new SurveyResponse(""+r.nextInt(1000));
        for(Question q: s.questions){
            x++;
            String[] keys = q.options.keySet().toArray(new String[0]);
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
     */   
    
    public class QuestionResponse {

        public Question q;
        public List<Tuple2<Component, Integer>> opts;
        public int indexSeen; // the index at which this question was seen.
        public boolean skipped;

        /** otherValues is a map of the key value pairs that are not necessary for QC,
         *  but are returned by the service. They should be pushed through the system
         *  and spit into an output file, unaltered.
         */
        Map<String, String> otherValues = new HashMap<String, String>();
        
        public QuestionResponse(Response response, Survey s) throws SurveyException{
            this.q = s.getQuestionById(response.quid());
            this.indexSeen = response.qIndexSeen();
            this.opts = new ArrayList<Tuple2<Component, Integer>>();
            if (q.freetext)
                opts.add(new Tuple2<Component, Integer>(q.options.get("freetext"), 0));
            else 
                for (OptData opt : response.opts()) {
                    int optLoc = opt.optIndexSeen();
                    Component c = s.getQuestionById(q.quid).getOptById(opt.optid());
                    opts.add(new Tuple2<Component, Integer>(c, optLoc));
                }
        }
        
        public int indexOf(String optid) throws RuntimeException {
            for (Tuple2<Component, Integer> c : opts)
                if (c._1().cid.equals(optid))
                    return c._2();
            throw new RuntimeException("Didn't assign something right (in QuestionResponse in SurveyResponse)");
        }
        
        @Override
        public String toString() {
            String retval = q.data.toString();
            for (Tuple2<Component, Integer> c : opts)
                retval = retval + "\n\t\t" + c._1().toString();
            return retval;
        }
    }
}

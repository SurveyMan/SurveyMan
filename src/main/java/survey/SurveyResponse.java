package survey;

import com.amazonaws.mturk.requester.Assignment;

import java.util.*;

import org.apache.log4j.Logger;

import scala.Tuple2;
import system.Gensym;
import scalautils.AnswerParse;
import scalautils.Response;
import scalautils.OptData;
import system.mturk.Record;


public class SurveyResponse {

    public static class QuestionResponse {

        public Question q;
        public List<Tuple2<Component, Integer>> opts;
        public int indexSeen; // the index at which this question was seen.
        public boolean skipped;

        /** otherValues is a map of the key value pairs that are not necessary for QC,
         *  but are returned by the service. They should be pushed through the system
         *  and spit into an output file, unaltered.
         */
        Map<String, String> otherValues = new HashMap<String, String>();

        public QuestionResponse(Response response, Survey s, Map<String, String> otherValues)
                throws SurveyException{

            boolean custom = response.quid().startsWith("custom");
            this.opts = new ArrayList<Tuple2<Component, Integer>>();
            this.otherValues = otherValues;

            if (custom){
                this.q = new Question(-1,-1);
                this.q.data = new LinkedList<Component>();
                this.q.data.add(new StringComponent("CUSTOM", -1, -1));
                this.indexSeen = response.qIndexSeen();
                for (OptData opt : response.opts())
                    this.opts.add(new Tuple2<Component, Integer>(new StringComponent(opt.optid(), -1, -1), -1));
            } else {
                this.q = s.getQuestionById(response.quid());
                this.indexSeen = response.qIndexSeen();
                if (q.freetext)
                    opts.add(new Tuple2<Component, Integer>(q.options.get("freetext"), 0));
                else
                    for (OptData opt : response.opts()) {
                        int optLoc = opt.optIndexSeen();
                        Component c = s.getQuestionById(q.quid).getOptById(opt.optid());
                        opts.add(new Tuple2<Component, Integer>(c, optLoc));
                    }
            }
        }

        @Override
        public String toString() {
            String retval = q.data.toString();
            for (Tuple2<Component, Integer> c : opts)
                retval = retval + "\n\t\t" + c._1().toString();
            return retval;
        }
    }

    public static final Logger LOGGER = Logger.getLogger("survey");
    public static final Gensym gensym = new Gensym("sr");
    public static String[] defaultHeaders = new String[]{"responseid", "workerid", "surveyid"
            , "questionid", "questiontext", "questionpos"
            , "optionid", "optiontext", "optionpos"};


    public final String srid = gensym.next();
    public String workerId = "";
    public boolean recorded = false;
    public List<QuestionResponse> responses = new ArrayList<QuestionResponse>();
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
        otherValues.put("acceptTime", a.getAcceptTime().toString());
        otherValues.put("approvalTime", a.getApprovalTime().toString());
        otherValues.put("rejectionTime", a.getRejectionTime().toString());
        otherValues.put("submitTime", a.getSubmitTime().toString());
        ArrayList<Response> rawResponses = AnswerParse.parse(s, a);
        for (Response r : rawResponses) {
            this.responses.add(new QuestionResponse(r,s,otherValues));
        }
    }
    
     // constructor without all the Mechanical Turk stuff (just for testing)
    public SurveyResponse(String wID){
        workerId = wID;
    }
    
    public static String outputHeaders(Survey survey, String sep) {
        StringBuilder s = new StringBuilder();

        // default headers
        s.append(defaultHeaders[0]);
        for (String header : Arrays.asList(defaultHeaders).subList(1, defaultHeaders.length))
            s.append(String.format("%s%s", sep, header));

        // user-provided other headers
        if (survey.otherHeaders != null)
            for (String header : survey.otherHeaders)
                s.append(String.format("%s%s", sep, header));

        // mturk-provided other headers
        Set<String> keys = otherValues.keySet();
        Collections.sort(Arrays.asList(keys.toArray(new String[keys.size()])));
        for (String key : keys)
            s.append(String.format("%s%s", sep, key));

        s.append("\n");
        LOGGER.info("headers:" + s.toString());
        return s.toString();
    }

    public String outputResponse(Survey survey, String sep) {
        StringBuilder retval = new StringBuilder();
        StringBuilder mturkStuff = new StringBuilder();

        // get mturk data - scope is the entire response
        Set<String> keys = otherValues.keySet();
        Collections.sort(Arrays.asList(keys.toArray(new String[keys.size()])));
        for(String key : keys){
            mturkStuff.append(sep);
            mturkStuff.append(otherValues.get(key));
        }

        // loop through question responses - each question+option pair gets its own line
        for (QuestionResponse qr : responses) {

            // construct actual question text
            StringBuilder qtext = new StringBuilder();
            for (Component c : qr.q.data) 
                qtext.append("<p>"+c.toString()+"</p>");
            qtext.insert(0, "\"");
            qtext.append("\"");

            // response options
            for (Tuple2<Component, Integer> opt : qr.opts) {

                // construct actual option text
                String otext;
                if (opt._1() instanceof URLComponent)
                    otext = ((URLComponent) opt._1()).data.toString();
                else otext = ((StringComponent) opt._1()).data.toString();
                otext = "\"" + otext + "\"";

                //construct line of contents
                StringBuilder toWrite = new StringBuilder("%1$s");
                for (int i = 1 ; i < defaultHeaders.length ; i++)
                    toWrite.append(String.format("%s%%%d$s", sep, i+1));
                retval.append(String.format(toWrite.toString()
                        , srid
                        , workerId
                        , survey.sid
                        , qr.q.quid
                        , qtext.toString()
                        , qr.indexSeen
                        , opt._1().getCid()
                        , otext
                        , opt._2()));

                // add contents for user-defined headers
                if (survey.otherHeaders!=null)
                    for (String header : survey.otherHeaders)
                        retval.append(String.format("%s%s", sep, qr.q.otherValues.get(header)));

                //add contents for mturk-defined headers
                if (!mturkStuff.toString().isEmpty())
                    retval.append(String.format("%s%s", sep, mturkStuff.toString()));

                retval.append("\r\n");
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
    

}

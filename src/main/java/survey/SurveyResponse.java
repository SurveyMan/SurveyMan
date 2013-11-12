package survey;

import com.amazonaws.mturk.requester.Assignment;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.*;

import org.apache.log4j.Logger;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

import scala.Tuple2;
import system.Gensym;
import scalautils.AnswerParse;
import scalautils.Response;
import scalautils.OptData;
import system.mturk.Record;


public class SurveyResponse {

    public static class QuestionResponse {

        public static final String newline = SurveyResponse.newline;
      
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

            boolean custom = customQuestion(response.quid());
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
                if (q.freetext){
                    String val = response.opts().get(0).optid();
                    opts.add(new Tuple2<Component, Integer>(new StringComponent(val, -1, -1), 0));
                } else
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
                retval = retval + newline + "\t\t" + c._1().toString();
            return retval;
        }
    }

    public static final Logger LOGGER = Logger.getLogger("survey");
    public static final Gensym gensym = new Gensym("sr");
    public static String[] defaultHeaders = new String[]{"responseid", "workerid", "surveyid"
            , "questionid", "questiontext", "questionpos"
            , "optionid", "optiontext", "optionpos"};
    public static final String newline = "\r\n";
    public static final String dateFormat = "EEE, d MMM yyyy HH:mm:ss Z";
    public static final String sep = ",";

    public String srid = gensym.next();
    public String workerId = "";
    public boolean recorded = false;
    public List<QuestionResponse> responses = new ArrayList<QuestionResponse>();
    public Record record;
    //to differentiate real/random responses (for testing)
    public boolean real = true;
    public double score;
    public String msg;
    
    /** otherValues is a map of the key value pairs that are not necessary for QC,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    public static Map<String, String> otherValues = new HashMap<String, String>();

    public SurveyResponse (Survey s, Assignment a, Record record)
            throws SurveyException{
        this.workerId = a.getWorkerId();
        this.record = record;
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        otherValues.put("acceptTime", String.format("\"%s\"", format.format(a.getAcceptTime().getTime())));
        otherValues.put("submitTime", String.format("\"%s\"", format.format(a.getSubmitTime().getTime())));
        ArrayList<Response> rawResponses = AnswerParse.parse(s, a);
        for (Response r : rawResponses) {
            this.responses.add(new QuestionResponse(r,s,otherValues));
        }
    }
    
     // constructor without all the Mechanical Turk stuff (just for testing)
    public SurveyResponse(String wID){
        workerId = wID;
    }

    public static boolean customQuestion(String quid) {
        return quid.startsWith("custom") || quid.contains("-1");
    }
    
        
    public static List<SurveyResponse> readSurveyResponses (Survey s, String filename) 
            throws FileNotFoundException, IOException, SurveyException{
        List<SurveyResponse> responses = new LinkedList<SurveyResponse>();
        final CellProcessor[] cellProcessors = new CellProcessor[] {
                  new StrRegEx("sr[0-9]+") //srid
                , null // workerid
                , null  //surveyid
                , new StrRegEx("q_-?[0-9]+_-?[0-9]+") // quid
                , null //qtext
                , new ParseInt() //qloc
                , new StrRegEx("comp_-?[0-9]+_-?[0-9]+") //optid
                , null //opttext
                , new ParseInt() // oloc
                //, new ParseDate(dateFormat)
                //, new ParseDate(dateFormat)
        };
        ICsvMapReader reader = new CsvMapReader(new FileReader(filename), CsvPreference.STANDARD_PREFERENCE);
        final String[] header = reader.getHeader(true);
        Map<String, Object> headerMap;
        SurveyResponse sr = null;
        while ((headerMap = reader.read(header, cellProcessors)) != null) {
            if (sr==null || !sr.srid.equals(headerMap.get("responseid"))){
                // add this to the list of responses and create a new one
                if (sr!=null) responses.add(sr);
                sr = new SurveyResponse("");
                sr.srid = (String) headerMap.get("responseid");
            }  
            Response r = new Response((String) headerMap.get("questionid")
                    , (Integer) headerMap.get("questionpos")
                    , new ArrayList<OptData>()
                );
            Map<String, String> o = new HashMap<String, String>();
            //o.put("acceptTime", (String) headerMap.get("acceptTime"));
            //o.put("submitTime", (String) headerMap.get("submitTime"));
            QuestionResponse response = new QuestionResponse(r,s,o);
            for (QuestionResponse qr : sr.responses)
                if (qr.q.quid.equals(headerMap.get("questionid"))) {
                    response = qr;
                    break;
                }
            Component c;
            if (!customQuestion(response.q.quid))
                c = response.q.getOptById((String) headerMap.get("optionid"));
            else c = new StringComponent((String) headerMap.get("optionid"), -1, -1);
            Integer i = (Integer) headerMap.get("optionpos");
            response.opts.add(new Tuple2<Component, Integer>(c,i));
            sr.responses.add(response);
        }
        reader.close();
        return responses;
    }
    
    public static String outputHeaders(Survey survey) {
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

        s.append("\r\n");
        LOGGER.info("headers:" + s.toString());
        return s.toString();
    }

    public String outputResponse(Survey survey, String sep) {
        StringBuilder retval = new StringBuilder();
        StringBuilder mturkStuff = new StringBuilder();

        // get mturk data - scope is the entire response
        Set<String> keys = otherValues.keySet();
        String[] keyArr = keys.toArray(new String[keys.size()]);
        if (keyArr.length > 0) {
            mturkStuff.append(otherValues.get(keyArr[0]));
            for (int i = 1 ; i < keyArr.length ; i++) {
                String key = keyArr[i];
                mturkStuff.append(sep);
                mturkStuff.append(otherValues.get(key));
            }
        }
        
        // loop through question responses - each question+option pair gets its own line
        for (QuestionResponse qr : responses) {

            // construct actual question text
            StringBuilder qtext = new StringBuilder();
            for (Component c : qr.q.data) 
                qtext.append(String.format("<p>%s</p>", c.toString()));
            qtext.insert(0, "\"");
            qtext.append("\"");

            // response options
            for (Tuple2<Component, Integer> opt : qr.opts) {

                // construct actual option text
                String otext = "";
                if (opt._1() instanceof URLComponent)
                    otext = ((URLComponent) opt._1()).data.toString();
                else if (opt._1() instanceof StringComponent)
                    otext = ((StringComponent) opt._1()).data.toString();
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
                if (survey.otherHeaders!=null) {
                    retval.append(survey.otherHeaders[0]);
                    for (int i = 1 ; i < survey.otherHeaders.length ; i++){
                        String header = survey.otherHeaders[i];
                        retval.append(String.format("%s%s", sep, qr.q.otherValues.get(header)));
                    }
                }
                //add contents for mturk-defined headers
                if (!mturkStuff.toString().isEmpty())
                    retval.append(String.format("%s%s", sep, mturkStuff.toString()));

                retval.append(newline);
            }
        }
        return retval.toString();
    }
    

}

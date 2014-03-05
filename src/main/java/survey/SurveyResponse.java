package survey;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gui.SurveyMan;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import system.Gensym;
import system.Record;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class SurveyResponse {

    public static class OptTuple {
        public Component c;
        public Integer i;
        public OptTuple(Component c, Integer i) {
            this.c = c; this.i = i;
        }
    }

    public static class QuestionResponse {

        public static final String newline = SurveyResponse.newline;

        public Question q;
        public List<OptTuple> opts = new ArrayList<OptTuple>();
        public int indexSeen;

        public QuestionResponse(){

        }

        public QuestionResponse(Survey s, String quid, int qpos) throws SurveyException {
            this.q = s.getQuestionById(quid);
            this.indexSeen = qpos;
        }

        /** otherValues is a map of the key value pairs that are not necessary for QC,
         *  but are returned by the service. They should be pushed through the system
         *  and spit into an output file, unaltered.
         */
        public Map<String, String> otherValues;

        public void add(String quid, OptTuple tupe, Map<String, String> otherValues) {
            this.otherValues = otherValues;
            if (this.q == null) {
                this.q = new Question(-1,-1);
                this.q.quid = quid;
            }
            this.opts.add(tupe);
            this.indexSeen = -1;
        }

        public void add(JsonObject response, Survey s, Map<String,String> otherValues) throws SurveyException {

            boolean custom = customQuestion(response.get("quid").getAsString());

            if (this.otherValues == null)
                this.otherValues = otherValues;
            else
                assert(this.otherValues.equals(otherValues));

            if (custom){
                this.q = new Question(-1,-1);
                this.q.data = new LinkedList<Component>();
                this.q.data.add(new StringComponent("CUSTOM", -1, -1));
                this.indexSeen = response.get("qpos").getAsInt();
                this.opts.add(new OptTuple(new StringComponent(response.get("oid").getAsString(), -1, -1), -1));
            } else {
                this.q = s.getQuestionById(response.get("quid").getAsString());
                this.indexSeen = response.get("qpos").getAsInt();
                if (q.freetext){
                } else {
                    Component c = s.getQuestionById(q.quid).getOptById(response.get("oid").getAsString());
                    int optloc = response.get("opos").getAsInt();
                    this.opts.add(new OptTuple(c, optloc));
                }
            }
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

    public static ArrayList<QuestionResponse> parse(Survey s, String ansXML)
            throws DocumentException, SurveyException, ParserConfigurationException, IOException, SAXException {
        ArrayList<QuestionResponse> retval = new ArrayList<QuestionResponse>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(ansXML.getBytes("utf-8"))));
        NodeList answers = doc.getElementsByTagName("Answer");
        for ( int i = 0 ; i < answers.getLength() ; i++ ) {
            Node n = answers.item(i);
            Element e = (Element) n;
            String quid = e.getElementsByTagName("QuestionIdentifier").item(0).getTextContent();
            String opts = e.getElementsByTagName("FreeText").item(0).getTextContent();
            QuestionResponse questionResponse = new QuestionResponse();
            if (quid.equals("commit"))
                continue;
            else if (quid.endsWith("Filename")) {
                questionResponse.add(quid, new OptTuple(new StringComponent(opts, -1, -1), -1), otherValues);
            } else {
                String[] optionStuff = opts.split("\\|");
                for (String optionJSON : optionStuff) {
                    try {
                        questionResponse.add(new JsonParser().parse(optionJSON).getAsJsonObject(), s, otherValues);
                    } catch (IllegalStateException ise) {
                        LOGGER.info(ise);
                        questionResponse.add(quid, new OptTuple(new StringComponent(optionJSON, -1, -1), -1), null);
                    }
                }
                retval.add(questionResponse);
            }
        }
        return retval;
    }


    public SurveyResponse (Survey s, String workerId, String xmlAns, Record record, Map<String, String> ov)
            throws SurveyException, DocumentException, IOException, SAXException, ParserConfigurationException {
        this.workerId = workerId;
        this.record = record;
        otherValues.putAll(ov);
        this.responses = parse(s, xmlAns);
    }
    
     // constructor without all the Mechanical Turk stuff (just for testing)
    public SurveyResponse(String wID){
        workerId = wID;
    }

    public static boolean customQuestion(String quid) {
        return quid.startsWith("custom") || quid.contains("-1");
    }

    public static List<SurveyResponse> readSurveyResponses (Survey s, String filename) throws SurveyException {
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
                , new ParseDate(dateFormat)
                , new ParseDate(dateFormat)
        };
        try{
            ICsvMapReader reader = new CsvMapReader(new FileReader(filename), CsvPreference.STANDARD_PREFERENCE);
            final String[] header = reader.getHeader(true);
            Map<String, Object> headerMap;
            SurveyResponse sr = null;
            while ((headerMap = reader.read(header, cellProcessors)) != null) {
                // loop through one survey response (i.e. per responseid) at a time
                if ( sr == null || !sr.srid.equals(headerMap.get("responseid"))){
                    if (sr!=null)
                        // add this to the list of responses and create a new one
                        responses.add(sr);
                    sr = new SurveyResponse("");
                    sr.srid = (String) headerMap.get("responseid");
                }
                // fill out the individual question responses
                QuestionResponse questionResponse = new QuestionResponse(s, (String) headerMap.get("questionid"), (Integer) headerMap.get("questionpos"));
                for (QuestionResponse qr : sr.responses)
                    if (qr.q.quid.equals((String) headerMap.get("questionid"))) {
                        // if we already have a QuestionResponse object matching this id, set it
                        questionResponse = qr;
                        break;
                    }
                Component c;
                if (!customQuestion(questionResponse.q.quid))
                    c = questionResponse.q.getOptById((String) headerMap.get("optionid"));
                else c = new StringComponent((String) headerMap.get("optionid"), -1, -1);
                Integer i = (Integer) headerMap.get("optionpos");
                questionResponse.opts.add(new OptTuple(c,i));
                sr.responses.add(questionResponse);
            }
            reader.close();
            return responses;
        } catch (IOException io) {
            SurveyMan.LOGGER.warn(io);
        }
        return null;
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
            for (OptTuple opt : qr.opts) {

                // construct actual option text
                String otext = "";
                if (opt.c instanceof URLComponent)
                    otext = ((URLComponent) opt.c).data.toString();
                else if (opt.c instanceof StringComponent)
                    otext = ((StringComponent) opt.c).data.toString();
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
                        , opt.c.getCid()
                        , otext
                        , opt.i));

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
                System.out.println(retval.toString());
            }
        }
        return retval.toString();
    }
    

}

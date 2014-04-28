package system;

import java.io.*;
import java.util.*;

import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
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
import qc.IQuestionResponse;
import qc.ISurveyResponse;
import qc.OptTuple;
import survey.*;
import survey.exceptions.SurveyException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class SurveyResponse implements ISurveyResponse {

    public static final Logger LOGGER = Logger.getLogger("survey");
    public static final Gensym gensym = new Gensym("sr");
    public static String[] defaultHeaders = new String[]{"responseid", "workerid", "surveyid"
            , "questionid", "questiontext", "questionpos"
            , "optionid", "optiontext", "optionpos"};
    public static final String newline = "\r\n";
    public static final String dateFormat = "EEE, d MMM yyyy HH:mm:ss Z";
    public static final String sep = ",";

    private String srid = gensym.next();
    private String workerId = "";
    private boolean recorded = false;
    private List<IQuestionResponse> responses = new ArrayList<IQuestionResponse>();
    public Record record;
    //to differentiate real/random responses (for testing)
    private boolean real = true;
    private double score;
    public String msg;
    
    /** otherValues is a map of the key value pairs that are not necessary for QC,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    private static Map<String, String> otherValues = new HashMap<String, String>();

    @Override
    public List<IQuestionResponse> getResponses() {
        return responses;
    }

    @Override
    public boolean isRecorded() {
        return recorded;
    }

    @Override
    public String srid() {
        return srid;
    }

    public static ArrayList<IQuestionResponse> parse(Survey s, String ansXML)
            throws DocumentException, SurveyException, ParserConfigurationException, IOException, SAXException {
        ArrayList<IQuestionResponse> retval = new ArrayList<IQuestionResponse>();
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
                    } catch (Exception ise) {
                        LOGGER.info(ise);
                        LOGGER.info(optionJSON);
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

    public Map<String,IQuestionResponse> resultsAsMap() {
        HashMap<String,IQuestionResponse> res = new HashMap<String, IQuestionResponse>();
        for(IQuestionResponse resp : responses) {
            res.put(resp.getQuestion().quid, resp);
        }
        return Collections.unmodifiableMap(res);
    }

     // constructor without all the Mechanical Turk stuff (just for testing)
    public SurveyResponse(String wID){
        workerId = wID;
    }

    public static boolean customQuestion(String quid) {
        return quid.startsWith("custom") || quid.contains("-1");
    }

    public static CellProcessor[] makeCellProcessors(Survey s) {

        List<CellProcessor> cells = new ArrayList<CellProcessor>(Arrays.asList(new CellProcessor[] {
                new StrRegEx("sr[0-9]+") //srid
                , null // workerid
                , null  //surveyid
                , new StrRegEx("(assignmentId)|(start_)?q_-?[0-9]+_-?[0-9]+") // quid
                , null //qtext
                , new ParseInt() //qloc
                , new StrRegEx("comp_-?[0-9]+_-?[0-9]+") //optid
                , null //opttext
                , new ParseInt() // oloc
                //, new ParseDate(dateFormat)
                //, new ParseDate(dateFormat)
        }));


        for (int i = 0 ; i < s.otherHeaders.length ; i++) {
            cells.add(null);
        }

        if (!s.correlationMap.isEmpty())
            cells.add(null);

        return cells.toArray(new CellProcessor[cells.size()]);

    }


    public static List<SurveyResponse> readSurveyResponses (Survey s, String filename) throws SurveyException {

        List<SurveyResponse> responses = null;

        try {
            responses = readSurveyResponses(s, new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return responses;
    }


    public static List<SurveyResponse> readSurveyResponses (Survey s, Reader r) throws SurveyException {

        List<SurveyResponse> responses = new LinkedList<SurveyResponse>();

        final CellProcessor[] cellProcessors = makeCellProcessors(s);

        try{
            ICsvMapReader reader = new CsvMapReader(r, CsvPreference.STANDARD_PREFERENCE);
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
                IQuestionResponse questionResponse = new QuestionResponse(s, (String) headerMap.get("questionid"), (Integer) headerMap.get("questionpos"));
                for (IQuestionResponse qr : sr.responses)
                    if (qr.getQuestion().quid.equals((String) headerMap.get("questionid"))) {
                        // if we already have a QuestionResponse object matching this id, set it
                        questionResponse = qr;
                        break;
                    }
                Component c;
                if (!customQuestion(questionResponse.getQuestion().quid))
                    c = questionResponse.getQuestion().getOptById((String) headerMap.get("optionid"));
                else c = new StringComponent((String) headerMap.get("optionid"), -1, -1);
                Integer i = (Integer) headerMap.get("optionpos");
                questionResponse.getOpts().add(new OptTuple(c,i));
                sr.responses.add(questionResponse);
            }
            reader.close();
            return responses;
        } catch (IOException io) {
            io.printStackTrace();
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

        //correlation
        if (!survey.correlationMap.isEmpty())
            s.append(String.format("%s%s", sep, Survey.CORRELATION));

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
        for (IQuestionResponse qr : responses) {

            // construct actual question text
            StringBuilder qtext = new StringBuilder();
            qtext.append(String.format("%s", qr.getQuestion().data.toString().replaceAll("\"", "\"\"")));
            qtext.insert(0, "\"");
            qtext.append("\"");

            // response options
            for (OptTuple opt : qr.getOpts()) {

                // construct actual option text
                String otext = "";
                if (opt.c instanceof HTMLComponent)
                    otext = ((HTMLComponent) opt.c).data.toString();
                else if (opt.c instanceof StringComponent && ((StringComponent) opt.c).data!=null)
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
                        , qr.getQuestion().quid
                        , qtext.toString()
                        , qr.getIndexSeen()
                        , opt.c.getCid()
                        , otext
                        , opt.i));

                // add contents for user-defined headers
                if (survey.otherHeaders!=null && survey.otherHeaders.length > 0) {
                    //retval.append(survey.otherHeaders[0]);
                    for (int i = 0 ; i < survey.otherHeaders.length ; i++){
                        String header = survey.otherHeaders[i];
                        retval.append(String.format("%s\"%s\"", sep, qr.getQuestion().otherValues.get(header)));
                    }
                }

                // add correlated info
                for (Map.Entry<String, List<Question>> entry : survey.correlationMap.entrySet ()) {
                    if (entry.getValue().contains(qr.getQuestion()))
                        retval.append(String.format("%s%s", sep, entry.getKey()));
                }

                // add contents for mturk-defined headers
                if (!mturkStuff.toString().isEmpty())
                    retval.append(String.format("%s%s", sep, mturkStuff.toString()));

                retval.append(newline);
                //System.out.println(retval.toString());
            }
        }
        return retval.toString();
    }

    @Override
    public void setRecorded(boolean recorded) {
        this.recorded = recorded;
    }


}
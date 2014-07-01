package system;

import java.io.*;
import java.util.*;

import com.google.gson.JsonParser;
import interstitial.ISurveyResponse;
import interstitial.Record;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
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
import interstitial.IQuestionResponse;
import interstitial.OptTuple;
import survey.*;
import survey.exceptions.SurveyException;
import util.Gensym;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


public class SurveyResponse implements ISurveyResponse {

    public static final Logger LOGGER = Logger.getLogger("survey");
    public static final Gensym gensym = new Gensym("sr");
    public static final String dateFormat = "EEE, d MMM yyyy HH:mm:ss Z";
    public static final String sep = ",";

    protected String srid = gensym.next();
    private String workerId = "";
    private boolean recorded = false;
    private List<IQuestionResponse> responses = new ArrayList<IQuestionResponse>();
    public Record record;
    //to differentiate real/random responses (for testing)
    private boolean real = true;
    private double score;
    private double pval;
    public String msg;
    
    /** otherValues is a map of the key value pairs that are not necessary for quality control,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    private static Map<String, String> otherValues = new HashMap<String, String>();

    // constructor without all the Mechanical Turk stuff (just for testing)
    public SurveyResponse(String wID){
        this.workerId = wID;
        this.srid = wID;
    }

    public SurveyResponse (Survey s, String workerId, String xmlAns, Record record, Map<String, String> ov)
            throws SurveyException, DocumentException, IOException, SAXException, ParserConfigurationException {
        this.workerId = workerId;
        this.record = record;
        otherValues.putAll(ov);
        this.responses = parse(s, xmlAns);
    }

    @Override
    public void setScore(double score){
        this.score = score;
    }

    @Override
    public double getScore(){
        return this.score;
    }

    @Override
    public void setThreshold(double pval) {
        this.pval = pval;
    }

    @Override
    public double getThreshold() {
        return pval;
    }


    @Override
    public List<IQuestionResponse> getResponses() {
        return responses;
    }

    @Override
    public void setResponses(List<IQuestionResponse> responses) {
        this.responses = responses;
    }

    @Override
    public boolean isRecorded() {
        return recorded;
    }

    @Override
    public void setRecorded(boolean recorded) {
        this.recorded = recorded;
    }

    @Override
    public String srid() {
        return srid;
    }

    @Override
    public void setSrid(String srid){
        this.srid = srid;
    }

    @Override
    public String workerId() {
        return workerId;
    }

    @Override
    public Map<String,IQuestionResponse> resultsAsMap() {
        HashMap<String,IQuestionResponse> res = new HashMap<String, IQuestionResponse>();
        for(IQuestionResponse resp : responses) {
            assert resp.getQuestion().data!=null : resp.getQuestion().quid;
            res.put(resp.getQuestion().quid, resp);
        }
        return Collections.unmodifiableMap(res);
    }


    public List<ISurveyResponse> readSurveyResponses(Survey s, String filename) throws SurveyException {

        List<ISurveyResponse> responses = null;

        if (new File(filename).isFile()) {

            try {
                responses = readSurveyResponses(s, new FileReader(filename));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (new File(filename).isDirectory()) {

            responses = new ArrayList<ISurveyResponse>();

            for (File f : new File(filename).listFiles()) {
                try {
                    responses.addAll(readSurveyResponses(s, new FileReader(f)));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

        } else throw new RuntimeException("Unknown file or directory: "+filename);

        return responses;
    }

    @Override
    public List<ISurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException {

        List<ISurveyResponse> responses = new LinkedList<ISurveyResponse>();

        final CellProcessor[] cellProcessors = s.makeProcessorsForResponse();

        try{
            ICsvMapReader reader = new CsvMapReader(r, CsvPreference.STANDARD_PREFERENCE);
            final String[] header = reader.getHeader(true);
            Map<String, Object> headerMap;
            ISurveyResponse sr = null;
            while ((headerMap = reader.read(header, cellProcessors)) != null) {
                // loop through one survey response (i.e. per responseid) at a time
                if ( sr == null || !sr.srid().equals(headerMap.get("responseid"))){
                    if (sr!=null)
                        // add this to the list of responses and create a new one
                        responses.add(sr);
                    sr = new SurveyResponse((String) headerMap.get("workerid"));
                    sr.setSrid((String) headerMap.get("responseid"));

                }
                // fill out the individual question responses
                IQuestionResponse questionResponse = new QuestionResponse(s, (String) headerMap.get("questionid"), (Integer) headerMap.get("questionpos"));
                for (IQuestionResponse qr : sr.getResponses())
                    if (qr.getQuestion().quid.equals((String) headerMap.get("questionid"))) {
                        // if we already have a QuestionResponse object matching this id, set it
                        questionResponse = qr;
                        break;
                    }
                Component c;
                if (!Question.customQuestion(questionResponse.getQuestion().quid))
                    c = questionResponse.getQuestion().getOptById((String) headerMap.get("optionid"));
                else c = new StringComponent((String) headerMap.get("optionid"), -1, -1);
                Integer i = (Integer) headerMap.get("optionpos");
                questionResponse.getOpts().add(new OptTuple(c,i));
                sr.getResponses().add(questionResponse);
            }
            reader.close();
            return responses;
        } catch (IOException io) {
            io.printStackTrace();
        }
        return null;
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
            QuestionResponse questionResponse;
            if (quid.equals("commit"))
                continue;
            else if (!quid.startsWith("q")) {
                questionResponse = new QuestionResponse();
                questionResponse.add(quid, new OptTuple(new StringComponent(opts, -1, -1), -1), otherValues);
            } else {
                questionResponse = new QuestionResponse(s.getQuestionById(quid));
                String[] optionStuff = opts.split("\\|");
                for (String optionJSON : optionStuff) {
                    try {
                        questionResponse.add(new JsonParser().parse(optionJSON).getAsJsonObject(), s, otherValues);
                    } catch (Exception ise) {
                        System.err.println(String.format("JSON parse error: %s\nGenerating alternate entry.", ise.getMessage()));
                        // this is a hack
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

}

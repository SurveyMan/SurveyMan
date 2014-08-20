package interstitial;

import input.AbstractParser;
import org.apache.log4j.Logger;
import survey.HTMLComponent;
import survey.StringComponent;
import survey.Survey;

import java.util.*;

public class ResponseWriter {

    public static String[] defaultHeaders = new String[]{"responseid", "workerid", "surveyid"
            , "questionid", "questiontext", "questionpos"
            , "optionid", "optiontext", "optionpos"};
    public static final String sep = ",";
    private static Map<String, String> otherValues = new HashMap<String, String>();
    public static final Logger LOGGER = Logger.getLogger(ResponseWriter.class);
    public static final String newline = "\r\n";


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
            s.append(String.format("%s%s", sep, AbstractParser.CORRELATION));

        s.append("\r\n");
        LOGGER.info("headers:" + s.toString());
        return s.toString();
    }

    private static String outputQuestionResponse(Survey survey, IQuestionResponse qr, ISurveyResponse sr) {

        StringBuilder retval = new StringBuilder();

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
            otext = otext.replaceAll("\"", "\"\"");
            otext = "\"" + otext + "\"";

            //construct line of contents
            StringBuilder toWrite = new StringBuilder("%1$s");
            for (int i = 1 ; i < defaultHeaders.length ; i++)
                toWrite.append(String.format("%s%%%d$s", sep, i+1));
            retval.append(String.format(toWrite.toString()
                    , sr.srid()
                    , sr.workerId()
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
            if (!survey.correlationMap.isEmpty())
                retval.append(String.format("%s%s", sep, survey.getCorrelationLabel(qr.getQuestion())));

            retval.append(newline);

        }

        //retval.append(newline);
        return retval.toString();

    }

    public static String outputSurveyResponse(Survey survey, ISurveyResponse sr) {

        StringBuilder retval = new StringBuilder();

        for (IQuestionResponse qr : sr.getResponses())
            retval.append(outputQuestionResponse(survey, qr, sr));

        return retval.toString();
    }

    public static String outputSurveyResponses(Survey survey, List<ISurveyResponse> surveyResponses) {

        StringBuilder retval = new StringBuilder();

        for (ISurveyResponse sr : surveyResponses)
            retval.append(outputSurveyResponse(survey, sr));

        return retval.toString();

    }


}

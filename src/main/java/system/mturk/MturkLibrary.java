package system.mturk;

import scala.Tuple2;
import survey.Survey;
import survey.SurveyException;
import system.Library;

import java.io.*;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;

import org.apache.log4j.Logger;

public class MturkLibrary extends Library {

    public static class MturkNumberFormat extends NumberFormat {
        final Long minvalue;
        final Long maxvalue;
        public MturkNumberFormat(int minvalue, int maxvalue) {
            super();
            this.minvalue = new Long(minvalue);
            this.maxvalue = new Long(maxvalue);
        }
        public Number parse(String source, ParsePosition parsePosition){
            Long me;
            try {
                me = (Long) NumberFormat.getIntegerInstance().parse(source, parsePosition);
            } catch (ClassCastException cce) {
                return maxvalue;
            }
            if (me < minvalue)
                me = minvalue;
            else if (me > maxvalue)
                me = maxvalue;
            return me;
        }
        public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos){
            return NumberFormat.getIntegerInstance().format(number, toAppendTo, pos);
        }
        public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos){
            return NumberFormat.getIntegerInstance().format(number, toAppendTo, pos);
        }
    }

    public static final Logger LOGGER = Logger.getLogger("system.mturk");

    public static final String HTMLSKELETON = String.format("%1$s%2$s.metadata%2$sHTMLSkeleton.html", DIR, fileSep);
    public static final String JSSKELETON = String.format("%1$s%2$s.metadata%2$sJSSkeleton.js", DIR, fileSep);
    public static final String QUOTS = String.format("%1$s%2$s.metadata%2$squots", DIR, fileSep);
    public static final String XMLSKELETON = String.format("%1$s%2$s.metadata%2$sXMLSkeleton.xml", DIR, fileSep);

    private static final String MTURK_SANDBOX_URL = "https://mechanicalturk.sandbox.amazonaws.com?Service=AWSMechanicalTurkRequester";
    private static final String MTURK_PROD_URL = "https://mechanicalturk.amazonaws.com?Service=AWSMechanicalTurkRequester";
    private static final String MTURK_SANDBOX_EXTERNAL_HIT = "https://workersandbox.mturk.com/mturk/externalSubmit";
    private static final String MTURK_PROD_EXTERNAL_HIT = "https://www.mturk.com/mturk/externalSubmit";

    public static String MTURK_URL;
    public static String EXTERNAL_HIT;
    public static final int mintime = 30;
    public static final int maxtime = 31536000;
    public static final NumberFormat duration_formatter = new MturkNumberFormat(mintime, maxtime);
    public static final NumberFormat lifetime_formatter = new MturkNumberFormat(mintime, maxtime);

    public static Map<JobStatus, List<Tuple2<String, Properties>>> surveyDB = new HashMap<JobStatus, List<Tuple2<String,Properties>>>();

    public static void addToSurveyDB(Survey survey, JobStatus status) {
        String fileName;
        Record r = ResponseManager.manager.get(survey);
        if (status==JobStatus.INTERRUPTED) {
            fileName = Library.STATEDATADIR + Library.fileSep + survey.sourceName + ".csv";
        } else fileName = survey.source;
        Tuple2<String, Properties> tupe = new Tuple2<String, Properties>(fileName, r.parameters);
        List<Tuple2<String, Properties>> entries = surveyDB.get(status);
        entries.add(tupe);
    }

    public static void populateSurveyDB() throws IOException {
        for (JobStatus status : new JobStatus[]{JobStatus.CANCELLED, JobStatus.COMPLETED, JobStatus.INTERRUPTED})
            surveyDB.put(status, new LinkedList<Tuple2<String, Properties>>());
        BufferedReader reader = new BufferedReader(new FileReader(JOBDATAFILE));
        String line;
        while ((line = reader.readLine())!=null){
            String[] data = line.split(",");
            JobStatus status = JobStatus.valueOf(data[2]);
            String fileCSV = data[0];
            Properties props = new Properties();
            props.load(new FileReader(data[1]));
            List<Tuple2<String, Properties>> instances = surveyDB.get(status);
            instances.add(new Tuple2<String, Properties>(fileCSV, props));
        }
    }

    public static void writeSurveyDB(Record record)
            throws SurveyException, IOException {
        for (Map.Entry<JobStatus, List<Tuple2<String, Properties>>> entry : surveyDB.entrySet()) {
            Survey survey = record.survey;
            JobStatus status = entry.getKey();
            String logFile =  "";
            for (Tuple2<String, Properties> tupe : entry.getValue()){
                String fileName;
                String paramsFileName = Library.STATEDATADIR + Library.fileSep + survey.sourceName + ".properties";
                if (status==JobStatus.INTERRUPTED){
                    // only save extra state information if it's just interrupted
                    String surveyString = survey.toFileString();
                    fileName = Library.STATEDATADIR + Library.fileSep + survey.sourceName + ".csv";
                    BufferedWriter csvWriter = new BufferedWriter(new FileWriter(fileName));
                    csvWriter.write(surveyString);
                    csvWriter.close();
                    logFile = record.outputFileName;
                } else {
                    fileName = survey.source;
                }
                Record r = ResponseManager.manager.get(survey);
                synchronized (r) {
                    r.parameters.store(new BufferedWriter(new FileWriter(paramsFileName)), "");
                    Library.writeJobInfo(fileName, paramsFileName, logFile, status);
                }
            }
        }
    }

    // editable stuff gets copied

    public static void updateURL(){
        if (Boolean.parseBoolean(props.getProperty("sandbox"))) {
            MTURK_URL = MTURK_SANDBOX_URL;
            EXTERNAL_HIT = MTURK_SANDBOX_EXTERNAL_HIT;
        } else {
            MTURK_URL = MTURK_PROD_URL;
            EXTERNAL_HIT = MTURK_PROD_EXTERNAL_HIT;
        }
    }

    public static void init() {
        Library.init();
        try {
            copyIfChanged(HTMLSKELETON, ".metadata" + fileSep + "HTMLSkeleton.html");
            copyIfChanged(JSSKELETON, ".metadata" + fileSep + "JSSkeleton.js");
            copyIfChanged(QUOTS, ".metadata" + fileSep + "quots");
            copyIfChanged(XMLSKELETON, ".metadata" + fileSep + "XMLSkeleton.xml");
            updateURL();
            populateSurveyDB();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.fatal(e.getMessage());
        }
    }

}



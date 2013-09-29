package system.mturk;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.exception.InsufficientFundsException;
import com.amazonaws.mturk.service.exception.ServiceException;
import csv.CSVLexer;
import csv.CSVParser;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import qc.QC;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

public class Runner {

    public static class BoxedBool{
        private boolean interrupt;
        public BoxedBool(boolean interrupt){
            this.interrupt = interrupt;
        }
        public void setInterrupt(boolean bool){
            this.interrupt = bool;
        }
        public boolean getInterrupt(){
            return interrupt;
        }
    }

    // everything that uses ResponseManager should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.
    private static final Logger LOGGER = Logger.getLogger(Runner.class);
    private static FileAppender txtHandler;

    protected static int waitTime = 10000;

    public static void recordAllHITsForSurvey (Survey survey)
            throws IOException, SurveyException {
        //Record record = ResponseManager.getRecord(survey);
        Record record = ResponseManager.manager.get(survey);
        System.out.println("total HITs generated: "+record.getAllHITs().length);
        for (HIT hit : record.getAllHITs()) {
            String hitid = hit.getHITId();
            ResponseManager.addResponses(survey, hitid);
            String msg = String.format("adding responses for %s (%d total)"
                    , SurveyPoster.makeHITURL(hit)
                    , record.responses.size());
            System.out.println(msg);
            LOGGER.info(msg);
        }
    }

    public static Thread makeResponseGetter(final Survey survey, final BoxedBool interrupt){
        // grab responses for each incomplete survey in the responsemanager
        return new Thread(){
            @Override
            public void run(){
                while (!interrupt.getInterrupt()){
                    System.out.println("Checking for responses");
                    synchronized (ResponseManager.manager) {
                        while(ResponseManager.manager.get(survey)==null) {
                            try {
                                ResponseManager.manager.wait();
                            } catch (InterruptedException ie) { LOGGER.info(ie); }
                        }
                    }
                    while(!interrupt.getInterrupt()){
                        try {
                            recordAllHITsForSurvey(survey);
                        } catch (IOException e) {
                            e.printStackTrace(); System.exit(-1);
                        } catch (SurveyException e) {
                            e.printStackTrace(); System.exit(-1);
                        }
                        ResponseManager.chill(2);
                    }
                    // if we're out of the loop, expire and process the remaining HITs
                    System.out.println("\n\tDANGER ZONE\n");
                    ResponseManager.chill(3);
                    Record record = ResponseManager.manager.get(survey);
                    for (HIT hit : record.getAllHITs()){
                        try {
                            ResponseManager.expireHIT(hit);
                            ResponseManager.addResponses(survey, hit.getHITId());
                        } catch (Exception e) {
                            System.out.println("something in the response getter thread threw an error.");
                            e.printStackTrace();
                            ResponseManager.chill(1);
                        }
                    }
                }
            }
        };
    }

    public static boolean stillLive(Survey survey, BoxedBool interrupt) throws IOException {
        //Record record = ResponseManager.getRecord(survey);
        Record record = ResponseManager.manager.get(survey);
        boolean done = QC.complete(record.responses, record.parameters);
        System.out.println(done+" "+interrupt.getInterrupt());
        if (done){
            //interrupt.setInterrupt(true);
            return false;
        } else {
            return true;
        }
    }

    public static void saveState(HashMap responseManager){

    }

    public static void saveJob(Survey survey) {

    }

    public static void writeResponses(Survey survey, Record record){
        synchronized (record) {
            for (SurveyResponse r : record.responses) {
                if (!r.recorded) {
                    BufferedWriter bw = null;
                    System.out.println("writing "+r.srid);
                    try {
                        String sep = ",";
                        File f = new File(record.outputFileName);
                        bw = new BufferedWriter(new FileWriter(f, true));
                        if (! f.exists() || f.length()==0)
                            bw.write(SurveyResponse.outputHeaders(survey, sep));
                        for (SurveyResponse sr : record.responses) {
                            if (! sr.recorded) {
                                bw.write(sr.toString(survey, sep));
                                sr.recorded = true;
                            }
                        }
                        bw.close();
                    } catch (IOException ex) {
                        LOGGER.warn(ex);
                    } finally {
                        try {
                            bw.close();
                        } catch (IOException ex) {
                            LOGGER.warn(ex);
                        }
                    }
                }
            }
        }
    }

    public static Thread makeWriter(final Survey survey, final BoxedBool interrupt){
        //writes hits that correspond to current jobs in memory to their files
        return new Thread(){
            @Override
            public void run(){
                Record record;
                do {
                    synchronized (ResponseManager.manager) {
                        while (ResponseManager.manager.get(survey)==null) {
                            try {
                                ResponseManager.manager.wait();
                            } catch (InterruptedException ie) { LOGGER.warn(ie); }
                        }
                    }
                    record = ResponseManager.manager.get(survey);
                    writeResponses(survey, record);
                    ResponseManager.chill(3);
                } while (!interrupt.getInterrupt());
                // clean up
                synchronized (record) {
                    try {
                        record.wait();
                    } catch (InterruptedException e) { LOGGER.warn(e); }
                    writeResponses(survey, record);
                }
            }
        };
    }

    public static void run(final Survey survey, final BoxedBool interrupt) throws SurveyException, ServiceException, IOException {
        final Properties params = (Properties) MturkLibrary.props.clone();
        //ResponseManager.manager.put(survey, new Record(survey, params));
        do {
            if (!interrupt.getInterrupt() && SurveyPoster.postMore(survey)){
                Map<String, Integer> orderSeen = survey.randomize();
                List<HIT> hits;
                hits = SurveyPoster.postSurvey(survey, orderSeen);
                System.out.println("num hits posted from Runner.run "+hits.size());
                ResponseManager.chill(2);
            }
            ResponseManager.chill(2);
        } while (stillLive(survey, interrupt) && !interrupt.getInterrupt());
        ResponseManager.chill(10);
        Record record = ResponseManager.getRecord(survey);
        synchronized (record) {
            for (HIT hit : ResponseManager.listAvailableHITsForRecord(ResponseManager.getRecord(survey)))
                ResponseManager.expireHIT(hit);
        }
        interrupt.setInterrupt(true);
    }

    public static void main(String[] args)
            throws IOException, SurveyException, InterruptedException {
        SurveyPoster.init();
        if (args.length!=3) {
            System.err.println("USAGE: <survey.csv> <sep> <expire>\r\n"
                + "survey.csv  the relative path to the survey csv file from the current location of execution.\r\n"
                + "sep         the field separator (should be a single char or 2-char special char, e.g. \\t\r\n"
                + "expire      a boolean representing whether to approve, expire and delete old HITs (only recommended if you're running in sandbox!). ");
            System.exit(-1);
        }

        // LOGGING
        LOGGER.setLevel(Level.ALL);
        try {
            txtHandler = new FileAppender(new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n"), "logs/Runner.log");
            txtHandler.setEncoding(CSVLexer.encoding);
            txtHandler.setAppend(true);
            LOGGER.addAppender(txtHandler);
        }
        catch (IOException io) {
            System.err.println(io.getMessage());
            System.exit(-1);
        }
        System.out.println("Approve, expire, and delete old HITs");
        if (Boolean.parseBoolean(args[2])){
            ResponseManager.approveAllHITs();
            ResponseManager.expireOldHITs();
            ResponseManager.deleteExpiredHITs();
        }
        String file = args[0];
        String sep = args[1];

        while (true) {
            try {
                BoxedBool interrupt = new BoxedBool(false);
                Survey survey = CSVParser.parse(file, sep);
                Thread writer = makeWriter(survey, interrupt);
                Thread responder = makeResponseGetter(survey, interrupt);
                writer.start();
                responder.start();
                Runner.run(survey, interrupt);
                writer.join();
                responder.join();
                System.exit(0);
            } catch (InsufficientFundsException ife) {
                System.out.println("Insufficient funds in your Mechanical Turk account. Would you like to:\n" +
                        "[1] Add more money to your account and retry\n" +
                        "[2] Quit\n");
                int i = 0;
                while(i!=1 && i!=2){
                    System.out.println("Type number corresponding to preference: ");
                    i = new Scanner(System.in).nextInt();
                    if (i==2)
                        System.exit(1);
                }
            }
        }
    }
}

package system.mturk;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.exception.AccessKeyException;
import com.amazonaws.mturk.service.exception.InsufficientFundsException;
import com.amazonaws.mturk.service.exception.ServiceException;
import csv.CSVLexer;
import csv.CSVParser;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;
import system.Rules;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import qc.QCMetrics.FreqProb;

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
    private static final Logger LOGGER = Logger.getRootLogger();
    private static FileAppender txtHandler;
    private static int totalHITsGenerated;

    public static int recordAllHITsForSurvey (Survey survey)
            throws IOException, SurveyException {
        //Record record = ResponseManager.getRecord(survey);
        Record record = ResponseManager.manager.get(survey.sid);
        int allHITs = record.getAllHITs().length;
        String hiturl = "", msg = "";
        int responsesAdded = 0;
        for (HIT hit : record.getAllHITs()) {
            hiturl = SurveyPoster.makeHITURL(hit);
            responsesAdded = ResponseManager.addResponses(survey, hit);
        }
        msg = String.format("Polling for responses for HITs at %s (%d total)"
                , hiturl
                , record.responses.size());
        if (allHITs > totalHITsGenerated) {
            System.out.println("total HITs generated: "+record.getAllHITs().length);
            totalHITsGenerated = allHITs;
            System.out.println(msg);
        }
        LOGGER.info(msg);
        return responsesAdded;
    }

    public static Thread makeResponseGetter(final Survey survey, final BoxedBool interrupt){
        // grab responses for each incomplete survey in the responsemanager
        return new Thread(){
            @Override
            public void run(){
                int waittime = 1;
                while (!interrupt.getInterrupt()){
                    System.out.println("Checking for responses");
                    while(!interrupt.getInterrupt()){
                        try {
                            int n = recordAllHITsForSurvey(survey);
                            if (n > 0)
                                waittime = 2;
                        } catch (IOException e) {
                            e.printStackTrace(); System.exit(-1);
                        } catch (SurveyException e) {
                            e.printStackTrace(); System.exit(-1);
                        }
                        ResponseManager.chill(waittime);
                        if (waittime > ResponseManager.maxwaittime)
                            waittime = 1;
                        else waittime *= 2;
                    }
                    // if we're out of the loop, expire and process the remaining HITs
                    System.out.println("\n\tDANGER ZONE\n");
                    ResponseManager.chill(3);
                    Record record = ResponseManager.manager.get(survey.sid);
                    for (HIT hit : record.getAllHITs()){
                        boolean expiredAndAdded = false;
                        while (! expiredAndAdded) {
                            try {
                                ResponseManager.expireHIT(hit);
                                ResponseManager.addResponses(survey, hit);
                                expiredAndAdded = true; 
                            } catch (Exception e) {
                                System.out.println("something in the response getter thread threw an error.");
                                e.printStackTrace();
                                ResponseManager.chill(1);
                            }
                        }
                    }
                }
            }
        };
    }

    public static boolean stillLive(Survey survey) throws IOException {
        Record record = ResponseManager.manager.get(survey.sid);
        boolean done = record.qc.complete(record.responses, record.library.props);
        return ! done;
    }

    public static void writeResponses(Survey survey, Record record){
        synchronized (record) {
            for (SurveyResponse sr : record.responses) {
                if (!sr.recorded) {
                    BufferedWriter bw = null;
                    System.out.println("writing "+sr.srid);
                    try {
                        String sep = ",";
                        File f = new File(record.outputFileName);
                        bw = new BufferedWriter(new FileWriter(f, true));
                        if (! f.exists() || f.length()==0)
                            bw.write(SurveyResponse.outputHeaders(survey));
                        bw.write(sr.outputResponse(survey, sep));
                        sr.recorded = true;
                        bw.close();
                    } catch (IOException ex) {
                        LOGGER.warn(ex);
                    } finally {
                        try {
                            if (bw != null) bw.close();
                        } catch (IOException ex) {
                            LOGGER.warn(ex);
                        }
                    }
                }
            }
        }
    }

    public static void writeBots(Survey survey, Record record) {
        synchronized (record) {
            String sep = ",";
            for (int i = 0 ; i < record.botResponses.size() ; i++) {
                SurveyResponse sr = record.botResponses.get(i);
                try {
                    File f = new File(record.outputFileName+"_suspectedbots");
                    BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
                    bw.write(sr.outputResponse(survey, sep));
                    bw.close();
                } catch (IOException io) {
                    LOGGER.warn(io);
                    i--;
                }
            }
            try {
                File f = new File(record.outputFileName+"_fmap");
                BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
                FreqProb fp = new FreqProb(survey, record.responses);
                for (String quid : fp.qHistograms.keySet()){
                    bw.write(quid + "\n");
                    for (Map.Entry<String, Integer> o : fp.qHistograms.get(quid).entrySet()) {
                        bw.write(String.format("%s%s%s%s", o.getKey(), o.getValue(), ":", sep));
                    }
                    bw.write("\n");
                }
                bw.close();
            } catch (IOException io) {
                LOGGER.warn(io);
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
                        while (ResponseManager.manager.get(survey.sid)==null) {
                            try {
                                System.out.println("waiting...");
                                ResponseManager.manager.wait();
                            } catch (InterruptedException ie) { LOGGER.warn(ie); }
                        }
                    }
                    record = ResponseManager.manager.get(survey.sid);
                    writeResponses(survey, record);
                    ResponseManager.chill(3);
                } while (!interrupt.getInterrupt());
                // clean up
                synchronized (record) {
                    System.out.print("Writing straggling data...");
                    try {
                        record.wait();
                    } catch (InterruptedException e) { LOGGER.warn(e); }
                    writeResponses(survey, record);
                    writeBots(survey, record);
                    ResponseManager.manager.put(survey.sid, null);
                    System.out.println("done.");
                }
            }
        };
    }

    public static void run(final Record record, final BoxedBool interrupt)
            throws SurveyException, ServiceException, IOException, ParseException {
        Survey survey = record.survey;
        do {
            if (!interrupt.getInterrupt() && SurveyPoster.postMore(survey)){
                survey.randomize();
                List<HIT> hits;
                hits = SurveyPoster.postSurvey(record);
                System.out.println("num hits posted from Runner.run "+hits.size());
                ResponseManager.chill(2);
            }
            ResponseManager.chill(2);
        } while (stillLive(survey) && !interrupt.getInterrupt());
        ResponseManager.chill(10);
        synchronized (record) {
            for (HIT hit : ResponseManager.listAvailableHITsForRecord(ResponseManager.getRecord(survey)))
                ResponseManager.expireHIT(hit);
            ResponseManager.removeQualification(record);
        }
        interrupt.setInterrupt(true);
    }

    public static void main(String[] args)
            throws IOException, SurveyException, InterruptedException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ParseException {


        if (args.length!=3) {
            System.err.println("USAGE: <survey.csv> <sep> <expire>\r\n"
                + "survey.csv  the relative path to the survey csv file from the current location of execution.\r\n"
                + "sep         the field separator (should be a single char or 2-char special char, e.g. \\t\r\n"
                + "expire      a boolean representing whether to approve, expire and delete old HITs (only recommended if you're running in sandbox!). ");
            System.exit(-1);
        }

        // LOGGING
        try {
            txtHandler = new FileAppender(new PatternLayout("%d{dd MMM yyyy HH:mm:ss,SSS}\t%-5p [%t]: %m%n"), "logs/Runner.log");
            txtHandler.setAppend(true);
            LOGGER.addAppender(txtHandler);
        }
        catch (IOException io) {
            System.err.println(io.getMessage());
            System.exit(-1);
        }
//        System.out.println("Approve, expire, and delete old HITs");
//        if (Boolean.parseBoolean(args[2])){
//            ResponseManager.approveAllHITs();
//            ResponseManager.expireOldHITs();
//            ResponseManager.deleteExpiredHITs();
//        }
        String file = args[0];
        String sep = args[1];

        while (true) {
            try {
                BoxedBool interrupt = new BoxedBool(false);
                CSVParser csvParser = new CSVParser(new CSVLexer(file, sep));
                Survey survey = csvParser.parse();
                // make sure survey is well formed
                Rules.ensureBranchForward(survey, csvParser);
                Rules.ensureCompactness(csvParser);
                Rules.ensureNoDupes(survey);
                Rules.ensureRandomizedBlockConsistency(survey, csvParser);
                // create and store the record
                Record record = new Record(survey);
                ResponseManager.addRecord(record);
                Thread writer = makeWriter(survey, interrupt);
                Thread responder = makeResponseGetter(survey, interrupt);
                responder.setPriority(Thread.MAX_PRIORITY);
                writer.start();
                responder.start();
                Runner.run(record, interrupt);
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
            } catch (AccessKeyException aws) {
              System.out.println(String.format("There is a problem with your access keys: %s; Exiting...", aws.getMessage()));
              ResponseManager.chill(2);
              System.exit(0);
            } 
        }
    }
}

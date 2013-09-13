package system;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.exception.ServiceException;
import csv.CSVParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import qc.QC;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;
import system.mturk.MturkLibrary;
import system.mturk.SurveyPoster;
import system.mturk.ResponseManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;


public class Runner {

    // everything that uses ResponseManager should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.
    public static final Logger LOGGER = Logger.getLogger("system");
    public static int waitTime = 10000;
    public static boolean interrupt = false;
    public static int writeInterval = 30000;

    public static void pollForResponse(String hittypeid, String hitid, Properties params) {
        boolean refreshed = false;
        while (! refreshed) {
            LOGGER.info("waittime(waitForResponse):"+waitTime);
            try {
                Thread.sleep(waitTime);
                if (! ResponseManager.hasResponse(hittypeid, hitid)) {
                    if (waitTime < 5*60*60*1000) //max out at 5mins
                        waitTime = waitTime*2;
                    ResponseManager.renewIfExpired(hitid, params);
                    System.out.println("no response yet for "+hitid);
                }
                else refreshed = true;
            } catch (InterruptedException e) {}
        }
    }

    public static boolean stillLive(Survey survey) throws IOException {
        ResponseManager.Record record = ResponseManager.getRecord(survey);
        if (QC.complete(record.responses, record.parameters)){
            interrupt = true;
            return false;
        } else {
            return true;
        }
    }

    public static void saveState(HashMap responseManager){

    }

    public static void startWriter(Survey survey){
        //writes hits that correspond to current jobs in memory to their files
        new Thread(){
            @Override
            public void run(){
                while(!interrupt){
                    for (Entry<Survey, ResponseManager.Record> entry : ResponseManager.manager.entrySet()) {
                        ResponseManager.Record record;
                        Survey survey;
                        synchronized (entry) {
                             record = entry.getValue();
                             survey = entry.getKey();
                        }
                        synchronized (record) {
                            for (SurveyResponse r : record.responses) {
                                if (!r.recorded) {
                                    BufferedWriter bw = null;
                                    try {
                                        String sep = ",";
                                        File f = new File(record.outputFileName);
                                        bw = new BufferedWriter(new FileWriter(f, true));
                                        if (! f.exists() || f.length()==0)
                                            bw.write(SurveyResponse.outputHeaders(survey, sep));
                                        for (SurveyResponse sr : record.responses) {
                                            LOGGER.info("recorded?:"+sr.recorded);
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
                    try {
                        Thread.sleep(writeInterval);
                    } catch (InterruptedException ex) {
                        LOGGER.info(ex);
                    }
                }
            }
        }.start();
    }

    public static boolean postMore(Survey survey) throws IOException {
            // post more if we have less than two posted at once
        ResponseManager.Record r = ResponseManager.getRecord(survey);
        int availableHITs = ResponseManager.listAvailableHITsForRecord(r).size();
        System.out.println(availableHITs);
        return availableHITs < 2;
    }

    public static void run(final Survey survey) throws SurveyException, ServiceException, IOException {
        final Properties params = (Properties) MturkLibrary.props.clone();
        ResponseManager.manager.put(survey, new ResponseManager.Record(survey, params));
        startWriter(survey);
        while (stillLive(survey) && !interrupt) {
            if (postMore(survey)){
                survey.randomize();
                boolean notPosted = true;
                List<HIT> hits;
                while (notPosted) {
                    hits = SurveyPoster.postSurvey(survey);
                    notPosted = false;
                    for (final HIT hit : hits) {
                        new Thread() {
                            public void run(){
                                pollForResponse(hit.getHITTypeId(), hit.getHITId(), params);
                                try {
                                    synchronized (ResponseManager.manager) {
                                        ResponseManager.addResponses(ResponseManager.manager.get(survey).responses, survey, hit.getHITId());
                                    }
                                } catch (SurveyException se) {
                                    LOGGER.fatal(se);
                                    Runner.saveState(ResponseManager.manager);
                                    System.exit(-1);
                                }

                            }
                        }.start();
                    }
                    try {
                        Thread.sleep(writeInterval);
                    } catch (InterruptedException ex) {
                        LOGGER.info(ex);
                    }
                }
            } 
        }
        for (HIT hit : ResponseManager.listAvailableHITsForRecord(ResponseManager.getRecord(survey))){
            ResponseManager.expireHIT(hit);
        }
    }

    public static void main(String[] args)
            throws IOException, SurveyException {
       Logger.getRootLogger().setLevel(Level.FATAL);
       MturkLibrary.init();
       if (args.length!=3) {
           System.err.println("USAGE: <survey.csv> <sep> <expire>\r\n"
                   + "survey.csv  the relative path to the survey csv file from the current location of execution.\r\n"
                   + "sep         the field separator (should be a single char or 2-char special char, e.g. \\t\r\n"
                   + "expire      a boolean representing whether to expire old HITs. ");
           System.exit(-1);
       }
       if (Boolean.parseBoolean(args[2]))
           ResponseManager.expireOldHITs();
       String file = args[0];
       String sep = args[1];
       Runner.run(CSVParser.parse(file, sep));
    }

}

package system;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.exception.ServiceException;
import csv.CSVParser;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import qc.QC;
import survey.Question;
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
import java.util.ArrayList;
import java.util.Properties;


public class Runner {

    // everything that uses ResponseManager should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.
    public static final Logger LOGGER = Logger.getLogger("system");
    public static int waitTime = 9000;
    public static String csvFileName = "";

    public static void writeResponses(Survey survey) throws IOException {
        csvFileName = String.format("%s%s%s_%s_%s.csv"
                , MturkLibrary.OUTDIR
                , MturkLibrary.fileSep
                , survey.sourceName
                , survey.sid
                , Library.TIME);
        String sep = ",";
        File f = new File(csvFileName);
        BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
        if (! f.exists() || f.length()==0)
            bw.write(SurveyResponse.outputHeaders(survey, sep));
        if (ResponseManager.manager.get(survey)!=null) {
            for (SurveyResponse sr : ResponseManager.manager.get(survey).responses) {
                LOGGER.info("recorded?:"+sr.recorded);
                if (! sr.recorded) {
                    bw.write(sr.toString(survey, sep));
                    sr.recorded = true;
                }
            }
        }
        bw.close();
    }

    public static void waitForResponse(String hittypeid, String hitid) {
        boolean refreshed = false;
        while (! refreshed) {
            LOGGER.info("waittime(waitForResponse):"+waitTime);
            try {
                Thread.sleep(waitTime);
                if (! ResponseManager.hasResponse(hittypeid, hitid))
                    waitTime = waitTime*2;
                else refreshed = true;
            } catch (InterruptedException e) {}
        }
    }

    public static boolean stillLive(Survey survey) {
        ResponseManager.Record record = ResponseManager.manager.get(survey);
        if (QC.complete(record.responses, record.parameters))
            return false;
        else {
            return true;
        }
    }

    public static void run(Survey survey) throws SurveyException, ServiceException {
        if (!ResponseManager.manager.containsKey(survey))
            ResponseManager.manager.put(survey, new ResponseManager.Record(survey, (Properties) MturkLibrary.props.clone()));
        while (stillLive(survey)) {
            survey.randomize();
            boolean notPosted = true;
            HIT hit;
            while (notPosted) {
                hit = SurveyPoster.postSurvey(survey);
                notPosted = false;
                waitForResponse(hit.getHITTypeId(), hit.getHITId());
                ResponseManager.addResponses(ResponseManager.manager.get(survey).responses, survey, hit.getHITId());
            }
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
           SurveyPoster.expireOldHITs();
       String file = args[0];
       String sep = args[1];
       Survey survey = CSVParser.parse(file, sep);
       for (Question q : survey.questions)
           System.out.println(q.toString());
       Runner.run(survey);
       while (true) {
           writeResponses(survey);
           if (! ResponseManager.hasJobs(survey)) break;
           try {
               Thread.sleep(waitTime);
           } catch (InterruptedException ie) {}
       }
    }

}

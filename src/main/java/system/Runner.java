package system;


import com.amazonaws.mturk.requester.HIT;
import java.io.IOException;
import csv.CSVParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import qc.QC;
import survey.*;
import system.mturk.MturkLibrary;
import system.mturk.ResponseManager;
import system.mturk.SurveyPoster;

public class Runner {

    // everything that uses ResponseManager should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.
    public static HashMap<String, SurveyResponse> responses = new HashMap<String, SurveyResponse>();
    public static int waitTime = 9000;
    
    public static void writeResponses(Survey survey) throws IOException {
        String filename = MturkLibrary.OUTDIR + MturkLibrary.fileSep + survey.sourceName + "_" + survey.sid + ".csv";
        String sep = ",";
        File f = new File(filename);
        BufferedWriter bw = new BufferedWriter(new FileWriter(f, true));
        if (! f.exists() || f.length()==0)
            bw.write(SurveyResponse.outputHeaders(survey, sep));
        for (SurveyResponse sr : responses.values()) {
            System.out.println("recorded?:"+sr.recorded);
            if (! sr.recorded) {
                bw.write(sr.toString(survey, sep));
                sr.recorded = true;
            }
        }
        bw.close();
    }
    
    public static void waitForResponse(String hittypeid, String hitid) {
        boolean refreshed = false;
        while (! refreshed) {
            System.out.println("waittime(waitForResponse):"+waitTime);
            try {
                Thread.sleep(waitTime);
                if (! ResponseManager.hasResponse(hittypeid, hitid))
                    waitTime = waitTime*2;
                else refreshed = true;
            } catch (InterruptedException e) {}
        }
    }
        
    public static Thread run(final Survey survey){
        Thread runner = new Thread() {
            @Override
            public void run() {
                while (! QC.complete(responses)) {
                    try {
                        survey.randomize();
                        boolean notPosted = true;
                        HIT hit;
                        while (notPosted) {
                            try {
                                System.out.println("waittime(run):"+waitTime);
                                hit = SurveyPoster.postSurvey(survey);
                                notPosted = false;
                                waitForResponse(hit.getHITTypeId(), hit.getHITId());
                                ResponseManager.addResponses(responses, survey, hit.getHITId());
                            } catch (Exception e) {
                                System.err.println("WARNING: "+e.getMessage());
                            }
                        }
                    } catch (SurveyException se) {
                        System.err.println(se.getMessage());
                        System.exit(-1);
                    }
                }
            }
        };
        runner.start();
        return runner;
    }

    public static void main(String[] args) throws IOException {
       SurveyPoster.expireOldHITs();
       Survey survey = CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", MturkLibrary.fileSep), ":");
       for (Question q : survey.questions)
           System.out.println(q.toString());
       Thread runner = run(survey);
       while (true) {
           writeResponses(survey);
           if (! (runner.isAlive() && ResponseManager.hasJobs())) break;
           try {
               Thread.sleep(waitTime);
           } catch (InterruptedException ie) {}
       }
    }

}

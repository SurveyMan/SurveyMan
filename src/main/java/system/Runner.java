package system;


import com.amazonaws.mturk.requester.HIT;
import java.io.IOException;
import csv.CSVParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import qc.QC;
import survey.*;
import system.mturk.ResponseManager;
import system.mturk.SurveyPoster;

public class Runner {

    // everything that uses ResponseManager should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.
    public static HashMap<String, SurveyResponse> responses = new HashMap<String, SurveyResponse>();
    public static int waitTime = 90;
    
    public static void writeResponses(Survey survey, String filename, String sep) throws IOException {
        File f = new File(filename);
        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
        if (! f.exists()) {
            bw.write(SurveyResponse.outputHeaders(survey, sep));
            bw.newLine();
        } 
        for (Map.Entry<String, SurveyResponse> sr : responses.entrySet()) {
            bw.write(sr.getValue().toString(survey, sep));
            bw.newLine();
            responses.remove(sr.getKey());
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
        runner.run();
        return runner;
    }

    public static void main(String[] args) throws IOException {
       SurveyPoster.expireOldHITs();
       Survey survey = CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", Library.fileSep), ":");
       for (Question q : survey.questions)
           System.out.println(q.toString());
       Thread runner = run(survey);
       while (true) {
           writeResponses(survey, Library.OUTDIR + Library.fileSep + survey.sid, ",");
           if (! (runner.isAlive() && ResponseManager.hasJobs())) break;
       }
               
    }

}

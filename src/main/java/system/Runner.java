package system;

import com.amazonaws.mturk.addon.HITDataCSVReader;
import com.amazonaws.mturk.addon.HITDataCSVWriter;
import com.amazonaws.mturk.addon.HITDataInput;
import com.amazonaws.mturk.addon.HITTypeResults;
import java.io.File;
import java.io.IOException;

import csv.CSVParser;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import qc.QC;
import survey.Survey;
import survey.SurveyResponse;
import system.mturk.ResponseManager;
import system.mturk.SurveyPoster;
import utils.Slurpie;

public class Runner {

    // everything that uses ResponseManager should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.
    public static HashMap<String, SurveyResponse> responses = new HashMap<String, SurveyResponse>();
    
//    public static void waitForResults() throws IOException {
//        boolean resultsNotIn = true;
//        while (resultsNotIn) {
//            try {
//                Thread.sleep(2*60000);
//                ResponseManager.addResponses(responses);
//                if (QC.complete(responses)) {
//                    resultsNotIn = false;
//                    System.out.println("Results written and recorded. Exiting.");
//                } System.out.print(".");
//            } catch (InterruptedException e) {
//                System.out.print("!");
//            }
//        }
//    }
    
    public static void run(final Survey survey){
//        Thread worker = new Thread() {
//            @Override
//            public void run() {
        while (! QC.complete(responses)) {
            survey.randomize();
            boolean notPosted = true;
            String id = "";
            while (notPosted) {
                try {
                    id = SurveyPoster.postSurvey(survey);
                    notPosted = false;
                } catch (IOException ex) {}
            }
            try {
                Thread.sleep(2*60000);
            } catch (InterruptedException e) {}
            ResponseManager.addResponses(responses, survey, id);
        }
    }

    public static void main(String[] args) throws IOException {
//        SurveyPoster.postSurvey(CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", Library.fileSep), ":"));
//        ResponseManager.complete = true;
//        waitForResults();
//        System.out.println(Slurpie.slurp(ResponseManager.RESULTS));
        SurveyPoster.expireOldHITs();
        run(CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", Library.fileSep), ":"));
    }

}

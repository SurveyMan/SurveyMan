package system;

import com.amazonaws.mturk.addon.HITDataCSVReader;
import com.amazonaws.mturk.addon.HITDataCSVWriter;
import com.amazonaws.mturk.addon.HITDataInput;
import com.amazonaws.mturk.addon.HITTypeResults;
import java.io.File;
import java.io.IOException;

import csv.CSVParser;
import survey.Survey;
import system.mturk.ResponseParser;
import system.mturk.SurveyPoster;
import utils.Slurpie;

public class Runner {

    // everything that uses ResponseParser should probably use some parameterized type to make this more general
    // I'm hard-coding in the mturk stuff for now though.

    public static void waitForResults() throws IOException {
        boolean resultsNotIn = true;
        while (resultsNotIn) {
            try {
                Thread.sleep(2*60000);
                ResponseParser.getResults();
                if (ResponseParser.complete) {
                    resultsNotIn = false;
                    System.out.println("Results written and recorded. Exiting.");
                } System.out.print(".");
            } catch (InterruptedException e) {
                System.out.print("!");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        SurveyPoster.postSurvey(CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", Library.fileSep), ":"));
        ResponseParser.complete = true;
        waitForResults();
        System.out.println(Slurpie.slurp(ResponseParser.RESULTS));
    }

}

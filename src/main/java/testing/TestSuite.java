package testing;
import csv.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import survey.*;
import qc.*;

public class TestSuite{
    public static void main(String[] args){
        //String filename = args[1];
        String separator = System.getProperty("file.separator");
        System.out.println(separator);
        String filename = "data"+separator+"blah.csv";
        Survey survey1 = null;
        try {
            survey1 = csv.CSVParser.parse(filename, ",");
            System.out.println(survey1);
        } catch (IOException ex) {
            System.out.println("File not found");
        }
        
        for (Question q : survey1.questions){
            System.out.println(q.data);
            System.out.println(q.options);
        }
        
        System.out.println();
        System.out.println("Generating responses:");
        System.out.println();
        
        ArrayList<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        Random rand = new Random();
        //generate group of respondents who always pick option 1
        int numResponses = 20;
        int numRandomResponses = 5;
        for(int x=0; x<numResponses; x++){
            SurveyResponse sr = new SurveyResponse(""+rand.nextInt(1000));
            responses.add(sr.consistentResponse(survey1));
        }
        SurveyResponse curR = responses.get(0);
        
        //generate group of random respondents
        for(int x=0; x<numRandomResponses; x++){
            SurveyResponse sr = new SurveyResponse(""+rand.nextInt(1000));
            responses.add(sr.randomResponse(survey1));
        }
        
        //shuffle real and random responses
        Collections.shuffle(responses);
                
//        for(SurveyResponse r: responses){
//            System.out.println(r.toString(survey1, ","));
//            System.out.println(r.real);
//            System.out.println();
//        }
        QCMetric qc = new QCMetric();
        ArrayList<SurveyResponse> outliers = qc.entropyBootstrap(survey1, responses);
        System.out.println("~~~~~~OUTLIERS~~~~~~");
        System.out.println("# outliers: "+outliers.size());
        for(SurveyResponse o: outliers){
            System.out.println(o.toString(survey1, ","));
            System.out.println(o.real);
            System.out.println();
        }
    }
}
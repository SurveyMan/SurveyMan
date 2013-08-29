package testing;
import csv.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import survey.*;
import qc.*;

public class TestSuite{
    public static void main(String[] args) throws SurveyException{
        //String filename = args[1];
        String separator = System.getProperty("file.separator");
        //System.out.println(separator);
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
        
//        double percentRight = 0; 
//        for(double percent = 0; percent<.2; percent+=.01){
//            ArrayList<SurveyResponse> responses = new ArrayList<SurveyResponse>();
//            Random rand = new Random();
//            //generate group of respondents who always pick option 1
//            double numReal=100*(1-percent);
//            for(int x=0; x<numReal; x++){
//                SurveyResponse sr = new SurveyResponse(""+rand.nextInt(1000));
//                responses.add(sr.consistentResponse(survey1));
//            }
//            double numRand=100*percent;
//            //generate group of random respondents
//            for(int x=0; x<numRand; x++){
//                SurveyResponse sr = new SurveyResponse(""+rand.nextInt(1000));
//                responses.add(sr.randomResponse(survey1));
//            }
//
//            //shuffle real and random responses
//            Collections.shuffle(responses);
// //            int c=0;        
// //            for(SurveyResponse r: responses){
////                System.out.println("Response "+c+": "+r.real);
////                System.out.println(r.toString(survey1, ","));
// //                System.out.println();
////                c++;
////            }
//                        
//            QCMetric qc = new QCMetric();
//            ArrayList<SurveyResponse> outliers = qc.entropyBootstrap(survey1, responses);
//            double numRight, numWrong;
//            numRight=numWrong=0;
//            for(SurveyResponse o: outliers){
//                if(o.real)
//                    numWrong++;
//                else
//                    numRight++;
//            }
//            System.out.println("Percent random: "+percent);
//            System.out.println("% Random responses identified: "+numRight/(100*percent));
//            System.out.println();
//        }
//        
        ArrayList<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        Random rand = new Random();
        //generate group of respondents who always pick option 1
        int numResponses = 40;
        int numRandomResponses = 4;
        for(int x=0; x<numResponses; x++){
            SurveyResponse sr = new SurveyResponse(""+rand.nextInt(1000));
            responses.add(sr.consistentResponse(survey1));
        }
        
        //generate group of random respondents
        for(int x=0; x<numRandomResponses; x++){
            SurveyResponse sr = new SurveyResponse(""+rand.nextInt(1000));
            responses.add(sr.randomResponse(survey1));
        }
        
        //shuffle real and random responses
        Collections.shuffle(responses);
        int c=0;        
//        for(SurveyResponse r: responses){
//            System.out.println("Response "+c+": "+r.real);
//            System.out.println(r.toString(survey1, ","));
//            System.out.println();
//            c++;
//        }
        try{
            File f = new File(".\\output.csv");
            f.createNewFile();
            ResultsCSV results = new ResultsCSV(survey1, responses, f);
        }catch(IOException ex){
            System.out.println("Trouble making file");
            ex.printStackTrace();
        }
//        QCMetric qc = new QCMetric();
//        ArrayList<SurveyResponse> outliers = qc.entropyBootstrap(survey1, responses);
//        System.out.println("~~~~~~OUTLIERS~~~~~~");
//        System.out.println("# outliers: "+outliers.size());
//        for(SurveyResponse o: outliers){
//            System.out.println(o.toString(survey1, ","));
//            System.out.println(o.real);
//            System.out.println();
//        }
    }
}
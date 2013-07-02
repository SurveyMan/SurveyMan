package testing;
import csv.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import survey.*;

public class TestSuite{
    public static void main(String[] args){
        //String filename = args[1];
        String filename = "C:\\Python27\\dev\\SurveyMan\\data\\linguistics\\test1.csv";
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
        
        ArrayList<SurveyResponse> responses = new ArrayList<>();
        //generate group of respondents who always pick option 1
        int numResponses = 25;
        int numRandomResponses = 5;
        for(int x=0; x<numResponses+1; x++){
            SurveyResponse sr = new SurveyResponse();
            responses.add(sr.consistentResponse(survey1));
        }
        //generate group of random respondents
        for(int x=0; x<numRandomResponses+1; x++){
            SurveyResponse sr = new SurveyResponse();
            responses.add(sr.randomResponse(survey1));
        }
        
    }
}
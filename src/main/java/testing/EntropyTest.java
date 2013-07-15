/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package testing;

import survey.*;
import csv.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import qc.QCMetric;
import survey.SurveyResponse.QuestionResponse;

public class EntropyTest {
    public static void main(String[] args){
        //create survey 
        String filename = "C:\\Users\\Molly\\dev\\SurveyMan\\data\\entropytest.csv";
        Survey s=null;
        try {
            s = csv.CSVParser.parse(filename, ",");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EntropyTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EntropyTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        //read in responses and create SurveyResponse 
        ArrayList<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("C:\\Users\\Molly\\dev\\SurveyMan\\surveys\\survey-big.csv"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EntropyTest.class.getName()).log(Level.SEVERE, null, ex);
        }
//        for(Question q: s.questions){
//            System.out.println(q);
//            System.out.println(q.options);
//        }
        System.out.println();
        int count=0;
        Question curQ;
        while(scanner.hasNextLine()){
            count++;
            String responseLine = scanner.nextLine();
            //System.out.println(responseLine);
            SurveyResponse curResponse = new SurveyResponse(""+count);
            String[] answers = responseLine.split(",");
            for(int a=0; a<answers.length; a++){
                curQ=s.questions.get(a);
                ArrayList<Component> qanswer = new ArrayList<Component>();
                for(Component c: curQ.options.values()){
                    if(((StringComponent)c).data.equals(answers[a])){
                        qanswer.add(c);
                    }    
                }
                //System.out.println(answers);
                curResponse.responses.add(curResponse.new QuestionResponse(curQ, qanswer, a));
            }
            //System.out.println(curResponse);
            responses.add(curResponse);
        }
        
//        for(SurveyResponse sr: responses){
//            System.out.println(sr.toString(s, ","));
//        }
        QCMetric qc = new QCMetric();
        ArrayList<SurveyResponse> outliers = qc.entropyBootstrap(s, responses);
        System.out.println("~~~~~~OUTLIERS~~~~~~");
        System.out.println("# outliers: "+outliers.size());
        for(SurveyResponse o: outliers){
            System.out.println(o.toString(s, ","));
            System.out.println();
        }
        
    }
}

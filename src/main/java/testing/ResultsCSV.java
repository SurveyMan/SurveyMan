/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package testing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import survey.*;
import survey.SurveyResponse.QuestionResponse;

/**
 *
 * @author Molly
 */
public class ResultsCSV {
    ArrayList<SurveyResponse> responses;
    Survey survey;
    FileWriter writer;
        
    public ResultsCSV(Survey s, ArrayList<SurveyResponse> responses, File filename){
        this.responses=responses;
        survey=s;
        try {
            writer = new FileWriter(filename);
            toCSV();
        } catch (IOException ex) {
            System.out.println("File error stuff in ResultsCSV");
            ex.printStackTrace();
        }
        
    }
    
    private void toCSV(){
        try{
            for(Question q: survey.questions){
                writer.write(q.data.get(0).toString()+",");
            }
            writer.append("\n");
            for(SurveyResponse r: responses){
                for(QuestionResponse qr: r.responses){
                    writer.write(qr.opts.get(0).toString()+",");
                }
                writer.flush();
                writer.write("\n");
            }
            writer.close();
            
        }catch(IOException ex){
            System.out.println("Not writing to file properly");
            ex.printStackTrace();
        }
        
    }
        
}

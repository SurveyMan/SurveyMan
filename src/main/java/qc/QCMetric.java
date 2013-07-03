package qc;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;
import survey.*;

/**
 * QCMetric is the measure of similar/outliers, etc.
 * It operates over the SurveyResponse class.
 * SurveyResponse is meant to be instantiated.
 * 
 */
public class QCMetric {
    public static void main(String[] args){
        // write test code here
    }
    
    /*public ArrayList<ArrayList<Double>> qHistograms(Survey s, ArrayList<SurveyResponse> responses){
        ArrayList<ArrayList<Double>> hists = new ArrayList<>(s.questions.size());
        for(SurveyResponse r: responses){
             ArrayList<> sr = 
        }
    }
    
    public double surveyEntropy(ArrayList<SurveyResponse> responses){
        
    }
    
    public ArrayList<SurveyResponse> entropyBootstrap(ArrayList<SurveyResponse> responses){
        double fraction = ((double)responses.size())/((double)responses.size()-1);
        double multiplier = Math.pow(fraction, responses.size());
        double numBootstraps = 1000*multiplier;
        int n = responses.size();
        
        Random r = new Random();
        double[] bootstrapStats = new double[(int)numBootstraps];
        ArrayList<ArrayList<Double>> responseEntropies = new ArrayList<>();
        boolean[] included;
        double entropy=0;
        for(int x=0; x<numBootstraps; x++){
            included = new boolean[n];
            ArrayList<SurveyResponse> temp = new ArrayList<SurveyResponse>();
            for(int y=0; y<n; y++){
                SurveyResponse sr = responses.get(r.nextInt(n));
                temp.add(sr);
                included[y]=true;
            }
            entropy=surveyEntropy(temp);
            bootstrapStats[x]=entropy;
            for(int z=0; z<n; z++){
                if(!included[z])
                    responseEntropies.get(z).add(entropy);
            }
            
        }
        double bootstrapMean = //continue from here later
        
    }*/
    
}

package qc;

import java.util.ArrayList;
import java.util.Map;
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
    
    public ArrayList<Map<String, Double>> qHistograms(Survey s, ArrayList<SurveyResponse> responses){
        ArrayList<Map<String, Double>> hists = new ArrayList<>(s.questions.size());
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
        final int THRESHOLD = 5;
        
        Random r = new Random();
        ArrayList<Double> bootstrapStats = new ArrayList<Double>((int)numBootstraps);
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
            bootstrapStats.set(x, entropy);
            for(int z=0; z<n; z++){
                if(!included[z])
                    responseEntropies.get(z).add(entropy);
            }
            
        }
        double bootstrapMean = Stat.mean(bootstrapStats);
        double bootstrapSD = Stat.stddev(bootstrapStats);
        double responseMean = 0;
        double responseSD = 0;
        double t;        
        ArrayList<SurveyResponse> outliers = new ArrayList<SurveyResponse>();
        for(int x =0; x<n; x++){
            responseMean = Stat.mean(responseEntropies.get(x));
            responseSD = Stat.stddev(responseEntropies.get(x));
            //Welch's T test
            t=(bootstrapMean-responseMean)/Math.sqrt((Math.pow(bootstrapSD, 2))/n + (Math.pow(responseSD, 2))/responseEntropies.get(x).size());
            if(t>THRESHOLD)
                outliers.add(responses.get(x));
        }
        
        return responses;
    }
    
}

package qc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import survey.*;
import survey.SurveyResponse.QuestionResponse;

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
        int numq = s.questions.size();
        ArrayList<Map<String, Double>> hists = new ArrayList<Map<String, Double>>(numq);
        for(int x=0; x<numq; x++){
            hists.add(new HashMap<String, Double>());
        }
        
        for(SurveyResponse r: responses){
             for(int q=0; q<numq; q++){
                 QuestionResponse curq = r.responses.get(q);
                 for(Component c : curq.opts){
                    if(hists.get(q).containsKey(c.cid)){
                        hists.get(q).put(c.cid, hists.get(q).get(c.cid)+1);
                    }else{
                        hists.get(q).put(c.cid, 1.0);
                    }
                 }
             }
        }
        //System.out.println(hists);
        return hists;
    }
    
    public double surveyEntropy(Survey s, ArrayList<SurveyResponse> responses){
        double entropy = 0;
        ArrayList<Map<String, Double>> hists = qHistograms(s, responses);
        double n = responses.size();
        //normalize histograms and compute entropy
        for(int x=0; x<s.questions.size(); x++){
            //System.out.println("[");
            for(Double count: hists.get(x).values()){
                //System.out.print(count/n+" , ");
                entropy+=(count/n)*Math.log(count/n);
            }
            //System.out.println("]");
        }
        return -entropy;
    }
    
    public ArrayList<SurveyResponse> entropyBootstrap(Survey s, ArrayList<SurveyResponse> responses){
        double fraction = ((double)responses.size())/((double)responses.size()-1);
        double multiplier = Math.pow(fraction, responses.size());
        double numBootstraps = 10000*multiplier;
        int n = responses.size();
        final int THRESHOLD = 5;
        System.out.println(numBootstraps);
        System.out.println();
        Random r = new Random();
        ArrayList<Double> bootstrapStats = new ArrayList<Double>((int)numBootstraps);
        ArrayList<ArrayList<Double>> responseEntropies = new ArrayList<ArrayList<Double>>();
        for(int x=0; x<n; x++){
            responseEntropies.add(new ArrayList<Double>());
        }
        //System.out.println(responseEntropies);
        boolean[] included;
        double entropy=0;
        for(int x=0; x<numBootstraps; x++){
            included = new boolean[n];
            ArrayList<SurveyResponse> temp = new ArrayList<SurveyResponse>();
            for(int y=0; y<n; y++){
                int randIndex = r.nextInt(n);
                SurveyResponse sr = responses.get(randIndex);
                temp.add(sr);
                included[randIndex]=true;
            }
            //System.out.println(temp);
            entropy=surveyEntropy(s, temp);
            bootstrapStats.add(entropy);
            for(int z=0; z<n; z++){
                //System.out.println("checking included");
                if(!included[z])
                    //System.out.println("Response "+z+" not included in bootstrap #"+x);
                    responseEntropies.get(z).add(entropy);
            }
            
        }
//        for(ArrayList<Double> e: responseEntropies){
//            System.out.println(e.size());
//        }
        
        double bootstrapMean = Stat.mean(bootstrapStats);
        System.out.println("Bootstrap mean: "+bootstrapMean);
        double bootstrapSD = Stat.stddev(bootstrapStats);
        System.out.println("Bootstrap standard deviation: "+bootstrapSD);
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

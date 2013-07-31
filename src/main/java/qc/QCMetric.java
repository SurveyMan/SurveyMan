package qc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
    
    public double thresholdBootstrap(Survey s, ArrayList<SurveyResponse> responses){
        //generate group of random respondents
        int origlen = responses.size();
        Random rand = new Random();
        for(int x=0; x<4; x++){
            SurveyResponse sr = new SurveyResponse(""+rand.nextInt(1000));
            responses.add(sr.randomResponse(s));
        }
        double fraction = ((double)responses.size())/((double)responses.size()-1);
        double multiplier = Math.pow(fraction, responses.size());
        double numBootstraps = 100000*multiplier;
        int n = responses.size();
//        System.out.println(numBootstraps);
//        System.out.println();
        Random r = new Random();
        ArrayList<Double> bootstrapStats = new ArrayList<Double>((int)numBootstraps);
        ArrayList<ArrayList<Double>> responseEntropies = new ArrayList<ArrayList<Double>>(n);
        for(int x=0; x<n; x++){
            responseEntropies.add(new ArrayList<Double>());
        }
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
            for(int z=origlen; z<n; z++){
                //System.out.println("checking included");
                if(!included[z])
                    //System.out.println("Response "+z+" not included in bootstrap #"+x);
                    responseEntropies.get(z).add(entropy);
            }
            
        }
        double bootstrapMean = Stat.mean(bootstrapStats);
        //System.out.println("Bootstrap mean: "+bootstrapMean);
        double bootstrapSD = Stat.stddev(bootstrapStats);
        //System.out.println("Bootstrap standard deviation: "+bootstrapSD);
        double responseMean = 0;
        double responseSD = 0;
        ArrayList<Double> tvalues = new ArrayList<Double>();
        
        for(int x =origlen; x<n; x++){
            responseMean = Stat.mean(responseEntropies.get(x));
            responseSD = Stat.stddev(responseEntropies.get(x));
            //Welch's T test
            tvalues.add((bootstrapMean-responseMean)/Math.sqrt((Math.pow(bootstrapSD, 2))/numBootstraps + (Math.pow(responseSD, 2))/responseEntropies.get(x).size()));
        }
        double tavg = Stat.mean(tvalues);
        double std = Stat.stddev(tvalues);
        return tavg;
    }
    
    public ArrayList<SurveyResponse> entropyBootstrap(Survey s, ArrayList<SurveyResponse> responses){
        double fraction = ((double)responses.size())/((double)responses.size()-1);
        double multiplier = Math.pow(fraction, responses.size());
        double numBootstraps = 100000*multiplier;
        int n = responses.size();
//        System.out.println(numBootstraps);
//        System.out.println();
        Random r = new Random();
        ArrayList<Double> bootstrapStats = new ArrayList<Double>((int)numBootstraps);
        ArrayList<ArrayList<Double>> responseEntropies = new ArrayList<ArrayList<Double>>(n);
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
        
        //add random responses to the list to determine the average effect of removing them
        
        
        
        
        double bootstrapMean = Stat.mean(bootstrapStats);
        //System.out.println("Bootstrap mean: "+bootstrapMean);
        double bootstrapSD = Stat.stddev(bootstrapStats);
        //System.out.println("Bootstrap standard deviation: "+bootstrapSD);
        double responseMean = 0;
        double responseSD = 0;
        double t = 0;
        double threshold = thresholdBootstrap(s, responses);
        
        System.out.println(threshold);
        
        ArrayList<SurveyResponse> outliers = new ArrayList<SurveyResponse>();
        for(int x =0; x<n; x++){
            responseMean = Stat.mean(responseEntropies.get(x));
            System.out.println("Response "+x+" mean: "+responseMean);
            responseSD = Stat.stddev(responseEntropies.get(x));
            System.out.println("Response "+x+" standard deviation: "+responseSD);
            //Welch's T test
            t=(bootstrapMean-responseMean)/Math.sqrt((Math.pow(bootstrapSD, 2))/numBootstraps + (Math.pow(responseSD, 2))/responseEntropies.get(x).size());
            System.out.println(t);
            if(t>threshold){
                //System.out.println("adding response "+x+" to outliers");
                outliers.add(responses.get(x));
            }
        }
        
        return outliers;
    }
    
}

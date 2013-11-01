package qc;

import java.util.*;

import scala.Tuple2;
import survey.*;

/**
 * QCMetrics is the measure of similar/outliers, etc.
 * It operates over the SurveyResponse class.
 * SurveyResponse is meant to be instantiated.
 * 
 */
public class QCMetrics {

    public static Map<String, Map<String, Integer>> qHistograms(Survey s, ArrayList<SurveyResponse> responses){
        Map<String, Map<String, Integer>> frequencies = new HashMap<String, Map<String, Integer>>();
        for (Question q : s.questions)
            frequencies.put(q.quid, new HashMap<String, Integer>());
        for(SurveyResponse r: responses){
            for(SurveyResponse.QuestionResponse qr : r.responses) {
                if (!frequencies.containsKey(qr.q))
                    continue;
                // get the question entry
                Map<String, Integer> optMap = frequencies.get(qr.q.quid);
                String key = "";
                for (Tuple2<Component, Integer> c : qr.opts)
                    key += c._1().getCid();
                if (optMap.containsKey(key))
                    optMap.put(key, optMap.get(key)+1);
                else optMap.put(key, 1);
            }
        }
        return frequencies;
    }

    public static double entropy(double[] probs){
        double bits = 0.0;
        for (int i = 0; i<probs.length; i++)
            if (probs[i]!=0)
                bits += probs[i] * Math.log(probs[i]);
        return -bits;
    }

    private static int getOptionSpaceSize(Question q){
        if (q.freetext)
            // should be something more meaningful here in the future, but for now, punt
            return Integer.MAX_VALUE;
        else if (q.exclusive)
            return q.options.size();
        else return (int) Math.pow(2, q.options.size()) - 1;
    }

    /**
     * Computes the empirical entropy for a survey, given some pilot data.
     * @param s
     * @param responses
     * @return
     */
    public static double surveyEntropy(Survey s, ArrayList<SurveyResponse> responses){
        Map<String, Map<String, Integer>> hist = qHistograms(s, responses);
        double bits = 0.0;
        for (Question q : s.questions){
            int uniqueOptionsChosen = hist.get(q.quid).size();
            Integer[] dataPoints = hist.get(q.quid).values().toArray(new Integer[uniqueOptionsChosen]);
            double[] probs = new double[getOptionSpaceSize(q)];
            double totalDataPoints = 0.0;
            for (Integer i : dataPoints)
                totalDataPoints += i;
            for (int i = 0 ; i < probs.length ; i++)
                if (i < dataPoints.length)
                    probs[i] = (double) dataPoints[i] / totalDataPoints;
                else probs[i] = 0.0;
            bits += entropy(probs);
        }
        return bits;
    }

    public static double getMaxPossibleEntropy(Survey s){
        double bits = 0.0;
        for (Question q : s.questions)
            bits += Math.log(q.options.size());
        return -bits;
    }

    public static ArrayList<SurveyResponse> entropyBootstrap(Survey s, ArrayList<SurveyResponse> responses){

        double fraction = ((double)responses.size())/((double)responses.size()-1);
        double multiplier = Math.pow(fraction, responses.size());
        double numBootstraps = 1000*multiplier;
        int n = responses.size();

        Random r = new Random();
        double[] bootstrapStats = new double[(int)numBootstraps];
        ArrayList<ArrayList<Double>> responseEntropies = new ArrayList<ArrayList<Double>>();
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
            entropy=surveyEntropy(s, temp);
            bootstrapStats[x]=entropy;
            for(int z=0; z<n; z++){
                if(!included[z])
                    responseEntropies.get(z).add(entropy);
            }

        }
        double bootstrapMean = Stat.mean(bootstrapStats);
        double bootstrapSD = Stat.stddev(bootstrapStats);

    }
    
}

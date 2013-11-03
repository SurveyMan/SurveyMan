package qc;

import java.util.*;

import scala.Tuple2;
import survey.*;
import survey.SurveyResponse.QuestionResponse;

/**
 * QCMetrics is the measure of similar/outliers, etc.
 * It operates over the SurveyResponse class.
 * SurveyResponse is meant to be instantiated.
 * 
 */
public class QCMetrics {
  
    public enum QCMetric { ENTROPY, LIKELIHOOD; }
  
    public static class FreqProb {
      
        public Map<String, Map<String, Integer>> qHistograms;
        public Map<String, Map<String, Double>> empiricalProbabilities;
      
        public FreqProb(Survey s, List<SurveyResponse> responses){
          Map<String, Map<String, Integer>> frequencies = new HashMap<String, Map<String, Integer>>();
          Map<String, Map<String, Double>> probabilities = new HashMap<String, Map<String, Double>>();

          for (Question q : s.questions) {
              frequencies.put(q.quid, new HashMap<String, Integer>());
              probabilities.put(q.quid, new HashMap<String, Double>());
          }
          for(SurveyResponse r: responses){
              for(SurveyResponse.QuestionResponse qr : r.responses) {
                  if (!frequencies.containsKey(qr.q.quid))
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
          for(Question q : s.questions){
              double size = 0;
              Map<String, Double> thisQuestionsOptionProb = probabilities.get(q.quid);
              for (Integer ct : frequencies.get(q.quid).values())
                size += (double) ct;
              for (String optId : thisQuestionsOptionProb.keySet())
                thisQuestionsOptionProb.put(optId, (double) frequencies.get(q.quid).get(optId) / size);
          }
          this.qHistograms = frequencies;
          this.empiricalProbabilities = probabilities;
        }

        public int getFrequency(String quid, String optId) {
          return qHistograms.get(quid).get(optId);
        }
        
        public double getProbabilities(String quid, String optId) {
          return empiricalProbabilities.get(quid).get(optId);
        }
   }

      public Map<RandomRespondent.AdversaryType, Integer> adversaryComposition = new EnumMap<RandomRespondent.AdversaryType, Integer>(RandomRespondent.AdversaryType.class);

      public QCMetrics(Map<RandomRespondent.AdversaryType, Integer> adversaryComposition) {
          this.adversaryComposition = adversaryComposition;
      }
    
      private static int getOptionSpaceSize(Question q){
          if (q.freetext)
              // should be something more meaningful here in the future, but for now, punt
              return Integer.MAX_VALUE;
          else if (q.exclusive)
              return q.options.size();
          else return (int) Math.pow(2, q.options.size()) - 1;
      }
      
      private static String getOptionId(QuestionResponse qr) {
          String id = "";
          for (Tuple2<Component, Integer> data : qr.opts)
            id += data._1.getCid();
          return id;
      }

      public static double entropy(Double[] probs){
          double bits = 0.0;
          for (int i = 0; i<probs.length; i++)
              if (probs[i]!=0)
                  bits += probs[i] * Math.log(probs[i]);
          return -bits;
      }
    
      public static double getLogLikelihood(SurveyResponse sr, FreqProb fp) {
          double likelihood = 0.0;
          for (QuestionResponse qr : sr.responses) {
              String quid = qr.q.quid;
              String optId = getOptionId(qr);
              likelihood += fp.getProbabilities(quid, optId);
          }
          return -Math.log(likelihood);
      }

      /**
       * Computes the empirical entropy for a survey, given some pilot data.
       */
      public static double surveyEntropy(Survey s, ArrayList<SurveyResponse> responses){
          FreqProb f = new FreqProb(s, responses);
          double bits = 0.0;
          for (Question q : s.questions) {
            Map<String, Double> optFreqs = f.empiricalProbabilities.get(q.quid);
            bits += entropy(optFreqs.values().toArray(new Double[optFreqs.size()]));
          }
          return bits;
      }

      public static double getMaxPossibleEntropy(Survey s){
          double bits = 0.0;
          for (Question q : s.questions)
              bits += Math.log(q.options.size());
          return -bits;
      }

    //Molly's code
    public static double thresholdBootstrap(Survey s, ArrayList<SurveyResponse> responses, QCMetrics metrics) throws SurveyException{
        //generate group of random respondents
        int origlen = responses.size();
        for(int x=0; x<4; x++){
            RandomRespondent rr = new RandomRespondent(s, RandomRespondent.selectAdversaryProfile(metrics));
            responses.add(rr.response);
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
        double entropy;
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
        double responseMean, responseSD;
        ArrayList<Double> tvalues = new ArrayList<Double>();

        for(int x =origlen; x<n; x++){
            responseMean = Stat.mean(responseEntropies.get(x));
            responseSD = Stat.stddev(responseEntropies.get(x));
            //Welch's T test
            tvalues.add((bootstrapMean-responseMean)/Math.sqrt((Math.pow(bootstrapSD, 2))/numBootstraps + (Math.pow(responseSD, 2))/responseEntropies.get(x).size()));
        }
        double tavg = Stat.mean(tvalues);
        //double std = Stat.stddev(tvalues);
        return tavg;
    }

    public static ArrayList<SurveyResponse> entropyBootstrap(Survey s, ArrayList<SurveyResponse> responses, QCMetrics metric) throws SurveyException {

        double fraction = ((double)responses.size())/((double)responses.size()-1);
        double multiplier = Math.pow(fraction, responses.size());
        double numBootstraps = 1000*multiplier;
        int n = responses.size();

        Random r = new Random();
        Double[] bootstrapStats = new Double[(int)numBootstraps];
        ArrayList<ArrayList<Double>> responseEntropies = new ArrayList<ArrayList<Double>>();
        boolean[] included;
        double entropy;

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
        double bootstrapMean = Stat.mean((ArrayList) Arrays.asList(bootstrapStats));
        double bootstrapSD = Stat.stddev((ArrayList) Arrays.asList(bootstrapStats));

        //copied from other version, since Molly's code didn't merge properly.

        System.out.println("Bootstrap standard deviation: "+bootstrapSD);
        double responseMean, responseSD, t;
        double threshold = thresholdBootstrap(s, responses, metric);

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

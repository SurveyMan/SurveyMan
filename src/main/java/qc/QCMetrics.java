package qc;

import java.util.*;
import survey.*;
import survey.SurveyResponse.QuestionResponse;

/**
 * QCMetrics is the measure of similar/outliers, etc.
 * It operates over the SurveyResponse class.
 * SurveyResponse is meant to be instantiated.
 * 
 */
public class QCMetrics {
  
    public enum QCMetric { ENTROPY, LIKELIHOOD, LEAST_POPULAR; }
  
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
                    String key = getOptionId(qr);
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
                for (String optId : frequencies.get(q.quid).keySet())
                  thisQuestionsOptionProb.put(optId, (double) frequencies.get(q.quid).get(optId) / size);
            }

            this.qHistograms = frequencies;
            this.empiricalProbabilities = probabilities;
        }

        public int getFrequency(String quid, String optId) {
            return qHistograms.get(quid).get(optId);
        }
        
        public double getProbabilities(String quid, String optId) {
            Map<String, Double> m = empiricalProbabilities.get(quid);
            if (m.containsKey(optId))
                return m.get(optId);
            else return Double.MIN_VALUE;
        }

        public String toString() {
            StringBuilder s = new StringBuilder();
            for (String quid : this.empiricalProbabilities.keySet()) {
                s.append(quid+":");
                for (Map.Entry<String, Double> entry : this.empiricalProbabilities.get(quid).entrySet()) {
                    s.append("\t" + entry.getKey() + " :" + entry.getValue() + "\n");
                }
            }
            return s.toString();
        }
   }

    public Map<RandomRespondent.AdversaryType, Integer> adversaryComposition = new EnumMap<RandomRespondent.AdversaryType, Integer>(RandomRespondent.AdversaryType.class);
    public static double tolerance = 0.1;

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
        for (SurveyResponse.OptTuple data : qr.opts)
            id += data.c.getCid();
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
              if (SurveyResponse.customQuestion(qr.q.quid))
                  continue;
              String quid = qr.q.quid;
              String optId = getOptionId(qr);
              likelihood += Math.log(fp.getProbabilities(quid, optId));
          }
          return -likelihood;
      }


    public static double[][] makeBootstrapSample(List<Double> rawSample, int bootstrapReps, Random rng){

//        System.out.print("rawSample: \t");
//        for (Double d : rawSample)
//            System.out.print(Double.toString(d)+"\t");
//        System.out.println();

        double[][] bootstrapSample = new double[bootstrapReps][];

        for (int i = 0 ; i < bootstrapReps ; i++)
            bootstrapSample[i] = new double[rawSample.size()];

        for (int i = 0 ; i < bootstrapReps ; i++)
            for (int j = 0 ; j <rawSample.size() ; j++)
                bootstrapSample[i][j] = rawSample.get(rng.nextInt(rawSample.size()));

        return bootstrapSample;
    }

    public static double[] getBootstrapMeans(double[][] bootstrapSample) {
        double[] means = new double[bootstrapSample.length];
        for (int i = 0 ; i < bootstrapSample.length ; i++) {
            double sum = 0.0;
            for (int j = 0 ; j < bootstrapSample[i].length ; j++)
                sum += bootstrapSample[i][j];
            means[i] = sum / (double) bootstrapSample[i].length;
        }
        return means;
    }

    public static double[] getBootstrapSEs(double[][] bootstrapSample, double[] bootstrapMeans) {
        double[] ses = new double[bootstrapSample.length];
        for (int i = 0 ; i < bootstrapSample.length ; i++) {
            double sumOfSquaredDiffs = 0.0;
            double mean = bootstrapMeans[i];
            for (int j = 0 ; j < bootstrapSample[i].length ; j++)
                sumOfSquaredDiffs += Math.pow(bootstrapSample[i][j] - mean, 2.0);
            ses[i] = Math.pow(sumOfSquaredDiffs / Math.pow((double) bootstrapSample[i].length, 2.0), 0.5);
        }
        return ses;
    }

    public static double getBootstrapAvgMetric(double[] bootstrapMetrics) {
        double se = 0.0;
        for (int i = 0 ; i < bootstrapMetrics.length ; i++)
            se += bootstrapMetrics[i];
        return se / ((double) bootstrapMetrics.length - 1);
    }

    public static double[] getBootstrapUpperQuants(double[][] bootstrapSample, double alpha){
        double[] upper = new double[bootstrapSample.length];
        for (int i = 0 ; i < bootstrapSample.length ; i++) {
            int quant = (int) Math.floor(bootstrapSample[i].length * (1.0 - alpha));
            Arrays.sort(bootstrapSample[i]);
            upper[i] = bootstrapSample[i][quant];
        }
        return upper;
    }

    public static double[] getBootstrapLowerQuants(double[][] bootstrapSample, double alpha){
        double[] lower = new double[bootstrapSample.length];
        for (int i = 0 ; i < bootstrapSample.length ; i++){
            int quant = (int) Math.ceil(bootstrapSample[i].length * alpha);
            Arrays.sort(bootstrapSample[i]);
            lower[i] = bootstrapSample[i][quant];
        }
        return lower;
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
        List<List<Question>> paths = s.getAllPaths();
        double bits = 0.0;
        for (Question q : s.questions) {
            int m = q.options.size();
            if (m > 1) {
                double p = 1.0 / m;
                bits += p * (Math.log(p) / Math.log(2));
            }
        }
        return -bits;
    }

    public static Map<String, List<String>> leastPopularOptions(Survey survey, FreqProb fp) throws SurveyException {
        Map<String, List<String>> leastPopularOptions = new HashMap<String, List<String>>();
        for (String quid : fp.qHistograms.keySet()) {
            // first check that a min exists - want to see if we are within some tolerance of the counts.
            Map<String, Integer> histogram = fp.qHistograms.get(quid);
            double average = 0.0;
            for (Integer i : histogram.values())
                average += i;
            average /= histogram.size();
            boolean withinTolerance = true;
            for (Integer i : histogram.values())
                if (Math.abs(i - average) > tolerance) {
                    withinTolerance = false;
                    break;
                }
            if (withinTolerance)
                continue;
            // since a min exists, we should now find it.
            int min = Integer.MAX_VALUE; String oid = "";
            for (Map.Entry<String, Integer> entry : fp.qHistograms.get(quid).entrySet())
                if (entry.getValue() < (1+tolerance)*min) {
                    min = entry.getValue();
                    oid = entry.getKey();
                }
            List<String> lst = new LinkedList<String>();
            lst.add(oid);
            for (Map.Entry<String, Integer> entry : fp.qHistograms.get(quid).entrySet()) {
                if (lst.contains(entry.getKey()))
                    continue;
                double x = (double) entry.getValue().intValue();
                double y = (1.0 + tolerance) * min;
                if (x < y)
                    lst.add(entry.getKey());
            }
            if (lst.size() < survey.getQuestionById(quid).options.size())
                leastPopularOptions.put(quid, lst);
        }
        return leastPopularOptions;
    }

    public static double[] probabilitiesOfLeastPopular(Survey s, Map<String, List<String>> leastPopularOptions) {
        double[] mus = new double[s.questions.size()];
        for (Question q : s.questions) {
            if (leastPopularOptions.containsKey(q.quid))
                mus[q.index] = (double) leastPopularOptions.get(q.quid).size() / (double) q.options.size();
        }
        return mus;
    }

    public static double expectationOfLeastPopular(double[] expectedLeastPopular) {
        double mu = 0.0;
        for (int i = 0 ; i < expectedLeastPopular.length ; i++)
            mu += expectedLeastPopular[i];
        return mu;
    }
    
    public static int numLeastPopularOptions(SurveyResponse sr, Map<String, List<String>> leastPopularOptions) {
        int ct = 0;
        for (QuestionResponse qr : sr.responses) {
            List<String> optids = leastPopularOptions.get(qr.q.quid);
            if (optids==null)
                continue;
            for (SurveyResponse.OptTuple tupe : qr.opts) {
                if (SurveyResponse.customQuestion(qr.q.quid))
                    continue;
                if (optids.contains(tupe.c.getCid()))
                    ct++;
            }
        }
        return ct;
    }
    
    public static double getProbOfLeastPopular(Survey s, FreqProb fp, QC qc) throws SurveyException {
        Map<String, List<String>> lpo = leastPopularOptions(s, fp);
        double mu = expectationOfLeastPopular(probabilitiesOfLeastPopular(s, lpo));
        //int x = numLeastPopularOptions(sr, lpo);
        double delta = (double) qc.deviation / mu;
        double p = Math.pow(Math.E, (-(Math.pow(delta, 2) * mu) / (2 + delta)));
        //System.out.println(String.format("sr : %s \t x : %d \t mu : %f \t delta : %f\t p : %f\t boundary : %d", sr.srid, x, mu, delta, p, (int) Math.ceil((1 - delta) * mu)));
        //assert(x < Math.ceil((1 - delta) * mu));
        return p;
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

        //System.out.println("Bootstrap standard deviation: "+bootstrapSD);
        double responseMean, responseSD, t;
        double threshold = thresholdBootstrap(s, responses, metric);

        //System.out.println(threshold);

        ArrayList<SurveyResponse> outliers = new ArrayList<SurveyResponse>();
        for(int x =0; x<n; x++){
            responseMean = Stat.mean(responseEntropies.get(x));
            //System.out.println("Response "+x+" mean: "+responseMean);
            responseSD = Stat.stddev(responseEntropies.get(x));
            //System.out.println("Response "+x+" standard deviation: "+responseSD);
            //Welch's T test
            t=(bootstrapMean-responseMean)/Math.sqrt((Math.pow(bootstrapSD, 2))/numBootstraps + (Math.pow(responseSD, 2))/responseEntropies.get(x).size());
            //System.out.println(t);
            if(t>threshold){
                //System.out.println("adding response "+x+" to outliers");
                outliers.add(responses.get(x));
            }
        }

        return outliers;
    }

}

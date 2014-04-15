package qc;

import survey.*;
import survey.SurveyResponse.QuestionResponse;
import system.Interpreter;

import java.util.*;

/**
 * QCMetrics is the measure of similar/outliers, etc.
 * It operates over the SurveyResponse class.
 * SurveyResponse is meant to be instantiated.
 * 
 */
public class QCMetrics {
  
    public enum QCMetric { ENTROPY, LIKELIHOOD, LEAST_POPULAR; }
    public enum PathMetric { MAX, MIN, AVG; }
  
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

    public static class Path {
        List<Block> path = new ArrayList<Block>();
        public Path(List<Block> path){
            for (Block b : path){
                this.path.add(0,b);
            }
        }
        public void append(List<Block> path){
            this.path.addAll(path);
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
        StringBuilder id = new StringBuilder();
        for (SurveyResponse.OptTuple data : qr.opts)
            id.append(data.c.getCid());
        return id.toString();
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
        //List<List<Question>> paths = s.getAllPaths();
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

    public static int minimumPathLength(Survey survey) {
        return pathLength(survey, PathMetric.MIN);
    }

    public static int maximumPathLength(Survey survey) {
        return pathLength(survey, PathMetric.MAX);
    }

    private static int pathLength(Survey survey, PathMetric metric){
        // get size of all top level randomizable blocks
        int size = 0;
        Map<Boolean, List<Block>> partitionedBlocks = Interpreter.partitionBlocks(survey);
        for (Block b : partitionedBlocks.get(true)) {
            size += b.dynamicQuestionCount();
        }
        // find the max path through the nonrandomizable blocks
        List<Block> blocks = Block.sort(partitionedBlocks.get(false));
        Block branchDest = null;
        for (Block b : blocks) {
            if (branchDest!=null && !b.equals(branchDest)) //skip this block
                continue;
            size += b.dynamicQuestionCount();
            if (branchDest!=null && b.equals(branchDest))
                branchDest = null;
            if (b.branchParadigm.equals(Block.BranchParadigm.ONE)) {
                List<Block> dests = Block.sort(new ArrayList(b.branchQ.branchMap.values()));
                switch (metric) {
                    case MAX:
                        branchDest = dests.get(0);
                        break;
                    case MIN:
                        branchDest = dests.get(dests.size() - 1);
                        break;
                    default:
                        break;
                }
            }
        }
        return size;
    }

    public static double averagePathLength(Survey survey) throws SurveyException {
        double lengthSum = 0.0;
        for (int i = 0 ; i < 5000 ; i++) {
            lengthSum += new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM).response.responses.size();
        }
        return lengthSum / 5000.0;
    }
}

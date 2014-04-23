package qc;

import survey.*;
import survey.SurveyResponse.QuestionResponse;
import system.Interpreter;
import system.Library;

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

    /**
     * Computes the empirical entropy for a survey, given some pilot data.
     */
    public static double surveyEntropy(Survey s, List<SurveyResponse> responses){
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

    public static double getBasePay(Survey survey) {
        double minPathLength = minimumPathLength(survey);
        double payPerSecond = Library.FEDMINWAGE / (60 * 60);
        return minPathLength * Library.timePerQuestionInSeconds * payPerSecond;
    }
}

package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.ISurveyResponse;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.Reader;
import java.util.*;

public class QCMetrics {

    /**
     * Takes in a list of Blocks; returns a list of lists of Blocks representing all possible paths through the survey.
     * See @etosch's blog post for more detail.
     * @param blockList A list of blocks we would like to traverse.
     * @return A list of lists of blocks, giving all possible traversals through the original input.
     */
    public static List<List<Block>> getDag(List<Block> blockList) {
        Collections.sort(blockList);
        if (blockList.isEmpty()) {
            // return a singleton list of the empty list
            List<List<Block>> newSingletonList = new ArrayList<List<Block>>();
            newSingletonList.add(new ArrayList<Block>());
            return newSingletonList;
        } else {
            Block thisBlock = blockList.get(0);
            if (thisBlock.branchQ != null
                    && thisBlock.branchQ.branchMap != null
                    && thisBlock.branchQ.branchMap.size() != 0) {
                Set<Block> dests = new HashSet<Block>(thisBlock.branchQ.branchMap.values());
                List<List<Block>> blists = new ArrayList<List<Block>>();
                for (Block b : dests) {
                    // for each destination, find the sublist of the blocklist starting with the destination
                    int index = blockList.indexOf(b);
                    if (index > -1) {
                        List<List<Block>> dags = getDag(blockList.subList(index, blockList.size()));
                        for (List<Block> dag : dags) {
                            dag.add(thisBlock);
                        }
                        blists.addAll(dags);
                    }
                }
                return blists;
            } else {
                List<List<Block>> subDag = getDag(blockList.subList(1, blockList.size()));
                for (List<Block> blist : subDag) {
                    blist.add(thisBlock);
                }
                return subDag;
            }
        }
    }

    private static List<Block> flatten(List<List<Block>> blists) {
        List<Block> retval = new ArrayList<Block>();
        for (List<Block> blist : blists) {
            for (Block b : blist) {
                if (!retval.contains(b))
                    retval.add(b);
            }
        }
        return retval;
    }

    /**
     * Returns paths through **blocks** in the survey. Top level randomized blocks are all listed last
     * @param s The survey whose paths we want to enumerate
     * @return A List of all paths through the survey. A path is represented by a List. There may be duplicate paths,
     * so if you need distinct paths, you will need to filter for uniqueness.
     */
    private static List<List<Block>> getPaths(Survey s) {
        Map<Boolean, List<Block>> partitionedBlocks = Interpreter.partitionBlocks(s);
        List<Block> topLevelRandomizableBlocks = partitionedBlocks.get(true);
        List<Block> nonrandomizableBlocks = partitionedBlocks.get(false);
        Collections.sort(nonrandomizableBlocks);
        List<List<Block>> dag = getDag(nonrandomizableBlocks);
        if (!flatten(dag).isEmpty()) {
            for (List<Block> blist : dag) {
                blist.addAll(topLevelRandomizableBlocks);
            }
        }
        return dag;
    }

    /**
     * Returns the set of enclosing blocks for this survey response.
     * @param r A single survey responses
     * @return The blocks the respondent has traversed in order to produce this response.
     */
    private static Set<Block> getPath(ISurveyResponse r) {
        Set<Block> retval = new HashSet<Block>();
        for (IQuestionResponse questionResponse : r.getResponses()) {
            retval.add(questionResponse.getQuestion().block);
        }
        return retval;
    }

    /**
     * Returns the counts for each path; see @etosch's blog post on the calculation.
     * @param paths The list of list of blocks through the survey; can be obtained with getPaths or getDag
     * @param responses The list of actual or simulated responses to the survey
     * @return A map from path to the frequency the path is observed.
     */
    private static Map<List<Block>, List<ISurveyResponse>> makeFrequenciesForPaths(List<List<Block>> paths,
                                                                     List<ISurveyResponse> responses) {
        Map<List<Block>, List<ISurveyResponse>> retval = new HashMap<List<Block>, List<ISurveyResponse>>();
        for (ISurveyResponse r : responses) {
            for (List<Block> path : paths) {
                Set<Block> pathTraversed = getPath(r);
                if (path.containsAll(pathTraversed)){
                    if (retval.containsKey(path))
                        retval.get(path).add(r);
                    else {
                        List<ISurveyResponse> srlist = new ArrayList<ISurveyResponse>();
                        srlist.add(r);
                        retval.put(path, srlist);
                    }
                }
            }
        }
        return retval;
    }

    private static List<Question> removeFreetext(List<Question> questionList) {
        List<Question> questions = new ArrayList<Question>();
        for (Question q : questionList) {
            if (!q.freetext)
                questions.add(q);
        }
        return questions;
    }


    /**
     * Returns equivalent answer options (a list of survey.Component)
     * @param q The question whose variants we want. If there are no variants, then a set of just this question is
     *          returned.
     * @param c The answer the respondent provided for this question.
     * @return
     */
    public static List<Component> getEquivalentAnswerVariants(Question q, Component c) {
        List<Component> retval = new ArrayList<Component>();
        List<Question> variants = q.getVariants();
        int offset = q.getSourceRow() - c.getSourceRow();
        for (Question variant : variants) {
            for (Component thisC : variant.options.values()) {
                int thisOffset = variant.getSourceRow() - thisC.getSourceRow();
                if (thisOffset == offset)
                    retval.add(thisC);
            }
        }
        return retval;
    }

    public static double surveyEntropy(Survey s, List<ISurveyResponse> responses){
        List<List<Block>> paths = getPaths(s);
        Map<List<Block>, List<ISurveyResponse>> pathMap = makeFrequenciesForPaths(paths, responses);
        int totalResponses = responses.size();
        double retval = 0.0;
        for (Question q : removeFreetext(s.questions)) {
            for (Component c : q.options.values()) {
                for (List<Block> path : paths) {
                    List<Component> variants = getEquivalentAnswerVariants(q, c);
                    List<ISurveyResponse> responsesThisPath = pathMap.get(path);
                    List<ISurveyResponse> ansThisPath = new ArrayList<ISurveyResponse> ();
                    for (ISurveyResponse r : responsesThisPath) {
                        if (r.surveyResponseContainsAnswer(variants)) {
                            ansThisPath.add(r);
                        }
                    }
                    double p = ansThisPath.size() / (double) totalResponses;
                    retval += (Math.log(p) / Math.log(2.0)) * p;
                }
            }
        }
        return -retval;
    }

    /**
     * Returns all questions in a block list (typically the topLevelBlocks of a Survey).
     * @param blockList
     * @return
     */
    public static List<Question> getQuestions(List<Block> blockList) {
        List<Question> questions = new ArrayList<Question>();
        if (!blockList.isEmpty()) {
            for (Block block : blockList) {
                questions.add(block.questions.get(new Random().nextInt(block.questions.size())));
                if (block.branchParadigm != Block.BranchParadigm.ALL) {
                    questions.addAll(getQuestions(block.subBlocks));
                }
            }
        }
        return questions;
    }

    /**
     * Returns the maximum possible entropy for a single Question.
     * @param question
     * @return
     */
    private static double maxEntropyOneQuestion(Question question) {
        double retval = 0.0;
        int numOptions = question.options.size();
        if (numOptions != 0) {
            retval += Math.log((double) numOptions) / Math.log(2.0);
        }
        return retval;
    }


    /**
     * Returns the total entropy for a list of Questions.
     * @param questionList
     * @return
     */
    private static double maxEntropyQlist(List<Question> questionList) {
        double retval = 0.0;
        for (Question q : questionList) {
            retval += maxEntropyOneQuestion(q);
        }
        return retval;
    }


    /**
     * Returns the path with the highest entropy.
     * @param blists
     * @return
     */
    private static List<Block> getMaxPathForEntropy(List<List<Block>> blists) {
        List<Block> retval = new ArrayList<Block>();
        double maxEnt = 0.0;
        for (List<Block> blist : blists) {
            double ent = maxEntropyQlist(getQuestions(blist));
            if (ent > maxEnt) {
                maxEnt = ent;
                retval = blist;
            }
        }
        return retval;
    }

    public static double getMaxPossibleEntropy(Survey s) {
        return maxEntropyQlist(getQuestions(getMaxPathForEntropy(getPaths(s))));
    }

    public static int minimumPathLength(Survey survey){
        List<List<Block>> paths = getPaths(survey);
        int max = Integer.MIN_VALUE;
        for (List<Block> path : paths) {
            int pathLength = getQuestions(path).size();
            if (pathLength > max)
                max = pathLength;
        }
        return max;
    }

    public static int maximumPathLength(Survey survey) {
        List<List<Block>> paths = getPaths(survey);
        int min = Integer.MAX_VALUE;
        for (List<Block> path : paths) {
            int pathLength = path.size();
            if (pathLength < min) {
                min = pathLength;
            }
        }
        return min;

    }

    public static double averagePathLength(Survey survey) throws SurveyException {
        int n = 5000;
        int stuff = 0;
        for (int i = 0 ; i < n ; i++) {
            RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
            stuff += rr.response.getResponses().size();
        }
        return (double) stuff / n;
    }

    public static Map<String, Map<String, Integer>> makeFrequencies(List<ISurveyResponse> responses) {
        return makeFrequencies(responses, null);
    }

    public static Map<String, Map<String, Integer>> makeFrequencies(List<ISurveyResponse> responses, Survey survey) {
        Map<String, Map<String, Integer>> retval = new HashMap<String, Map<String, Integer>>();
        Set<String> allComponentIdsSelected = new HashSet<String>();
        for (ISurveyResponse sr : responses) {
            for (IQuestionResponse qr : sr.getResponses()) {
                String quid = qr.getQuestion().quid;
                Map<String, Integer> tmp = new HashMap<String, Integer>();
                if (retval.containsKey(quid)) {
                    tmp = retval.get(quid);
                } else {
                    retval.put(quid, tmp);
                }
                List<String> cids = OptTuple.getCids(qr.getOpts());
                for (String cid : cids) {
                    allComponentIdsSelected.add(cid);
                    if (tmp.containsKey(cid))
                        tmp.put(cid, tmp.get(cid) + 1);
                    else tmp.put(cid, 1);
                }
            }
        }
        // +1 smoothing
        if (survey != null) {
            int numberNeedingSmoothing = 0;
            for (Question q : survey.questions) {
                for (Component c : q.options.values()) {
                    if (!retval.containsKey(q.quid)) {
                        retval.put(q.quid, new HashMap<String, Integer>());
                    }
                    retval.get(q.quid).put(c.getCid(), 1);
                    if (!allComponentIdsSelected.contains(c.getCid())) {
                        numberNeedingSmoothing++;
                    }
                }
            }
        }
        return retval;
    }

    public static Map<String, Map<String, Double>> makeProbabilities(Map<String, Map<String, Integer>> frequencies) {
        Map<String, Map<String, Double>> retval = new HashMap<String, Map<String, Double>>();
        for (Map.Entry<String, Map<String, Integer>> e : frequencies.entrySet()) {
            String quid = e.getKey();
            Map<String, Integer> map = e.getValue();
            double total = 0.0;
            for (Integer i : map.values()) {
                total += i;
            }
            retval.put(quid, new HashMap<String, Double>());
            for (String cid : map.keySet()) {
                retval.get(quid).put(cid, map.get(cid) / total);
            }
        }
        return retval;
    }

    public static double getLLForResponse(ISurveyResponse surveyResponse, Map<String, Map<String, Double>> probabilities) {
        double ll = 0.0;
        for (IQuestionResponse questionResponse : surveyResponse.getResponses()) {
            String qid = questionResponse.getQuestion().quid;
            for (String cid : OptTuple.getCids(questionResponse.getOpts())) {
                ll += Math.log(probabilities.get(qid).get(cid)) / Math.log(2.0);
            }
        }
        return ll;
    }

    public static double getEntropyForResponse(ISurveyResponse surveyResponse, Map<String, Map<String, Double>> probabilities) {
        double ent = 0.0;
        for (IQuestionResponse questionResponse : surveyResponse.getResponses()) {
            String qid = questionResponse.getQuestion().quid;
            for (String cid : OptTuple.getCids(questionResponse.getOpts())) {
                double p = probabilities.get(qid).get(cid);
                ent += p * (Math.log(p) / Math.log(p));
            }
        }
        return ent;
    }

    public static List<Double> calculateLogLikelihoods(List<ISurveyResponse> responses, Map<String, Map<String, Double>> probabilities) {
        List<Double> retval = new LinkedList<Double>();
        for (ISurveyResponse sr : responses) {
            retval.add(getLLForResponse(sr, probabilities));
        }
        return retval;
    }

    public static List<String> getQuestionIds(List<IQuestionResponse> questionResponses) {
        List<String> quids = new ArrayList<String>();
        for (IQuestionResponse qr : questionResponses) {
            quids.add(qr.getQuestion().quid);
        }
        return quids;
    }

    public static List<ISurveyResponse> truncateResponses(List<ISurveyResponse> surveyResponses, ISurveyResponse surveyResponse) {
        List<ISurveyResponse> retval = new ArrayList<ISurveyResponse>();
        for (final ISurveyResponse sr : surveyResponses) {
            final Set<String> answeredQuestions = new HashSet(getQuestionIds(surveyResponse.getResponses()));
            final Set<String> targetResponses = new HashSet(getQuestionIds(sr.getResponses()));
            if (targetResponses.contains(answeredQuestions)) {
                retval.add(new ISurveyResponse() {
                    @Override
                    public List<IQuestionResponse> getResponses() {
                        List<IQuestionResponse> retval = new ArrayList<IQuestionResponse>();
                        for (IQuestionResponse qr : sr.getResponses()) {
                            if (answeredQuestions.contains(qr.getQuestion().quid))
                                retval.add(qr);
                        }
                        return retval;
                    }

                    @Override
                    public void setResponses(List<IQuestionResponse> responses) {

                    }

                    @Override
                    public boolean isRecorded() {
                        return true;
                    }

                    @Override
                    public void setRecorded(boolean recorded) {

                    }

                    @Override
                    public String getSrid() {
                        return null;
                    }

                    @Override
                    public void setSrid(String srid) {

                    }

                    @Override
                    public String workerId() {
                        return null;
                    }

                    @Override
                    public Map<String, IQuestionResponse> resultsAsMap() {
                        return null;
                    }

                    @Override
                    public List<ISurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException {
                        return null;
                    }

                    @Override
                    public void setScore(double score) {

                    }

                    @Override
                    public double getScore() {
                        return 0;
                    }

                    @Override
                    public void setThreshold(double pval) {

                    }

                    @Override
                    public double getThreshold() {
                        return 0;
                    }

                    @Override
                    public boolean surveyResponseContainsAnswer(List<Component> variants) {
                        return false;
                    }
                });
            }
        }
        return retval;
    }

    public static List<List<ISurveyResponse>> generateBootstrapSample(List<ISurveyResponse> responseList, int iterations) {
        List<List<ISurveyResponse>> retval = new ArrayList<List<ISurveyResponse>>();
        for (int i = 0; i < iterations; i++) {
            List<ISurveyResponse> sample = new ArrayList<ISurveyResponse>();
            for (int j = 0 ; j < responseList.size() ; j++) {
                sample.add(responseList.get(Interpreter.random.nextInt(responseList.size())));
            }
            retval.add(sample);
        }
        return retval;
    }

    public static boolean logLikelihoodClassification(Survey survey, ISurveyResponse sr, List<ISurveyResponse> responses,
                                                      boolean smoothing, double alpha) {
        Map<String, Map<String, Double>> probabilities = makeProbabilities(makeFrequencies(responses, smoothing ? survey : null));
        List<Double> lls = calculateLogLikelihoods(truncateResponses(responses, sr), probabilities);
        if (new HashSet<Double>(lls).size() > 5) {
            double thisLL = getLLForResponse(sr, probabilities);
            List<List<ISurveyResponse>> bsSample = generateBootstrapSample(responses, 500);
            List<Double> means = new ArrayList<Double>();
            for (List<ISurveyResponse> sample : bsSample) {
                double total = 0.0;
                for (ISurveyResponse surveyResponse: sample) {
                    total += getLLForResponse(surveyResponse, probabilities);
                }
                means.add(total / sample.size());
            }
            Collections.sort(means);
            assert means.get(0) < means.get(means.size() - 1);
            double threshHold = means.get((int) Math.floor(alpha * means.size()));
            sr.setScore(thisLL);
            return thisLL < threshHold;
        } else return false;
    }

    public boolean entropyClassification(Survey survey, ISurveyResponse sr, List<ISurveyResponse> responses,
                                         boolean smoothing, double alpha) {
        // basically the same as logLikelihood, but scores are p * log p, rather than straight up p
        Map<String, Map<String, Double>> probabilities = makeProbabilities(makeFrequencies(responses, smoothing ? survey : null));
        List<Double> lls = calculateLogLikelihoods(truncateResponses(responses, sr), probabilities);
        if (new HashSet<Double>(lls).size() > 5) {
            double thisLL = getEntropyForResponse(sr, probabilities);
            List<List<ISurveyResponse>> bsSample = generateBootstrapSample(responses, 500);
            List<Double> means = new ArrayList<Double>();
            for (List<ISurveyResponse> sample : bsSample) {
                double total = 0.0;
                for (ISurveyResponse surveyResponse: sample) {
                    total += getEntropyForResponse(surveyResponse, probabilities);
                }
                means.add(total / sample.size());
            }
            Collections.sort(means);
            assert means.get(0) < means.get(means.size() - 1);
            double threshHold = means.get((int) Math.floor(alpha * means.size()));
            sr.setScore(thisLL);
            return thisLL < threshHold;
        } else return false;
    }

    public boolean lpoClassification(Survey survey, ISurveyResponse sr, List<ISurveyResponse> responses) {
        return true;
    }
    //public double calculateBonus(ISurveyResponse sr, Record record);
    //public double getBotThresholdForSurvey(Survey s);

}

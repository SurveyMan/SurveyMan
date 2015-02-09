package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.*;
import edu.umass.cs.surveyman.output.*;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import java.io.Reader;
import java.util.*;

public class QCMetrics {

    private static double log2(double p) {
        if (p == 0)
            return 0.0;
        return Math.log(p) / Math.log(2.0);
    }

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
            if (thisBlock.hasBranchQuestion()) {
                Set<Block> dests = thisBlock.getBranchDestinations();
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

    /**
     * Returns paths through **blocks** in the survey. Top level randomized blocks are all listed last
     * @param s The survey whose paths we want to enumerate
     * @return A List of all paths through the survey. A path is represented by a List. There may be duplicate paths,
     * so if you need distinct paths, you will need to filter for uniqueness.
     */
    protected static List<List<Block>> getPaths(Survey s) {
        List<List<Block>> retval = new ArrayList<List<Block>>();
        Map<Boolean, List<Block>> partitionedBlocks = Interpreter.partitionBlocks(s);
        List<Block> topLevelRandomizableBlocks = partitionedBlocks.get(true);
        List<Block> nonrandomizableBlocks = partitionedBlocks.get(false);
        Collections.sort(nonrandomizableBlocks);
        List<List<Block>> dag = getDag(nonrandomizableBlocks);
        SurveyMan.LOGGER.info("Computing paths for survey having DAG with "+dag.size()+" paths through fixed blocks.");
        for (List<Block> blist : dag) {
            if (blist.isEmpty())
                continue;
            blist.addAll(topLevelRandomizableBlocks);
            retval.add(blist);
        }
        SurveyMan.LOGGER.info(String.format("Computed %d paths through the survey.", retval.size()));
        return retval;
    }

    /**
     * Returns the set of enclosing blocks for this survey response.
     * @param r A single survey responses
     * @return The blocks the respondent has traversed in order to produce this response.
     */
    private static Set<Block> getPath(AbstractSurveyResponse r) {
        Set<Block> retval = new HashSet<Block>();
        for (IQuestionResponse questionResponse : r.getResponses()) {
            Question q = questionResponse.getQuestion();
            retval.add(q.block);
        }
        return retval;
    }

    /**
     * Returns the counts for each path; see @etosch's blog post on the calculation.
     * @param paths The list of list of blocks through the survey; can be obtained with getPaths or getDag
     * @param responses The list of actual or simulated responses to the survey
     * @return A map from path to the frequency the path is observed.
     */
    protected static Map<List<Block>, List<AbstractSurveyResponse>> makeFrequenciesForPaths(
            List<List<Block>> paths, List<AbstractSurveyResponse> responses) {
        Map<List<Block>, List<AbstractSurveyResponse>> retval = new HashMap<List<Block>, List<AbstractSurveyResponse>>();
        // initialize the map
        for (List<Block> path : paths)
            retval.put(path, new ArrayList<AbstractSurveyResponse>());
        for (AbstractSurveyResponse r : responses) {
            Set<Block> pathTraversed = getPath(r);
            boolean pathFound = false;
            for (List<Block> path : retval.keySet()) {
                if (path.containsAll(pathTraversed)){
                    retval.get(path).add(r);
                    pathFound = true;
                    break;
                }
            }
            assert pathFound : "Path survey respondent took does not match any known paths through the survey.";
        }
        return retval;
    }

    protected static List<Question> removeFreetext(List<Question> questionList) {
        List<Question> questions = new ArrayList<Question>();
        for (Question q : questionList) {
            // freetext will be null if we've designed the survey programmatically and have
            // not added any questions (i.e. if it's an instructional question).
            // TODO(etosch) : subclass Question to make an Instructional Question.
            if (q.freetext == null)
                q.freetext = false;
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
    protected static List<Component> getEquivalentAnswerVariants(Question q, Component c) {

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
        SurveyMan.LOGGER.debug("Variant set size: " + retval.size());
        return retval;
    }

    public static double surveyEntropy(Survey s, List<AbstractSurveyResponse> responses){
        List<List<Block>> paths = getPaths(s);
        Map<List<Block>, List<AbstractSurveyResponse>> pathMap = makeFrequenciesForPaths(paths, responses);
        int totalResponses = responses.size();
        assert totalResponses > 1 : "surveyEntropy is meaningless for fewer than 1 response.";
        double retval = 0.0;
        for (Question q : removeFreetext(s.questions)) {
            for (Component c : q.options.values()) {
                for (List<Block> path : paths) {
                    List<Component> variants = getEquivalentAnswerVariants(q, c);
                    List<AbstractSurveyResponse> responsesThisPath = pathMap.get(path);
                    double ansThisPath = 0.0;
                    for (AbstractSurveyResponse r : responsesThisPath) {
                        boolean responseInThisPath = r.surveyResponseContainsAnswer(variants);
                        if (responseInThisPath) {
                            SurveyMan.LOGGER.info("Found an answer on this path!");
                            ansThisPath += 1.0;
                        }
                    }
                    double p = ansThisPath / (double) totalResponses;
                    retval += log2(p) * p;
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
    public static List<Question> getQuestions(final List<Block> blockList) {
        List<Question> questions = new ArrayList<Question>();
        for (Block block : blockList) {
            if (block.branchParadigm != Block.BranchParadigm.ALL)
                questions.addAll(block.questions);
            else {
                questions.add(block.questions.get(new Random().nextInt(block.questions.size())));
            }
            questions.addAll(getQuestions(block.subBlocks));
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
            retval += log2((double) numOptions);
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
        int min = Integer.MAX_VALUE;
        for (List<Block> path : paths) {
            int pathLength = getQuestions(path).size();
            if (pathLength < min)
                min = pathLength;
        }
        return min;
    }

    public static int maximumPathLength(Survey survey) {
        List<List<Block>> paths = getPaths(survey);
        int max = Integer.MIN_VALUE;
        for (List<Block> path : paths) {
            int pathLength = getQuestions(path).size();
            if (pathLength > max) {
                max = pathLength;
            }
        }
        return max;

    }

    public static double averagePathLength(Survey survey) throws SurveyException {
        int n = 5000;
        int stuff = 0;
        for (int i = 0 ; i < n ; i++) {
            RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
            stuff += rr.getResponse().getResponses().size();
        }
        return (double) stuff / n;
    }

    /**
     * When used without the survey argument, this returns frequencies that do not calculate smoothing.
     * @param responses The list of actual or simulated responses to the survey
     * @return A map from question ids to maps of option ids to counts.
     */
    public static Map<String, Map<String, Integer>> makeFrequencies(List<AbstractSurveyResponse> responses) {
        return makeFrequencies(responses, null);
    }

    /**
     * Creates a frequency map for the actual responses to the survey. If the survey argument is not null, it will c
     * calculate LaPlace smoothing.
     * @param responses The list of actual or simulated responses to the survey.
     * @param survey The survey these respondents answered.
     * @return A map from question ids to a map of option ids to counts.
     */
    public static Map<String, Map<String, Integer>> makeFrequencies(List<AbstractSurveyResponse> responses, Survey survey) {
        Map<String, Map<String, Integer>> retval = new HashMap<String, Map<String, Integer>>();
        Set<String> allComponentIdsSelected = new HashSet<String>();
        for (AbstractSurveyResponse sr : responses) {
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
        // LaPlace (+1 smoothing)
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
            if (numberNeedingSmoothing > 0)
                SurveyMan.LOGGER.info("Number needing smoothing " + numberNeedingSmoothing);
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

    public static double getLLForResponse(AbstractSurveyResponse surveyResponse, Map<String, Map<String, Double>> probabilities) {
        double ll = 0.0;
        for (IQuestionResponse questionResponse : surveyResponse.getResponses()) {
            String qid = questionResponse.getQuestion().quid;
            for (String cid : OptTuple.getCids(questionResponse.getOpts())) {
                ll += log2(probabilities.get(qid).get(cid));
            }
        }
        return ll;
    }

    public static double getEntropyForResponse(AbstractSurveyResponse surveyResponse, Map<String, Map<String, Double>> probabilities) {
        double ent = 0.0;
        for (IQuestionResponse questionResponse : surveyResponse.getResponses()) {
            String qid = questionResponse.getQuestion().quid;
            for (String cid : OptTuple.getCids(questionResponse.getOpts())) {
                double p = probabilities.get(qid).get(cid);
                assert p > 0.0;
                ent += p * log2(p);
            }
        }
        return -ent;
    }

    public static List<Double> calculateLogLikelihoods(List<AbstractSurveyResponse> responses, Map<String, Map<String, Double>> probabilities) {
        List<Double> retval = new LinkedList<Double>();
        for (AbstractSurveyResponse sr : responses) {
            retval.add(getLLForResponse(sr, probabilities));
        }
        return retval;
    }

    private static AbstractSurveyResponse getResponseSubset(
            AbstractSurveyResponse base,
            AbstractSurveyResponse target
    ) throws SurveyException {
        // These will be used to generate the return value.
        final List<IQuestionResponse> responses = new ArrayList<IQuestionResponse>();

        // For each question in our base response, check whether the target has answered that question or one of its
        // variants.
        for (IQuestionResponse qr : base.getResponses()) {
            // Get the variants for this question.
            List<Question> variants = qr.getQuestion().getVariants();
            boolean variantFound = false;
            for (Question q : variants) {
                if (target.hasResponseForQuestion(q)) {
                    responses.add(target.getResponseForQuestion(q));
                    assert !variantFound : "Answers to two of the same variant found.";
                    variantFound = true;
                }
            }
        }

        AbstractSurveyResponse asr = new AbstractSurveyResponse() {

            @Override
            public Map<String, IQuestionResponse> resultsAsMap() {
                Map<String, IQuestionResponse> retval = new HashMap<String, IQuestionResponse>();
                for (IQuestionResponse qr : responses)
                    retval.put(qr.getQuestion().quid, qr);
                return retval;
            }

            @Override
            public List<AbstractSurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException {
                throw new RuntimeException("Should not be calling readSurveyResponses from inside QCMetrics.");
            }

            @Override
            public boolean surveyResponseContainsAnswer(List<Component> variants) {
                for (IQuestionResponse qr : this.getResponses())
                    for (OptTuple optTuple : qr.getOpts())
                        if (variants.contains(optTuple.c))
                            return true;
                return false;
            }
        };
        asr.setResponses(responses);
        return asr;
    }

    public static List<AbstractSurveyResponse> truncateResponses(
            List<AbstractSurveyResponse> surveyResponses,
            AbstractSurveyResponse surveyResponse
    ) throws SurveyException {

        List<AbstractSurveyResponse> retval = new ArrayList<AbstractSurveyResponse>();

        for (AbstractSurveyResponse sr : surveyResponses) {
            AbstractSurveyResponse abstractSurveyResponse = getResponseSubset(surveyResponse, sr);
            if (abstractSurveyResponse!=null)
                retval.add(abstractSurveyResponse);
        }

        return retval;
    }

    public static List<List<AbstractSurveyResponse>> generateBootstrapSample(List<AbstractSurveyResponse> responseList, int iterations) {
        List<List<AbstractSurveyResponse>> retval = new ArrayList<List<AbstractSurveyResponse>>();
        for (int i = 0; i < iterations; i++) {
            List<AbstractSurveyResponse> sample = new ArrayList<AbstractSurveyResponse>();
            for (int j = 0 ; j < responseList.size() ; j++) {
                sample.add(responseList.get(Interpreter.random.nextInt(responseList.size())));
            }
            retval.add(sample);
        }
        return retval;
    }

    /**
     * Returns true if the response is valid, on the basis of the log likelihood.
     * @param survey The survey these respondents answered.
     * @param sr The survey response we are classifying.
     * @param responses The list of actual or simulated responses to the survey
     * @param smoothing Boolean indicating whether we should smooth our calculation of answer frequencies.
     * @param alpha The cutoff used for determining whether a likelihood is too low (a percentage of area under the curve).
     * @return
     */
    public static boolean logLikelihoodClassification(
            Survey survey,
            AbstractSurveyResponse sr,
            List<AbstractSurveyResponse> responses,
            boolean smoothing,
            double alpha
    ) throws SurveyException {
        Map<String, Map<String, Double>> probabilities = makeProbabilities(makeFrequencies(responses, smoothing ? survey : null));
        List<Double> lls = calculateLogLikelihoods(truncateResponses(responses, sr), probabilities);
        if (new HashSet<Double>(lls).size() > 5) {
            double thisLL = getLLForResponse(sr, probabilities);
            List<List<AbstractSurveyResponse>> bsSample = generateBootstrapSample(responses, 500);
            List<Double> means = new ArrayList<Double>();
            for (List<AbstractSurveyResponse> sample : bsSample) {
                double total = 0.0;
                for (AbstractSurveyResponse surveyResponse: sample) {
                    total += getLLForResponse(surveyResponse, probabilities);
                }
                means.add(total / sample.size());
            }
            Collections.sort(means);
            assert means.get(0) < means.get(means.size() - 1);
            //SurveyMan.LOGGER.info(String.format("Range of means: [%f, %f]", means.get(0), means.get(means.size() -1)));
            double threshHold = means.get((int) Math.floor(alpha * means.size()));
            //SurveyMan.LOGGER.info(String.format("Threshold: %f\tLL: %f", threshHold, thisLL));
            sr.setScore(thisLL);
            sr.setThreshold(threshHold);
            return thisLL > threshHold;
        } else return true;
    }

    /**
     * Return true if the response is valid, on the basis of an entropy-based metric.
     * @param survey The survey these respondents answered.
     * @param sr The survey response we are classifying.
     * @param responses The list of actual or simulated responses to the survey
     * @param smoothing Boolean indicating whether we should smooth our calculation of answer frequencies.
     * @param alpha The cutoff used for determining whether a likelihood is too low (a percentage of area under the curve).
     * @return
     */
    public static boolean entropyClassification(
            Survey survey,
            AbstractSurveyResponse sr,
            List<AbstractSurveyResponse> responses,
            boolean smoothing,
            double alpha
    ) throws SurveyException {
        // basically the same as logLikelihood, but scores are p * log p, rather than straight up p
        Map<String, Map<String, Double>> probabilities = makeProbabilities(makeFrequencies(responses, smoothing ? survey : null));
        List<Double> lls = calculateLogLikelihoods(truncateResponses(responses, sr), probabilities);
        if (new HashSet<Double>(lls).size() > 5) {
            double thisEnt = getEntropyForResponse(sr, probabilities);
            List<List<AbstractSurveyResponse>> bsSample = generateBootstrapSample(responses, 200);
            List<Double> means = new ArrayList<Double>();
            for (List<AbstractSurveyResponse> sample : bsSample) {
                assert sample.size() > 0 : "Sample size must be greater than 0.";
                double total = 0.0;
                for (AbstractSurveyResponse surveyResponse : sample) {
                    double ent = getEntropyForResponse(surveyResponse, probabilities);
                    total += ent;
                }
                means.add(total / sample.size());
            }
            Collections.sort(means);
            assert means.get(0) < means.get(means.size() - 1) :
                    String.format("Ranked means expected mean at position 0 to be greater than the mean at %d (%f < %f).",
                    means.size(), means.get(0), means.get(means.size() - 1));
            double threshHold = means.get((int) Math.floor(alpha * means.size()));
            sr.setThreshold(threshHold);
            sr.setScore(thisEnt);
            SurveyMan.LOGGER.debug(String.format("This entropy: %f\tThis threshold:%f", thisEnt, threshHold));
            return thisEnt < threshHold;
        } else return true;
    }

    public static boolean lpoClassification(Survey survey, AbstractSurveyResponse sr, List<AbstractSurveyResponse> responses) {
        return true;
    }
    //public double calculateBonus(AbstractSurveyResponse sr, Record record);
    //public double getBotThresholdForSurvey(Survey s);

    protected static void computeRanks(double[] xranks, List xs) {
        Object lastComponent = null;
        int startRun = Integer.MAX_VALUE;
        int endRun = 0;

        for (int i = 0 ; i < xs.size() ; i++) {
            if (lastComponent==null || !lastComponent.equals(xs.get(i))) {
                if (endRun > startRun) {
                    // then we need to distribute the ranks of the run of equals
                    double denominator = endRun - startRun + 1.0;
                    int numerator = ((endRun^2 + endRun) / 2) - ((startRun^2 + startRun) / 2) + startRun;
                    double rank = numerator / denominator;
                    for (; startRun <= endRun ; startRun++){
                        xranks[startRun] = rank;
                    }
                }
                xranks[i] = i+1;
                lastComponent = xs.get(i);
            } else {
                if (startRun >= endRun)
                    startRun = i;
                endRun = i;
            }
        }
    }

    protected static double spearmansRho(Map<String, IQuestionResponse> listA, Map<String, IQuestionResponse> listB) {
        // order the IQuestionResponses
        List<Component> xs = new ArrayList<Component>(), ys = new ArrayList<Component>();

        for (IQuestionResponse qr : listA.values()) {
            xs.add(qr.getOpts().get(0).c);
        }
        for (IQuestionResponse qr : listB.values()) {
            ys.add(qr.getOpts().get(0).c);
        }
        Collections.sort(xs);
        Collections.sort(ys);

        // compute ranks
        double[] xranks = new double[xs.size()];
        double[] yranks = new double[ys.size()];
        computeRanks(xranks, xs);
        computeRanks(yranks, ys);

        double sumOfSquares = 0.0;
        for (int i = 0; i < xranks.length; i++){
            sumOfSquares += Math.pow(xranks[i] - yranks[i], 2);
        }

        int n = xranks.length;

        return 1 - ((6 * sumOfSquares) / (n * (n^2 - 1)));
    }

    protected static double cellExpectation(int[][] contingencyTable, int i, int j, int n) {
        int o1 = 0, o2 = 0;
        for (int r = 0 ; r < contingencyTable.length ; r++)
            o1 += contingencyTable[r][j];
        for (int c = 0 ; c < contingencyTable[0].length; c++)
            o2 += contingencyTable[i][c];
        return o1 * o2 / ((double) n);
    }

    protected static double chiSquared(int[][] contingencyTable, Object[] categoryA, Object[] categoryB) {
        double testStatistic = 0.0;
        int numSamples = 0;
        for (int i = 0; i < contingencyTable.length; i ++)
            for (int j = 0; j < contingencyTable[i].length; j++)
                numSamples+=contingencyTable[i][j];
        for (int r = 0; r < categoryA.length; r++)
            for (int c = 0; c < categoryB.length; c++) {
                double eij = cellExpectation(contingencyTable, r, c, numSamples);
                if (eij == 0.0)
                    continue;
                testStatistic += Math.pow(contingencyTable[r][c] - eij, 2.0) / eij;
            }
        return testStatistic;
    }


    protected static double cramersV(Map<String, IQuestionResponse> listA, Map<String,IQuestionResponse> listB) {
        Question sampleQA = ((IQuestionResponse) listA.values().toArray()[0]).getQuestion();
        Question sampleQB = ((IQuestionResponse) listB.values().toArray()[0]).getQuestion();
        assert listA.size() == listB.size() : String.format(
                "Question responses have different sizes:\n%d for question %s\n%d for question %s",
                listA.size(), sampleQA,
                listB.size(), sampleQB
        );
        // get the categories for the contingency table:
        final Component[] categoryA = new Component[sampleQA.options.values().size()];
        final Component[] categoryB = new Component[sampleQB.options.values().size()];
        sampleQA.options.values().toArray(categoryA);
        sampleQB.options.values().toArray(categoryB);

        int r = categoryA.length;
        int c = categoryB.length;
        if (r==0 || c==0)
            return -0.0;
        // get the observations and put them in a contingency table:
        int[][] contingencyTable = new int[r][c];
        // initialize
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                contingencyTable[i][j] = 0;
        for (Map.Entry<String, IQuestionResponse> entry : listA.entrySet()) {
            String id = entry.getKey();
            Component ansA = entry.getValue().getOpts().get(0).c;
            Component ansB = listB.get(id).getOpts().get(0).c;
            int i = 0, j = 0;
            for (; i < r ; i++)
                if (categoryA[i].equals(ansA))
                    break;
            for (; j < c ; j++)
                if (categoryB[j].equals(ansB))
                    break;
            contingencyTable[i][j] += 1;
        }

        return Math.sqrt((chiSquared(contingencyTable, categoryA, categoryB) / listA.size()) / Math.min(c - 1, r - 1));
    }

    protected static double mannWhitney(List<Component> list1, List<Component> list2) {
        if (list1.size()==0 || list2.size()==0)
            return -0.0;
        // make ranks on the basis of the source row index
        Collections.sort(list1);
        Collections.sort(list2);
        double[] list1ranks = new double[list1.size()];
        double[] list2ranks = new double[list2.size()];
        for (int i = 0 ; i < list1.size() ; i++)
            list1ranks[i] = (double) list1.get(i).getSourceRow();
        for (int i = 0 ; i < list2.size() ; i++)
            list2ranks[i] = (double) list2.get(i).getSourceRow();
        return new MannWhitneyUTest().mannWhitneyUTest(list1ranks, list2ranks);
    }

    /**
     * Simulates a survey of 100% random uniform respondents over sampleSize and calculates a prior on false correlation.
     * @param survey The survey these respondents answered.
     * @param sampleSize The sample size the survey writer intends to use during the full-scale study.
     * @param alpha The cutoff used for determining correlation.
     * @return Empirical false correlation.
     * @throws SurveyException
     */
    public static Map<Question, Map<Question, CorrelationStruct>> getFrequenciesOfRandomCorrelation(
            Survey survey,
            int sampleSize,
            double alpha)
            throws SurveyException
    {

        Map<Question, Map<Question, CorrelationStruct>> corrs =
                new HashMap<Question, Map<Question, CorrelationStruct>>();
        List<RandomRespondent> randomRespondents =
                new ArrayList<RandomRespondent>();

        for (int i = 0 ; i < sampleSize; i++){
            randomRespondents.add(new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM));
        }

        for (Question q1 : survey.questions) {
            if (!q1.exclusive) continue;
            assert !q1.freetext : String.format(
                    "Cannot have a freetext question with exclusive set to true (%s)", q1);
            for (Question q2: survey.questions) {
                if (!q2.exclusive) continue;
                assert !q2.freetext : String.format(
                        "Cannot have a freetext question with exclusive set to true (%s), q2");
                // get responses having answered both questions
                Map<String, IQuestionResponse> q1responses = new HashMap<String, IQuestionResponse>();
                Map<String, IQuestionResponse> q2responses = new HashMap<String, IQuestionResponse>();
                for (RandomRespondent rr : randomRespondents) {

                    IQuestionResponse qr1 = null;
                    IQuestionResponse qr2 = null;

                    for (IQuestionResponse qr : rr.getResponse().getResponses()) {
                        if (qr.getQuestion().equals(q1))
                            qr1 = qr;
                        if (qr.getQuestion().equals(q2))
                            qr2 = qr;
                        if (qr1!=null && qr2!=null)
                            break;
                    }

                    if (qr1!=null && qr2!=null){
                        q1responses.put(rr.id, qr1);
                        q2responses.put(rr.id, qr2);
                    }
                }
                // compute the appropriate correlation coefficient
                Map<Question, CorrelationStruct> stuff = new HashMap<Question, CorrelationStruct>();
                if (q1.ordered && q2.ordered)
                    stuff.put(q2, new CorrelationStruct(
                            CoefficentsAndTests.RHO,
                            spearmansRho(q1responses, q2responses),
                            q1,
                            q2,
                            q1responses.size(),
                            q2responses.size()));
                else
                    stuff.put(q2, new CorrelationStruct(
                            CoefficentsAndTests.V,
                            cramersV(q1responses, q2responses),
                            q1,
                            q2,
                            q1responses.size(),
                            q2responses.size()
                    ));
                corrs.put(q1, stuff);
                // count how many p-values are below the threshhold.
            }
        }
        return corrs;
    }

    private static List<IQuestionResponse> removeCustomQuestions(
            AbstractSurveyResponse sr)
    {
        // remove custom questions
        List<IQuestionResponse> qrs = new ArrayList<IQuestionResponse>();
        for (IQuestionResponse qr : sr.getResponses()) {
            if (!Question.customQuestion(qr.getQuestion().quid))
                qrs.add(qr);
        }
        return qrs;
    }

    public static BreakoffByPosition calculateBreakoffByPosition (
            Survey survey,
            List<AbstractSurveyResponse> responses)
    {
        // for now this just reports breakoff, rather than statistically significant breakoff
        BreakoffByPosition breakoffMap = new BreakoffByPosition(survey);
        for (AbstractSurveyResponse sr : responses) {
            int answerLength = removeCustomQuestions(sr).size();
            //TODO(etosch): remove legit final positions from breakoff.
            breakoffMap.update(answerLength);
        }
        return breakoffMap;

    }

    public static BreakoffByQuestion calculateBreakoffByQuestion (
            Survey survey,
            List<AbstractSurveyResponse> responses)
    {
        BreakoffByQuestion breakoffMap = new BreakoffByQuestion(survey);
        for (AbstractSurveyResponse sr : responses) {
            // get the last question responded to
            List<IQuestionResponse> qrs = removeCustomQuestions(sr);
            IQuestionResponse lastQuestionAnswered = qrs.get(0);
            for (IQuestionResponse qr : qrs)
               if (qr.getIndexSeen() > lastQuestionAnswered.getIndexSeen())
                   lastQuestionAnswered = qr;
            //TODO(etosch): remove legit final questions from breakoff.
            breakoffMap.update(lastQuestionAnswered.getQuestion());
        }
        return breakoffMap;
    }

    public static WordingBiasStruct calculateWordingBiases (
            Survey survey,
            List<AbstractSurveyResponse> responses,
            double alpha)
            throws SurveyException {
        WordingBiasStruct retval = new WordingBiasStruct(survey, alpha);
        // get variants
        for (Block b : survey.blocks.values()) {
            if (b.branchParadigm.equals(Block.BranchParadigm.ALL)) {
                List<Question> variants = b.branchQ.getVariants();
                for (Question q1: variants) {
                    if (!q1.exclusive)
                        continue;
                    for (Question q2: variants) {
                        assert q2.exclusive : "All question variants must have the same parameter settings.";
                        List<Component> q1answers = new ArrayList<Component>();
                        List<Component> q2answers = new ArrayList<Component>();
                        for (AbstractSurveyResponse sr : responses) {
                            if (sr.hasResponseForQuestion(q1))
                                q1answers.add(sr.getResponseForQuestion(q1).getOpts().get(0).c);
                            if (sr.hasResponseForQuestion(q2))
                                q2answers.add(sr.getResponseForQuestion(q2).getOpts().get(0).c);
                        }
                        if (q1.exclusive && q2.exclusive) {
                            retval.update(b, q1, q2, new CorrelationStruct(
                                    CoefficentsAndTests.U,
                                    mannWhitney(q1answers, q2answers),
                                    q1,
                                    q2,
                                    q1answers.size(),
                                    q2answers.size())
                            );
                        } else {
                            // sort by their source rows
                            List<Component> categoryA = Arrays.asList(q1.getOptListByIndex());
                            List<Component> categoryB = Arrays.asList(q2.getOptListByIndex());
                            Collections.sort(categoryA);
                            Collections.sort(categoryB);
                            int[][] contingencyTable = new int[categoryA.size()][2];
                            // initialize the contingency table
                            for (int i = 0 ; i < categoryA.size() ; i++) {
                                contingencyTable[i][0] = 0;
                                contingencyTable[i][1] = 0;
                            }

                            for (Component c : q1answers)
                                contingencyTable[0][categoryA.indexOf(c)] += 1;
                            for (Component c : q2answers)
                                contingencyTable[0][categoryA.indexOf(c)] += 1;

                            retval.update(b, q1, q2, new CorrelationStruct(
                                    CoefficentsAndTests.CHI,
                                    chiSquared(contingencyTable, categoryA.toArray(), new List[]{q1answers, q2answers}),
                                    q1,
                                    q2,
                                    q1answers.size(),
                                    q2answers.size())
                            );
                        }
                    }
                }
            }
        }
        return retval;
    }

    public static OrderBiasStruct calculateOrderBiases (
            Survey survey,
            List<AbstractSurveyResponse> responses,
            double alpha)
            throws SurveyException {
        OrderBiasStruct retval = new OrderBiasStruct(survey, alpha);
        for (Question q1 : survey.questions) {
            for (Question q2 : survey. questions) {
                if (!q1.exclusive || q1.equals(q2))
                    break;
                else {
                    // q1 answers when q1 comes first
                    List<Component> q1q2 = new ArrayList<Component>();
                    // q1 answers when q1 comes second
                    List<Component> q2q1 = new ArrayList<Component>();
                    for (AbstractSurveyResponse sr : responses) {
                        if (sr.hasResponseForQuestion(q1) && sr.hasResponseForQuestion(q2)) {
                            IQuestionResponse qr1 = sr.getResponseForQuestion(q1);
                            IQuestionResponse qr2 = sr.getResponseForQuestion(q2);
                            if (qr1.getIndexSeen() < qr2.getIndexSeen())
                                // add the response to question 1 to the list of q1s that precede q2s
                                q1q2.add(qr1.getOpts().get(0).c);
                            else if (qr1.getIndexSeen() > qr2.getIndexSeen())
                                // add the response to question 1 to the list of q1s that succeed q2s
                                q2q1.add(qr1.getOpts().get(0).c);
                        }
                    }
                    if (q1.ordered && q2.ordered)
                        retval.update(q1, q2, new CorrelationStruct(
                                CoefficentsAndTests.U,
                                mannWhitney(q1q2, q2q1),
                                q1,
                                q2,
                                q1q2.size(),
                                q2q1.size())
                        );
                    else {
                        Component[] categoryA = q1.getOptListByIndex();
                        int[][] contingencyTable = new int[categoryA.length][2];
                        for (int i = 0 ; i < categoryA.length ; i++ ){
                            contingencyTable[i][0] = 0;
                            contingencyTable[i][1] = 0;
                        }
                        // if the difference in the observations is large, the orderings is incomparable
                        // make this more principled in the future.
                        double ratio = q1q2.size() / (double) q1q2.size();
                        if (q1q2.size() < 5 || q2q1.size() < 5 || (ratio > 0.8 && ratio < 1.2))
                            break;
                        for (Component c : q1q2)
                            contingencyTable[Arrays.asList(categoryA).indexOf(c)][0] += 1;
                        for (Component c : q2q1)
                            contingencyTable[Arrays.asList(categoryA).indexOf(c)][1] += 1;
                        retval.update(q1, q2, new CorrelationStruct(
                                CoefficentsAndTests.CHI,
                                chiSquared(contingencyTable, categoryA, new List[]{q1q2, q2q1}),
                                q1,
                                q2,
                                q1q2.size(),
                                q2q1.size()));
                    }
                }
            }
        }
        return retval;
    }

    public static ClassifiedRespondentsStruct classifyResponses(
            Survey survey,
            List<AbstractSurveyResponse> responses,
            Classifier classifier,
            boolean smoothing,
            double alpha)
            throws SurveyException {
        ClassifiedRespondentsStruct classificationStructs = new ClassifiedRespondentsStruct();
        for (AbstractSurveyResponse sr : responses) {
            boolean valid;
            double score, threshold;
            switch (classifier) {
                case ENTROPY:
                    valid = QCMetrics.entropyClassification(survey, sr, responses, smoothing, alpha);
                    score = sr.getScore();
                    threshold = sr.getThreshold();
                    classificationStructs.add(new ClassificationStruct(sr, classifier, score, threshold, valid));
                    break;
                case LOG_LIKELIHOOD:
                    valid = QCMetrics.logLikelihoodClassification(survey, sr, responses, smoothing, alpha);
                    score = sr.getScore();
                    threshold = sr.getThreshold();
                    classificationStructs.add(new ClassificationStruct(sr, classifier, score, threshold, valid));
                    break;
                default:
                    throw new RuntimeException("Unknown classification policy: "+classifier);
            }
        }
        return classificationStructs;
    }
}

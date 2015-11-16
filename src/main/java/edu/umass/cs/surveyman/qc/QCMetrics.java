package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.*;
import edu.umass.cs.surveyman.output.*;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.MersenneRandom;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;

public class QCMetrics {

    static class QCSurveyException extends SurveyException {
        public QCSurveyException(String msg) {
            super(msg);
        }
    }

    public static final MersenneRandom rng = new MersenneRandom();
    public static int bootstrapIterations = 2000;
    private static double log2(double p) {
        if (p == 0)
            return 0.0;
        return Math.log(p) / Math.log(2.0);
    }
    private SurveyDAG surveyDAG;
    private List<SurveyPath> surveyPaths;
    public final Survey survey;
    private boolean smoothing;
    private AnswerFrequencyMap answerFrequencyMap;
    private AnswerProbabilityMap answerProbabilityMap;
    private double alpha;
    private int numClusters;
    private static Set<Question> notAnalyzable = new HashSet<>();

    public QCMetrics(Survey survey) {
        this(survey, false, 0.05, 2);
    }

    public QCMetrics(Survey survey, boolean smoothing, double alpha, int numClusters) {
        this.surveyDAG = SurveyDAG.getDag(survey);
        this.surveyPaths = SurveyDAG.getPaths(survey);
        this.survey = survey;
        this.smoothing = smoothing;
        this.alpha = alpha;
        this.numClusters = numClusters;
    }

    public int maximumPathLength() {
        return surveyDAG.maximumPathLength();
    }

    private static boolean alreadyWarned(Question question) {
        return notAnalyzable.contains(question);
    }

    public static boolean isAnalyzable(Question question) {
        // freetext will be null if we've designed the survey programmatically and have
        // not added any questions (i.e. if it's an instructional question).
        if (question.freetext == null)
            question.freetext = false;
        boolean analyzable = !question.freetext && !question.isInstructional() && !question.isCustomQuestion();
        if (!analyzable && !alreadyWarned(question)) {
            SurveyMan.LOGGER.debug(String.format("Skipping question [%s]: not analysable.", question));
            notAnalyzable.add(question);
        }
        return analyzable;
    }

    /**
     * Filter the list of questions for things that can be analyzed in terms of survey correctness.
     * This means questions that are not instructional, not custom questions, and not freetext.
     * @param questionList The list to filter.
     * @return A new list consisting only of analyzable questions.
     */
    public static List<Question> filterAnalyzable(List<Question> questionList) {
        List<Question> questions = new ArrayList<>();
        for (Question q : questionList)
            if (isAnalyzable(q))
                questions.add(q);
        return questions;
    }

    /**
     * Returns equivalent answer options (a list of survey.SurveyDatum)
     * @param q The question whose variants we want. If there are no variants, then a set of just this question is
     *          returned.
     * @param c The answer the respondent provided for this question.
     * @return A list of the equivalent answers.
     */
    protected static List<SurveyDatum> getEquivalentAnswerVariants(Question q, SurveyDatum c) {

        List<SurveyDatum> retval = new ArrayList<>();
        List<Question> variants = q.getVariants();
        int offset = q.getSourceRow() - c.getSourceRow();
        for (Question variant : variants) {
            for (SurveyDatum thisC : variant.options.values()) {
                int thisOffset = variant.getSourceRow() - thisC.getSourceRow();
                if (thisOffset == offset)
                    retval.add(thisC);
            }
        }
        //SurveyMan.LOGGER.debug("Variant set size: " + retval.size());
        return retval;
    }

    /**
     * Calculates the empirical entropy for this survey, given a set of responses.
     * @param survey The survey these respondents answered.
     * @param responses The list of actual or simulated responses to the survey.
     * @return The caluclated base-2 entropy.
     */
    public static double surveyEntropy(Survey survey, List<? extends SurveyResponse> responses) {
        List<SurveyPath> paths = SurveyDAG.getPaths(survey);
        PathFrequencyMap pathMap = PathFrequencyMap.makeFrequenciesForPaths(paths, responses);
        int totalResponses = responses.size();
        assert totalResponses > 1 : "surveyEntropy is meaningless for fewer than 1 response.";
        double retval = 0.0;
        for (Question q : filterAnalyzable(survey.questions))
            for (SurveyDatum c : q.options.values()) {
                for (SurveyPath path : paths) {
                    List<SurveyDatum> variants = getEquivalentAnswerVariants(q, c);
                    List<? extends SurveyResponse> responsesThisPath = pathMap.get(path);
                    double ansThisPath = 0.0;
                    for (SurveyResponse r : responsesThisPath) {
                        boolean responseInThisPath = r.surveyResponseContainsAnswer(variants);
                        if (responseInThisPath) {
                            //SurveyMan.LOGGER.info("Found an answer on this path!");
                            ansThisPath += 1.0;
                        }
                    }
                    double p = ansThisPath / (double) totalResponses;
                    retval += log2(p) * p;
                }
            }
        return -retval;
    }

    /**
     * Returns the maximum possible entropy for a single Question.
     * @param question The question of interest.
     * @return An entropy-based calculation that distributes mass across all possible options equally.
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
     * @param questionList The list of questions whose entropy we want.
     * @return The total entropy for a list of Questions.
     */
    private static double maxEntropyQuestionList(List<Question> questionList){
        double retval = 0.0;
        for (Question q : questionList) {
            retval += maxEntropyOneQuestion(q);
        }
        return retval;
    }

    /**
     * Returns the path with the highest entropy.
     * @param blists List of paths through the survey, where the path is defined by a list of blocks.
     * @return The path through the survey having the maximum entropy, expressed as a block list.
     */
    private static SurveyPath getMaxPathForEntropy(List<SurveyPath> blists) {
        SurveyPath retval = null;
        double maxEnt = 0.0;
        for (SurveyPath blist : blists) {
            double ent = maxEntropyQuestionList(blist.getQuestionsFromPath());
            if (ent > maxEnt) {
                maxEnt = ent;
                retval = blist;
            }
        }
        return retval;
    }

    /**
     * The public method used to compute the maximum number of bits needed to represent this survey.
     * @return The maximum possible entropy for this source.
     */
    public double getMaxPossibleEntropy() {
        double maxEnt = maxEntropyQuestionList(getMaxPathForEntropy(surveyPaths).getQuestionsFromPath());
        SurveyMan.LOGGER.info(String.format("Maximum possible entropy for survey %s: %f", survey.sourceName, maxEnt));
        return maxEnt;
    }

    public int minimumPathLength() {
        int min = Integer.MAX_VALUE;
        for (SurveyPath path : surveyPaths) {
            int pathLength = path.getPathLength();
            if (pathLength < min)
                min = pathLength;
        }
        SurveyMan.LOGGER.info(String.format("Survey %s has minimum path length of %d", survey.sourceName, min));
        return min;
    }

    /**
     * Simulates the survey for 5000 uniform random respondents and returns the average path length.
     * @return average path length.
     * @throws SurveyException
     */
    public double averagePathLength() throws SurveyException {
        int n = 5000;
        int stuff = 0;
        for (int i = 0 ; i < n ; i++) {
            RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
            stuff += rr.getResponse().getNonCustomResponses().size();
        }
        double avg = (double) stuff / n;
        SurveyMan.LOGGER.info(String.format("Survey %s has average path length of %f", survey.sourceName, avg));
        return avg;
    }

    /**
     * Creates a frequency map for the actual responses to the survey.
     * @param responses The list of actual or simulated responses to the survey.
     */
    public void makeFrequencies(List<? extends SurveyResponse> responses) {
        // map from question id to a map from answer id to counts
        this.answerFrequencyMap = new AnswerFrequencyMap();
        Set<String> allAnswerOptionIdsSelected = new HashSet<>();
        for (SurveyResponse sr : responses) {
            for (IQuestionResponse qr : sr.getNonCustomResponses()) {
                Question question = qr.getQuestion();
                if (!isAnalyzable(question)) continue;
                String quid = question.id;
                // get the answer option map associated with this question
                HashMap<String, Integer> tmp;
                if (answerFrequencyMap.containsKey(quid)) {
                    tmp = answerFrequencyMap.get(quid);
                } else {
                    tmp = new HashMap<>();
                    answerFrequencyMap.put(quid, tmp);
                }
                List<String> aids = OptTuple.getCids(qr.getOpts());
                for (String cid : aids) {
                    allAnswerOptionIdsSelected.add(cid);
                    if (tmp.containsKey(cid))
                        tmp.put(cid, tmp.get(cid) + 1);
                    else tmp.put(cid, 1);
                }
            }
        }
        // LaPlace (+1 smoothing)
        if (this.smoothing) {
            int numberNeedingSmoothing = 0;
            for (Question q : survey.questions) {
                for (SurveyDatum c : q.options.values()) {
                    if (!answerFrequencyMap.containsKey(q.id)) {
                        answerFrequencyMap.put(q.id, new HashMap<String, Integer>());
                    }
                    answerFrequencyMap.get(q.id).put(c.getId(), answerFrequencyMap.get(q.id).get(c.getId()) + 1);
                    if (!allAnswerOptionIdsSelected.contains(c.getId())) {
                        numberNeedingSmoothing++;
                    }
                }
            }
            if (numberNeedingSmoothing > 0)
                SurveyMan.LOGGER.info("Number needing smoothing " + numberNeedingSmoothing);
        }
    }

    /**
     * Populates the empirical probabilities of the responses.
     */
    private void makeProbabilities() {
        this.answerProbabilityMap = new AnswerProbabilityMap();
        for (Map.Entry<String, HashMap<String, Integer>> e: this.answerFrequencyMap.entrySet()) {
            String quid = e.getKey();
            HashMap<String, Integer> map = e.getValue();
            double total = 0.0;
            for (Integer i : map.values()) {
                total += i;
            }
            answerProbabilityMap.put(quid, new HashMap<String, Double>());
            for (String cid : map.keySet()) {
                answerProbabilityMap.get(quid).put(cid, map.get(cid) / total);
            }
        }
    }

    public void makeProbabilities(List<? extends SurveyResponse> responses) {
        this.makeFrequencies(responses);
        this.makeProbabilities();
    }

    private Map<Set<Question>, List<List<SurveyResponse>>> cache = new HashMap<>();
    protected List<List<SurveyResponse>> cachedQuestionSet(SurveyResponse sr, List<? extends SurveyResponse> responses) {

        Set<Question> questions = new HashSet<>();
        for (IQuestionResponse qr : sr.getAllResponses())
            questions.add(qr.getQuestion());

        if (cache.containsKey(questions))
            return cache.get(questions);

        List<List<SurveyResponse>> bssample = generateBootstrapSample(responses);
        cache.put(questions, bssample);
        return bssample;
    }

    private Map<Classifier, Map<List<List<SurveyResponse>>, List<Double>>> means = new HashMap<>();
    protected List<Double> cachedMeans(SurveyResponse sr,
                                       List<? extends SurveyResponse> responses,
                                       Classifier classifier) throws SurveyException {

        List<Double> retval = new ArrayList<>();
        List<List<SurveyResponse>> bsSample = cachedQuestionSet(sr, responses);
        if (means.containsKey(classifier) && means.get(classifier).containsKey(bsSample))
            return means.get(classifier).get(bsSample);


        for (List<? extends SurveyResponse> sample : bsSample) {
            double total = 0.0;
            for (SurveyResponse surveyResponse: sample) {
                switch (classifier) {
                    case LOG_LIKELIHOOD:
                        total += getLLForResponse(getResponseSubset(sr, surveyResponse));
                        break;
                    case ENTROPY:
                        total += getEntropyForResponse(surveyResponse);
                        break;
                    default:
                        throw new RuntimeException("FML");

                }
            }
            retval.add(total / sample.size());
        }
        assert retval.size() == bsSample.size();
        Collections.sort(retval);
        assert retval.get(0) < retval.get(retval.size() - 1) :
                String.format("Ranked means expected mean at position 0 to be greater than the mean at %d (%f < %f).",
                        retval.size(), retval.get(0), retval.get(retval.size() - 1));
        if (!means.containsKey(classifier))
            means.put(classifier, new HashMap<List<List<SurveyResponse>>, List<Double>>());
        Map<List<List<SurveyResponse>>, List<Double>> classifiersMeans = means.get(classifier);
        classifiersMeans.put(bsSample, retval);
        return retval;
    }

    public double getLLForResponse(SurveyResponse surveyResponse) throws SurveyException {
        return getLLForResponse(surveyResponse.getAllResponses());
    }

    private double getLLForResponse(List<IQuestionResponse> questionResponseList) throws SurveyException {
        if (this.answerProbabilityMap == null) {
            throw new QCSurveyException("Cannot compute the log likelihood of a response without computing the empirical distribution of responses.");
        }
        double ll = 0.0;
        for (IQuestionResponse questionResponse : questionResponseList) {
            Question question = questionResponse.getQuestion();
            if (!isAnalyzable(question)) continue;
            String qid = question.id;
            for (String cid : OptTuple.getCids(questionResponse.getOpts())) {
                ll += log2(this.answerProbabilityMap.get(qid).get(cid));
            }
        }
        return ll;
    }

    public double getEntropyForResponse(SurveyResponse surveyResponse) throws SurveyException {
        return getEntropyForResponse(surveyResponse.getAllResponses());
    }

    private double getEntropyForResponse(List<IQuestionResponse> questionResponseList) throws SurveyException {
        if (this.answerProbabilityMap == null) {
            throw new QCSurveyException("Cannot compute the log likelihood of a response without computing the empirical distribution of responses.");
        }
        double ent = 0.0;
        for (IQuestionResponse questionResponse : questionResponseList) {
            Question question = questionResponse.getQuestion();
            if (!isAnalyzable(question)) continue;
            String qid = question.id;
            for (String cid : OptTuple.getCids(questionResponse.getOpts())) {
                double p = this.answerProbabilityMap.get(qid).get(cid);
                assert p > 0.0;
                ent += p * log2(p);
            }
        }
        return -ent;
    }

    private List<Double> calculateLogLikelihoods(SurveyResponse base, List<? extends SurveyResponse> responses) throws SurveyException {
        this.makeProbabilities(responses);
        List<Double> retval = new LinkedList<>();
        // get the first response count
        int responseSize = base.getNonCustomResponses().size();
        for (SurveyResponse sr : responses) {
            List<IQuestionResponse> questionResponses = getResponseSubset(base, sr);
            int thisresponsesize = questionResponses.size();
            if (thisresponsesize == 0)
                continue;
            assert responseSize == thisresponsesize : String.format(
                    "Expected %d responses, got %d",
                    responseSize,
                    thisresponsesize);
            retval.add(getLLForResponse(questionResponses));
        }
        return retval;
    }

    private static List<IQuestionResponse> getResponseSubset(SurveyResponse base, SurveyResponse target) throws SurveyException {
        // These will be used to generate the return value.
        List<IQuestionResponse> responses = new ArrayList<>();

        // For each question in our base response, check whether the target has answered that question or one of its
        // variants.
        for (IQuestionResponse qr : base.getAllResponses()) {
            Question question = qr.getQuestion();
            if (!isAnalyzable(question))
                continue;
            // Get the variants for this question.
            List<Question> variants = question.getVariants();
            boolean variantFound = false;
            for (Question q : variants) {
                if (target.hasResponseForQuestion(q)) {
                    responses.add(target.getResponseForQuestion(q));
                    assert !variantFound : "Answers to two of the same variant found.";
                    variantFound = true;
                }
            }
            if (!variantFound)
                return new ArrayList<>();
        }
        return responses;
    }

    /**
     * Generates the bootstrap sample for the input response and the specified number of iterations. Default 2000.
     * @param responses The list of actual or simulated responses to the survey.
     * @return The bootstrapped sample of possible survey responses.
     */
    public static List<List<SurveyResponse>> generateBootstrapSample(List<? extends SurveyResponse> responses) {
        List<List<SurveyResponse>> retval = new ArrayList<>();
        for (int i = 0; i < bootstrapIterations; i++) {
            List<SurveyResponse> sample = new ArrayList<>();
            for (int j = 0 ; j < responses.size() ; j++) {
                sample.add(responses.get(rng.nextInt(responses.size())));
            }
            retval.add(sample);
        }
        return retval;
    }

    protected List<Double> computeMeans(SurveyResponse sr, List<? extends SurveyResponse> responses, Classifier classifier) throws SurveyException {

        List<Double> retval = new ArrayList<>();
        List<List<SurveyResponse>> bsSample = generateBootstrapSample(responses);

        for (List<? extends SurveyResponse> sample : bsSample) {
            double total = 0.0;
            for (SurveyResponse surveyResponse: sample) {
                switch (classifier) {
                    case LOG_LIKELIHOOD:
                        total += getLLForResponse(getResponseSubset(sr, surveyResponse));
                        break;
                    case ENTROPY:
                        total += getEntropyForResponse(surveyResponse);
                        break;
                    default:
                        throw new RuntimeException("FML");

                }
            }
            retval.add(total / sample.size());
        }
        assert retval.size() == bsSample.size();
        Collections.sort(retval);
        assert retval.get(0) < retval.get(retval.size() - 1) :
                    String.format("Ranked means expected mean at position 0 to be greater than the mean at %d (%f < %f).",
                            retval.size(), retval.get(0), retval.get(retval.size() - 1));
        return retval;
    }

    /**
     * Returns true if the response is valid, on the basis of the log likelihood.
     * @param sr The survey response we are classifying.
     * @param responses The list of actual or simulated responses to the survey
     * @param alpha The cutoff used for determining whether a likelihood is too low (a percentage of area under the curve).
     * @return Decision indicating whether the input response is valid.
     */
    public boolean logLikelihoodClassification(SurveyResponse sr, List<? extends SurveyResponse> responses, double alpha) throws SurveyException {
        if (answerProbabilityMap == null) {
            makeProbabilities(responses);
        }

        List<Double> lls = calculateLogLikelihoods(sr, responses);
        Set<Double> llSet = new HashSet<>(lls);

        if (llSet.size() > 5) {

            double thisLL = getLLForResponse(sr.getNonCustomResponses());
            List<Double> means = cachedMeans(sr, responses, Classifier.LOG_LIKELIHOOD);
            //SurveyMan.LOGGER.info(String.format("Range of means: [%f, %f]", means.get(0), means.get(means.size() -1)));
            double threshHold = means.get((int) Math.floor(alpha * means.size()));
            //SurveyMan.LOGGER.info(String.format("Threshold: %f\tLL: %f", threshHold, thisLL));
            sr.setScore(thisLL);
            sr.setThreshold(threshHold);
            return thisLL > threshHold;

        } else {
            SurveyMan.LOGGER.debug(String.format("Not enough samples to compute a distribution: %d", llSet.size()));
            return true;
        }
    }

    /**
     * Computes the validity of the input responses, based on the "Least popular option" metric.
     * @param responses The survey respondents' responses.
     * @param epsilon A tunable parameter for defining the least popular option.
     *                <ol>
     *                    <li>Sorts the answers according to frequency.</li>
     *                    <li>Working backwards from the most frequent response, selects the set of least popular
     *                responses after the first multiplicative difference of size <i>epsilon</i></li>
     *                </ol>
     * @throws SurveyException
     */
    public void lpoClassification(List<? extends SurveyResponse> responses, double epsilon) throws SurveyException {

        if (answerProbabilityMap == null) {
            makeProbabilities(responses);
        }

        Map<Question, List<SurveyDatum>> lpos = new HashMap<>();

        for (Question q: survey.getQuestionListByIndex()) {

            if (!answerProbabilityMap.containsKey(q.id))
                continue;

            Map<String, Integer> cmap = answerFrequencyMap.get(q.id);
            Integer[] crap = new Integer[cmap.size()];
            cmap.values().toArray(crap);
            Arrays.sort(crap);

            if (crap.length > 1)
                assert crap[0] <= crap[1];
            else continue;

            List<SurveyDatum> theseLPOs = new ArrayList<>();

            for (Map.Entry<String, Integer> e : cmap.entrySet()) {
                if (e.getValue().equals(crap[0])) {
                    theseLPOs.add(q.getOptById(e.getKey()));
                    break;
                }
            }

            if (theseLPOs.size() == cmap.size())
                continue;

            for (int i = 1; i < crap.length; i++) {
                if (crap[i] > (1 + epsilon) * crap[i - 1])
                    break;
                else {
                    for (Map.Entry<String, Integer> e : cmap.entrySet()) {
                        if (e.getValue().equals(crap[i])) {
                            theseLPOs.add(q.getOptById(e.getKey()));
                            break;
                        }
                    }
                }
            }
            lpos.put(q, theseLPOs);
        }
        // let delta be 0.5
        double delta = 0.5;
        double mu = 0.0;
        for (Question q : survey.questions) {
            if (lpos.containsKey(q))
                mu += lpos.get(q).size() / (1.0 * q.options.size());
        }
        double threshold = (1 - delta) * mu;
        double percentage = threshold / lpos.size();
        for (SurveyResponse sr : responses) {
            int ct = 0;
            for (IQuestionResponse questionResponse : sr.getAllResponses()) {
                Question q = questionResponse.getQuestion();
                if (lpos.containsKey(q)) {
                    List<SurveyDatum> theseLPOs = lpos.get(questionResponse.getQuestion());
                    if ((q.exclusive && theseLPOs.contains(questionResponse.getAnswer())) ||
                        (!q.exclusive && theseLPOs.containsAll(questionResponse.getAnswers())))
                        ct += 1;
                }
            }
            sr.setThreshold(percentage * sr.resultsAsMap().size());
            sr.setScore(ct);
            sr.setComputedValidityStatus(ct > threshold ? KnownValidityStatus.NO : KnownValidityStatus.YES);
        }
    }

    /**
     * Return true if the response is valid, on the basis of an entropy-based metric.
     * @param sr The survey response we are classifying.
     * @param responses The list of actual or simulated responses to the survey
     * @param alpha The cutoff used for determining whether a likelihood is too low (a percentage of area under the curve).
     * @return boolean indicating whether the response is valid.
     */
    public boolean entropyClassification(SurveyResponse sr, List<? extends SurveyResponse> responses, double alpha) throws SurveyException {
        // basically the same as logLikelihood, but scores are p * log p, rather than straight up p
        List<Double> lls = calculateLogLikelihoods(sr, responses);
        Set<Double> scoreSet = new HashSet<>(lls);
        if (scoreSet.size() > 5) {
            double thisEnt = getEntropyForResponse(sr);
            List<Double> means = cachedMeans(sr, responses, Classifier.ENTROPY);
            double threshHold = means.get((int) Math.ceil(alpha * means.size()));
            boolean valid = thisEnt < threshHold;
            sr.setThreshold(threshHold);
            sr.setScore(thisEnt);
            sr.setComputedValidityStatus(valid ? KnownValidityStatus.YES : KnownValidityStatus.NO);
            //SurveyMan.LOGGER.debug(String.format("This entropy: %f\tThis threshold:%f", thisEnt, threshHold));
            return valid;
        } else {
            SurveyMan.LOGGER.debug(String.format("Not enough samples to compute a distribution: %d", scoreSet.size()));
            return true;
        }
    }

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
        List<SurveyDatum> xs = new ArrayList<>(), ys = new ArrayList<>();

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

    protected static double cellExpectation(
            int[][] contingencyTable,
            int i,
            int j,
            int n)
    {
        int o1 = 0, o2 = 0;
        for (int[] aContingencyTable : contingencyTable)
            o1 += aContingencyTable[j];
        for (int c = 0 ; c < contingencyTable[0].length; c++)
            o2 += contingencyTable[i][c];
        return o1 * o2 / ((double) n);
    }

    /**
     * Returns the chi-squared statistic for the input data.
     * @param contingencyTable
     * @param categoryA
     * @param categoryB
     * @return
     */
    public static double chiSquared(
            int[][] contingencyTable,
            Object[] categoryA,
            Object[] categoryB)
    {
        double testStatistic = 0.0;
        int numSamples = 0;
        for (int[] aContingencyTable : contingencyTable)
            for (int anAContingencyTable : aContingencyTable) numSamples += anAContingencyTable;
        for (int r = 0; r < categoryA.length; r++)
            for (int c = 0; c < categoryB.length; c++) {
                double eij = cellExpectation(contingencyTable, r, c, numSamples);
                if (eij == 0.0)
                    continue;
                testStatistic += Math.pow(contingencyTable[r][c] - eij, 2.0) / eij;
            }
        return testStatistic;
    }

    public static double chiSquareTest(int df, double testStatistic) {
        ChiSquaredDistribution chi = new ChiSquaredDistribution(df);
        return chi.density(testStatistic);
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
        final SurveyDatum[] categoryA = new SurveyDatum[sampleQA.options.values().size()];
        final SurveyDatum[] categoryB = new SurveyDatum[sampleQB.options.values().size()];
        sampleQA.options.values().toArray(categoryA);
        sampleQB.options.values().toArray(categoryB);

        int r = categoryA.length;
        int c = categoryB.length;
        if (r==0 || c==0)
            return -0.0;
        // get the observations and put them in a contingency table:
        int[][] contingencyTable = new int[r][c];
        // initialize
        for (int i = 0; i < r; i++) Arrays.fill(contingencyTable[i], 0);
        for (Map.Entry<String, IQuestionResponse> entry : listA.entrySet()) {
            // Tabulate the places where A and B agree
            String id = entry.getKey();
            SurveyDatum ansA = entry.getValue().getOpts().get(0).c;
            SurveyDatum ansB = listB.get(id).getOpts().get(0).c;
            int i = 0, j = 0;
            for (; i < r ; i++)
                if (categoryA[i].equals(ansA))
                    break;
            for (; j < c ; j++)
                if (categoryB[j].equals(ansB))
                    break;
            // If they never co-occur
            if (i==r || j==c) {
                SurveyMan.LOGGER.warn(
                        String.format("No co-occurances of %s and %s -- consider using smoothing",
                        ansA, ansB));
                continue;
            }
            contingencyTable[i][j] += 1;
        }

        return Math.sqrt((chiSquared(contingencyTable, categoryA, categoryB) / listA.size()) / Math.min(c - 1, r - 1));
    }

    /**
     * Mann-Whitney statistic, specialized for comparing survey questions.
     * @param q1
     * @param q2
     * @param list1
     * @param list2
     * @return
     */
    public static ImmutablePair<Double, Double> mannWhitney(Question q1, Question q2, List<SurveyDatum> list1, List<SurveyDatum> list2) {
        if (list1.size()==0 || list2.size()==0) {
            SurveyMan.LOGGER.warn(String.format("Cannot compare response lists of sizes: %d and %d", list1.size(), list2.size()));
            return new ImmutablePair<>(-0.0, -0.0);
        }
        // make ranks on the basis of the source row index
        Collections.sort(list1);
        Collections.sort(list2);
        double[] list1ranks = new double[list1.size()];
        double[] list2ranks = new double[list2.size()];
        for (int i = 0 ; i < list1.size() ; i++)
            list1ranks[i] = (double) list1.get(i).getSourceRow() - q1.getSourceRow() + 1;
        for (int i = 0 ; i < list2.size() ; i++)
            list2ranks[i] = (double) list2.get(i).getSourceRow() - q2.getSourceRow() + 1;
        // default constructor for mann whitney averages ties.
        MannWhitneyUTest test = new MannWhitneyUTest();
        double testStatistic = test.mannWhitneyU(list1ranks, list2ranks);
        double pvalue = test.mannWhitneyUTest(list1ranks, list2ranks);
        return new ImmutablePair<>(testStatistic, pvalue);
    }

    private static boolean validToTestCorrelation(Question q1, Question q2) {
        List<Question> questions =  q1.getVariants();
        return !questions.contains(q2) &&
                q1.exclusive && q2.exclusive &&
                !q1.freetext && !q2.freetext &&
                q1.options.size() > 0 && q2.options.size() > 0;
    }

    /**
     * Returns the total number of bins whose contents we need to be at least 5.
     * @return
     */
    public ImmutablePair<Long, Double> getSampleSize() {
        long maxSampleSize = 0;
        double p = 0.0;
        int maxWidth = 1;
        for (SurveyPath path: surveyPaths) {
//            int pathLength = path.getPathLength();
//            double p = pathLength * Math.log(0.95);
            long sampleSize = 0;
            double pp = 1.0;
            for (Question question : path.getQuestionsFromPath()) {
                if (isAnalyzable(question)) {
                    int n = question.getVariants().size();
                    if (n > maxWidth) maxWidth = n;
                    int m = question.options.size();
                    int thisSampleSize = (int) Math.ceil(5 * m * n * Math.pow(0.95, 0.2));
                    pp *= 1.0 - (CombinatoricsUtils.binomialCoefficient(thisSampleSize, 5) * Math.pow(1.0 / m, 5));
//                    p += (5.0 * Math.log(question.options.size()));
                    sampleSize += thisSampleSize;
                }
            }
//            sampleSize = (long) Math.ceil(Math.exp((p + (5.0 * Math.log(5.0))) / 5.0));
            assert sampleSize > 0 : String.format("Sample size cannot be less than 0: %d", sampleSize);
            if (sampleSize > maxSampleSize) maxSampleSize = sampleSize;
            p += pp;
        }
        return new ImmutablePair<>(maxSampleSize * surveyPaths.size() * maxWidth, Math.exp(p));
    }

    /**
     * Simulates a survey of 100% random uniform respondents over sampleSize and calculates a prior on false correlation.
     * @return Empirical false correlation.
     * @throws SurveyException
     */
    public Map<Question, Map<Question, CorrelationStruct>> getFrequenciesOfRandomCorrelation() throws SurveyException {

        Map<Question, Map<Question, CorrelationStruct>> corrs = new HashMap<>();
        List<RandomRespondent> randomRespondents = new ArrayList<>();
        int numInsufficientData = 0;
        int numComparisons = 0;

        ImmutablePair<Long, Double> pair = getSampleSize();
        long sampleSize = pair.getLeft();
        double p = pair.getRight();
        SurveyMan.LOGGER.debug(String.format("Sample size: %d; prob. of too few in any cell: %f", sampleSize, p));

        for (int i = 0 ; i < sampleSize; i++){
            randomRespondents.add(new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM));
        }

        for (int i = 0; i < survey.questions.size() - 1; i++) {
            Question q1 = survey.questions.get(i);
            for (int j = i + 1; j < survey.questions.size(); j++) {
                Question q2 = survey.questions.get(j);
                if (!validToTestCorrelation(q1, q2)) continue;
                // get responses having answered both questions
                Map<String, IQuestionResponse> q1responses = new HashMap<>();
                Map<String, IQuestionResponse> q2responses = new HashMap<>();
                for (RandomRespondent rr : randomRespondents) {

                    IQuestionResponse qr1 = null;
                    IQuestionResponse qr2 = null;

                    for (IQuestionResponse qr : rr.getResponse().getNonCustomResponses()) {
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
                if (q1responses.size()==0 && q2responses.size()==0) {
                    SurveyMan.LOGGER.warn(String.format("No one answered both questions: [%s], [%s]", q1, q2));
                    numInsufficientData++;
                    continue;
                }
                numComparisons++;
                // compute the appropriate correlation coefficient
                Map<Question, CorrelationStruct> stuff = new HashMap<>();
                if (q1.ordered && q2.ordered)
                    stuff.put(q2, new CorrelationStruct(
                            CoefficentsAndTests.RHO,
                            spearmansRho(q1responses, q2responses),
                            -0.0,
                            q1,
                            q2,
                            q1responses.size(),
                            q2responses.size()));
                else
                    stuff.put(q2, new CorrelationStruct(
                            CoefficentsAndTests.V,
                            cramersV(q1responses, q2responses),
                            -0.0,
                            q1,
                            q2,
                            q1responses.size(),
                            q2responses.size()
                    ));
                corrs.put(q1, stuff);
                // count how many p-values are below the threshhold.
            }
        }
        SurveyMan.LOGGER.info(String.format("Number of comparison made vs. number of comparisons with insufficient data: %d vs. %d", numComparisons, numInsufficientData));
        return corrs;
    }

    public boolean isFinalQuestion(Question question, SurveyResponse surveyResponse) {
        for (SurveyPath path : this.surveyPaths) {
            for (Block block : path) {
                if (block.containsQuestion(question)) {
                    int questionsInBlock = block.blockSize();
                    int questionsAnsweredFromBlock = 0;
                    for (IQuestionResponse qr: surveyResponse.getAllResponses()) {
                        if (block.containsQuestion(qr.getQuestion())) {
                            questionsAnsweredFromBlock++;
                        }
                    }
                    return questionsAnsweredFromBlock == questionsInBlock;
                }
            }
        }
        return false;
    }

    private static void labelValidity(List<CentroidCluster<SurveyResponse>> clusters) {
        // get max representative validity for each cluster and label responses according to that.
        for (CentroidCluster cluster : clusters) {
            int numValid = 0;
            int numInvalid = 0;
            int numMaybe = 0;
            for (Object point : cluster.getPoints()) {
                switch (((SurveyResponse) point).getKnownValidityStatus()) {
                    case MAYBE:
                        numMaybe++; break;
                    case YES:
                        numValid++; break;
                    case NO:
                        numInvalid++; break;
                }
            }
            int maxCt = Math.max(Math.max(numInvalid, numValid), numMaybe);
            KnownValidityStatus status = maxCt == numValid || maxCt == numMaybe ? KnownValidityStatus.YES : KnownValidityStatus.NO;
            for (Object point : cluster.getPoints()) {
                SurveyResponse sr = (SurveyResponse) point;
                sr.setComputedValidityStatus(status);
                if (status.equals(KnownValidityStatus.YES)) {
                    // Set a score so we return the proper thing later on (even though this isn't the score we use)
                    // Maybe want to make this a projection later?
                    sr.setScore(-1);
                    sr.setThreshold(1);
                } else {
                    sr.setScore(1);
                    sr.setThreshold(-1);
                }
            }
        }
    }

    private void clusterResponses(List<? extends SurveyResponse> responses) {
        int maxIterations = 50;
        HammingDistance hamming = new HammingDistance();
        KMeansPlusPlusClusterer<SurveyResponse> responseClusters = new KMeansPlusPlusClusterer<>(
                numClusters, maxIterations, hamming);
        List<CentroidCluster<SurveyResponse>> clusters = responseClusters.cluster(new ArrayList<>(responses));

        for (int i = 0; i < clusters.size(); i++) {
            CentroidCluster cluster = clusters.get(i);
            Clusterable center = cluster.getCenter();
            for (Object point : cluster.getPoints()) {
                SurveyResponse sr = (SurveyResponse) point;
                sr.center = center;
                sr.clusterLabel = "cluster_" + i;
            }
        }
        labelValidity(clusters);
    }

    private void linearlyClassifyResponses(List<? extends SurveyResponse> responses){
        // represent scores as matrices
        int n = responses.size();
        int d = survey.questions.size();
        double[][] scores = new double[d][n];
        for (int i = 0 ; i < n; i++) {
            SurveyResponse sr = responses.get(i);
            scores[i] = sr.getPoint();
        }
        // calculate means
        double[] mus = new double[d];
        for (int i = 0; i < d; i++) {
            double total = 0.0;
            for (int j = 0; j < n; j++)
                total += scores[i][j];
            mus[i] = total / d;
        }
        // create matrix of means
        double[][] mmus = new double[n][d];
        for (int i = 0; i < n; i++)
            mmus[n] = mus;

        BlockRealMatrix m = new BlockRealMatrix(scores).subtract(new BlockRealMatrix(mmus).transpose()).transpose(); // D x N
        BlockRealMatrix squareM = m.transpose().multiply(m); // N  x N
        EigenDecomposition e = new EigenDecomposition(squareM);
        RealVector firstEigenVector = e.getEigenvector(0); // N x 1
        double[][] reduced = new double[1][n]; // 1 x N
        reduced[0] = firstEigenVector.toArray(); // 1 x N
        // am i not *Actually* interested in the lowest variance in the data?
        BlockRealMatrix reducedData = m.multiply(new BlockRealMatrix(reduced).transpose()); // D x 1
        // use the learned basis vectors to find a partition
        //TODO(etosch): finish this.
        throw new RuntimeException("Linear classifier not implemented");
    }

    /**
     * Classifies the input responses according to the classifier. The DynamicSurveyResponse objects will hold the
     * computed classification, and the method will return a classification structure for easy printing and jsonizing.
     * @param responses The list of actual or simulated responses to the survey.
     * @param classifier The enum corresponding to the classifier type.
     * @return A ClassifiedRespondentsStruct object containing all of the values just computed.
     * @throws SurveyException
     */
    public ClassifiedRespondentsStruct classifyResponses(List<? extends SurveyResponse> responses, Classifier classifier) throws SurveyException {
        double start = System.currentTimeMillis();
        ClassifiedRespondentsStruct classificationStructs = new ClassifiedRespondentsStruct();
        int numValid = 0;
        int numInvalid = 0;
        double validMin = Double.POSITIVE_INFINITY;
        double validMax = Double.NEGATIVE_INFINITY;
        double invalidMin =  Double.POSITIVE_INFINITY;
        double invalidMax = Double.NEGATIVE_INFINITY;

        if (classifier.equals(Classifier.CLUSTER)) {
            // For cluster, we inject enough responses to ensure that there are at least 10% bots
            int numBotsToInject = (int) Math.floor((0.1 / 0.9) * responses.size());
            SurveyMan.LOGGER.info(String.format("Injecting %d uniform random bad actors", numBotsToInject));
            // Need a temporary list to widen the type.
            List<SurveyResponse> tmpList = new ArrayList<>(responses);
            // Add the random respondents with known validity statuses.
            while (numBotsToInject > 0) {
                RandomRespondent rr = new RandomRespondent(this.survey, RandomRespondent.AdversaryType.UNIFORM);
                tmpList.add(rr.getResponse());
                numBotsToInject--;
            }
            clusterResponses(tmpList);
            tmpList.clear();
            // Only add true responses, not our injected ones.
            for (SurveyResponse sr : responses) {
                classificationStructs.add(new ClassificationStruct(sr, Classifier.CLUSTER));
            }
            return classificationStructs;
        } else if (classifier.equals(Classifier.LINEAR)) {
            linearlyClassifyResponses(responses);
        } else if (classifier.equals(Classifier.LPO)) {
            lpoClassification(responses, 0.5);
            for (SurveyResponse sr : responses)
                classificationStructs.add(new ClassificationStruct(sr, Classifier.LPO));
            return classificationStructs;
        } else if (classifier.equals(Classifier.STACKED)) {
            lpoClassification(responses, 0.5);
            clusterResponses(responses);
            for (SurveyResponse sr : responses)
                classificationStructs.add(new ClassificationStruct(sr, Classifier.STACKED));
            return classificationStructs;
        }

        for (int i = 0; i < responses.size(); i++) {

            if (i % 25 == 0)
                SurveyMan.LOGGER.info(String.format("Classified %d responses (%d valid [%f, %f], %d invalid [%f, %f]) " +
                        "using %s policy."
                        , i, numValid, validMin, validMax, numInvalid, invalidMin, invalidMax, classifier.name()));

            SurveyResponse sr = responses.get(i);
            boolean valid;

            switch (classifier) {
                case ENTROPY:
                    valid = entropyClassification(sr, responses, alpha);
                    if (valid)
                        numValid++;
                    else numInvalid++;
                    break;
                case LOG_LIKELIHOOD:
                    valid = logLikelihoodClassification(sr, responses, alpha);
                    if (valid)
                        numValid++;
                    else numInvalid++;
                    break;
                case ALL:
                    valid = true;
                    numValid++;
                    break;
                default:
                    throw new RuntimeException("Unknown classification policy: " + classifier);
            }

            if (valid) {
                if (validMin > sr.getScore())
                    validMin = sr.getScore();
                if (validMax < sr.getScore())
                    validMax = sr.getScore();
            } else {
                if (invalidMin > sr.getScore())
                    invalidMin = sr.getScore();
                if (invalidMax < sr.getScore())
                    invalidMax = sr.getScore();
            }
            sr.setComputedValidityStatus(valid ? KnownValidityStatus.YES : KnownValidityStatus.NO);
            classificationStructs.add(new ClassificationStruct(sr, classifier));
        }
        double end = System.currentTimeMillis();
        double totalSeconds = (end - start) / 1000;
        double totalMins = totalSeconds / 60;
        SurveyMan.LOGGER.info(String.format("Finished classifying %d responses in %6.0fm%2.0fs",
                responses.size(), totalMins, totalSeconds - (totalMins * 60)));
        return classificationStructs;
    }
}

package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.AnswerFrequencyMap;
import edu.umass.cs.surveyman.qc.AnswerProbabilityMap;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.*;

public abstract class AbstractClassifier {

    /**
     * The Exception class for QCMetrics.
     */
    protected static class ClassifierException extends SurveyException {
        public ClassifierException(String msg) {
            super(msg);
        }
    }


    /**
     * The number of bootstrapIterations. Default is 500 (previously 2000, but that consumes *lots* of resources).
     */
    public int bootstrapIterations = 500;

    public final double alpha;

    protected Map<Set<Question>, List<List<SurveyResponse>>> cache = new HashMap<>();
    protected Map<List<List<SurveyResponse>>, List<Double>> means = new HashMap<>();
    protected AnswerProbabilityMap answerProbabilityMap;
    protected AnswerFrequencyMap answerFrequencyMap;
    protected final boolean smoothing;
    protected final int numClusters;
    protected final Survey survey;

    public AbstractClassifier(Survey survey, boolean smoothing, double alpha, int numClusters) {
        this.survey = survey;
        this.smoothing = smoothing;
        this.alpha = alpha;
        this.numClusters = numClusters;
    }

    public AbstractClassifier(Survey survey, List<? extends SurveyResponse> responses, boolean smoothing, double alpha, int numClusters) {
        this.survey = survey;
        this.smoothing = smoothing;
        this.alpha = alpha;
        this.numClusters = numClusters;
        makeProbabilities(responses);
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
                if (!QCMetrics.isAnalyzable(question)) continue;
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
                        HashMap<String, Integer> tmp = new HashMap<>();
                        tmp.put(c.getId(), 0);
                        answerFrequencyMap.put(q.id, tmp);
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

    /**
     * Populates the empirical probabilities of the responses.
     */
    public void makeProbabilities(List<? extends SurveyResponse> responses) {
        this.makeFrequencies(responses);
        this.makeProbabilities();
    }


    /**
     * Compares two survey responses, <em>base</em> and <em>taget</em>, such that if |<em>base</em>| < |<em>taget</em>|,
     * we return only return the set of questions in <em>target</em> that correspond to those in <em>base</em>. If
     * <em>base</em> is not a subset of <em>targat</em>, we return an empty list.
     *
     * @param base   The response we want to classify.
     * @param target The response we want to compare with the response we want to classify.
     * @return An empty list if <em>base</em> is not a subset of <em>target</em>. Otherwise, the set of questions in
     * <em>target</em> that <em>base</em> answered.
     * @throws SurveyException
     */
    public static List<IQuestionResponse> getResponseSubset(SurveyResponse base, SurveyResponse target) throws SurveyException {
        // These will be used to generate the return value.
        List<IQuestionResponse> responses = new ArrayList<>();

        // For each question in our base response, check whether the target has answered that question or one of its
        // variants.
        for (IQuestionResponse qr : base.getAllResponses()) {
            Question question = qr.getQuestion();
            if (!QCMetrics.isAnalyzable(question))
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
     *
     * @param responses The list of actual or simulated responses to the survey.
     * @return The bootstrapped sample of possible survey responses.
     */
    public List<List<SurveyResponse>> generateBootstrapSample(List<? extends SurveyResponse> responses) {
        List<List<SurveyResponse>> retval = new ArrayList<>();
        for (int i = 0; i < bootstrapIterations; i++) {
            List<SurveyResponse> sample = new ArrayList<>();
            for (int j = 0; j < responses.size(); j++) {
                sample.add(responses.get(QCMetrics.rng.nextInt(responses.size())));
            }
            retval.add(sample);
        }
        return retval;
    }

    /**
     * Abstract method that each classifier needs to implement.
     *
     * @param responses The question responses that comprise this survey response.
     * @return The response score.
     */
    public abstract double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException;

    /**
     * Abstract method that each classifier needs to implement. This may be identical to the other getScoreForResponse,
     * or it may implement a filter on the argument's responses.
     *
     * @param surveyResponse The survey response to score.
     * @return The score for the provided survey response.
     */
    public abstract double getScoreForResponse(SurveyResponse surveyResponse) throws SurveyException;

    public abstract void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException;

    public abstract boolean classifyResponse(SurveyResponse response) throws SurveyException;

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

    protected List<Double> cacheMeans(SurveyResponse sr, List<? extends SurveyResponse> responses) throws SurveyException {

        List<Double> retval = new ArrayList<>();
        List<List<SurveyResponse>> bsSample = cachedQuestionSet(sr, responses);
        if (means.containsKey(bsSample))
            return means.get(bsSample);


        for (List<? extends SurveyResponse> sample : bsSample) {
            double total = 0.0;
            for (SurveyResponse surveyResponse : sample) {
                total += getScoreForResponse(getResponseSubset(sr, surveyResponse));
            }
            retval.add(total / sample.size());
        }
        assert retval.size() == bsSample.size();
        Collections.sort(retval);
        assert retval.get(0) < retval.get(retval.size() - 1) :
                String.format("Ranked means expected mean at position 0 to be greater than the mean at %d (%f < %f).",
                        retval.size(), retval.get(0), retval.get(retval.size() - 1));
        means.put(bsSample, retval);
        return retval;
    }

    protected List<Double> computeMeans(SurveyResponse sr, List<? extends SurveyResponse> responses) throws SurveyException {

        List<Double> retval = new ArrayList<>();
        List<List<SurveyResponse>> bsSample = generateBootstrapSample(responses);

        for (List<? extends SurveyResponse> sample : bsSample) {
            double total = 0.0;
            for (SurveyResponse surveyResponse : sample) {
                total += getScoreForResponse(getResponseSubset(sr, surveyResponse));
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

    public List<SurveyResponse> injectRandomRespondents(List<? extends SurveyResponse> responses) throws SurveyException {
        // For cluster, we inject enough responses to ensure that there are at least 10% bots
        int numBotsToInject = (int) Math.floor((0.1 / 0.9) * responses.size());
        SurveyMan.LOGGER.info(String.format("Injecting %d uniform random bad actors", numBotsToInject));
        // Need a temporary list to widen the type.
        List<SurveyResponse> tmpList = new ArrayList<>(responses);
        Survey survey = responses.get(0).getSurvey();
        // Add the random respondents with known validity statuses.
        while (numBotsToInject > 0) {
            RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
            tmpList.add(rr.getResponse());
            numBotsToInject--;
        }
        return tmpList;
    }

}

package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.*;
import edu.umass.cs.surveyman.output.*;
import edu.umass.cs.surveyman.qc.classifiers.AbstractClassifier;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.MersenneRandom;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.util.CombinatoricsUtils;

import java.util.*;

public class QCMetrics {

    /**
     * The random number generator. This may be used by other classes.
     */
    public static final MersenneRandom rng = new MersenneRandom();

    /**
     * Convenience method for base-2 logs.
     * @param p
     * @return
     */
    public static double log2(double p) {
        if (p == 0)
            return 0.0;
        return Math.log(p) / Math.log(2.0);
    }

    private SurveyDAG surveyDAG;
    private List<SurveyPath> surveyPaths;
    private ImmutablePair sampleSize;

    /**
     * The survey associated with this QCMetrics object.
     */
    public final Survey survey;

    /**
     * The classifier associated with this survey.
     */
    public final AbstractClassifier classifier;

    private static Set<Question> notAnalyzable = new HashSet<>();

    /**
     * Constructor for the QCMetrics instance.
     * @param survey The survey for which we want to compute metrics.
     * @param classifier The classifier we want to use for respondents.
     */
    public QCMetrics(Survey survey, AbstractClassifier classifier) {
        this.surveyDAG = SurveyDAG.getDag(survey);
        this.surveyPaths = SurveyDAG.getPaths(survey);
        this.survey = survey;
        this.classifier = classifier;
        // Compute the sample size we need to figure out false correlation
        long maxSampleSize = 0;
        // double p = 0.0;
        int maxWidth = 1;
        for (SurveyPath path: this.surveyPaths) {
            // int pathLength = path.getPathLength();
            // double p = pathLength * Math.log(0.95);
            long sampleSize = 0;
            // double pp = 1.0;
            for (Question question : path.getQuestionsFromPath()) {
                if (isAnalyzable(question)) {
                    int n = question.getVariants().size();
                    if (n > maxWidth) maxWidth = n;
                    int m = question.options.size();
                    int thisSampleSize = (int) Math.ceil(5 * m * n * Math.pow(0.95, 0.2));
                    // pp *= 1.0 - (CombinatoricsUtils.binomialCoefficient(thisSampleSize, 5) * Math.pow(1.0 / m, 5));
                    // p += (5.0 * Math.log(question.options.size()));
                    sampleSize += thisSampleSize;
                }
            }
            // sampleSize = (long) Math.ceil(Math.exp((p + (5.0 * Math.log(5.0))) / 5.0));
            assert sampleSize > 0 : String.format("Sample size cannot be less than 0: %d", sampleSize);
            if (sampleSize > maxSampleSize) maxSampleSize = sampleSize;
            // p += pp;
        }
        // this.sampleSize = new ImmutablePair<>(maxSampleSize * surveyPaths.size() * maxWidth, Math.exp(p));
        this.sampleSize = new ImmutablePair(maxSampleSize * surveyPaths.size() * maxWidth, null);
    }

    /**
     * Computes the maximum path length through the survey.
     * @return The length of the longest path through the survey.
     */
    public int maximumPathLength() {
        return surveyDAG.maximumPathLength();
    }

    private static boolean alreadyWarned(Question question) {
        return notAnalyzable.contains(question);
    }

    /**
     * Test indicating whether the provided question can be analyzed using our metrics. Currently, only checkbox and
     * radio button questions can be analyzed (freetext and custom questions cannot).
     * @param question The question we may want to analyze.
     * @return Boolean indicating whether we can use our QCMetrics object to analyze this question.
     */
    public static boolean isAnalyzable(Question question) {
        // freetext will be null if we've designed the survey programmatically and have
        // not added any questions (i.e. if it's an instructional question).
        if (question.freetext == null)
            question.freetext = false;
        boolean analyzable = !question.freetext && !question.isInstructional() && !question.isCustomQuestion();
        if (!analyzable && !alreadyWarned(question)) {
            SurveyMan.LOGGER.debug(String.format("Skipping question [%s]: not analysable.", question.toString()));
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

    /**
     * Computes the minimum path through the survey.
     * @return The minimum path length through the survey.
     */
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

    protected static double cellExpectation(int[][] contingencyTable, int i, int j, int n) {
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
    public static double chiSquared(int[][] contingencyTable, Object[] categoryA, Object[] categoryB) {
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
        return this.sampleSize;
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
        // double p = pair.getRight();
        SurveyMan.LOGGER.debug(String.format("Sample size: %d; prob. of too few in any cell: %f", sampleSize, 0.0));

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
                return corrs; //UNCOMMENT ME
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

    /**
     * Classifies the input responses according to the classifier. The DynamicSurveyResponse objects will hold the
     * computed classification, and the method will return a classification structure for easy printing and jsonizing.
     * @param responses The list of actual or simulated responses to the survey.
     * @return A ClassifiedRespondentsStruct object containing all of the values just computed.
     * @throws SurveyException
     */
    public ClassifiedRespondentsStruct classifyResponses(List<? extends SurveyResponse> responses) throws SurveyException {
        double start = System.currentTimeMillis();
        ClassifiedRespondentsStruct classificationStructs = new ClassifiedRespondentsStruct();
        this.classifier.computeScoresForResponses(responses);
        for (SurveyResponse sr : responses) {
            boolean isValid = this.classifier.classifyResponse(sr);
            sr.setComputedValidityStatus(isValid ? KnownValidityStatus.YES : KnownValidityStatus.NO);
            classificationStructs.add(new ClassificationStruct(sr, classifier));
        }
        double end = System.currentTimeMillis();
        SurveyMan.LOGGER.info(String.format("Classified %d responses in %ds", responses.size(), (int) Math.ceil((end - start) / 1000)));
        return classificationStructs;
    }

}

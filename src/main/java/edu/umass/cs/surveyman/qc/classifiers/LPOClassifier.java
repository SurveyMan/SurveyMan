package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.*;

public class LPOClassifier extends AbstractClassifier {

    public LPOClassifier(Survey survey, boolean smoothing, double alpha, int numClusters) {
        super(survey, smoothing, alpha, numClusters);
    }

    public LPOClassifier(Survey survey, List<? extends SurveyResponse> responses, boolean smoothing, double alpha, int numClusters) {
        super(survey, responses, smoothing, alpha, numClusters);
    }

    /**
     * A tunable parameter for defining the least popular option.
     *  <ol>
     *   <li>Sorts the answers according to frequency.</li>
     *   <li>Working backwards from the most frequent response, selects the set of least popular
     *   responses after the first multiplicative difference of size <i>epsilon</i></li>
     *  </ol>
     */
    public double epsilon = 0.5;
    /**
     * The expected number of least popular options for the full survey
     */
    private double mu = 0.0;
    private double threshold = 0;
    private double delta = 0.5;
    private double alpha = 0.05;
    private double percentage;

    protected Map<Question, List<SurveyDatum>> lpos = null;

    protected void makeLPOs() throws SurveyException {

        if (answerProbabilityMap == null) {
            throw new RuntimeException("Must populate probability map before running this.");
        }

        lpos = new HashMap<>();

        for (Question q: survey.getQuestionListByIndex()) {

            if (!answerProbabilityMap.containsKey(q.id))
                continue;

            Set<SurveyDatum> theseLPOs = new HashSet<>();

            // Get the frequency counts for this question
            Map<String, Integer> cmap = new HashMap<>(answerFrequencyMap.get(q.id));

            // If there are responses no one answered, then by definition this is an LPO
            if (q.options.values().size() > cmap.size()) {
                for (SurveyDatum option : q.options.values()) {
                    if (!cmap.containsKey(option.getId())) {
                        theseLPOs.add(option);
                    }
                }
            }

            // Create a sorted list of entry sets
            List<Map.Entry<String, Integer>> lst = new ArrayList<>();
            for (Map.Entry<String, Integer> e : cmap.entrySet()) {
                if (lst.isEmpty()) {
                    lst.add(e);
                } else {
                    // Find the point where we need to add this value.
                    boolean added = false;
                    for (int i = 0; i < lst.size() ; i++) {
                        if (lst.get(i).getValue() >= e.getValue()) {
                            lst.add(i, e);
                            added = true;
                            break;
                        }
                    }
                    if (!added) {
                        lst.add(e);
                    }
                }
            }

            if (lst.size() > 1)
                // If we have at least two items, make sure the order is correct
                assert lst.get(0).getValue() <= lst.get(lst.size() - 1).getValue();
            // Otherwise, skip computing LPO for this question (won't be useful in classification)
            else continue;

            // For each count, check to see if we have an inflection point of the desired size (this will be the cutoff
            // for determining whether this option belongs in the LPO set. Start by computing the deltas between counts
            int[] deltas = new int[lst.size() - 1];
            for (int i = 1; i < lst.size(); i++) {
                deltas[i-1] = lst.get(i).getValue() - lst.get(i-1).getValue();
            }
            // Find the max inflection point.
            int inflectionPoint = 0;
            for (int i = 0; i < deltas.length; i++) {
                if (deltas[i] > inflectionPoint)
                    inflectionPoint = i;
            }
            // If the max inflection point is greater than epsilon times the inflection point to its right, then we have
            // an LPO set.
            if (inflectionPoint > 0 && deltas[inflectionPoint] * epsilon > deltas[inflectionPoint - 1]) {
                for (int i = 0; i <= inflectionPoint; i++) {
                    theseLPOs.add(q.getOptById(lst.get(i).getKey()));
                }
            }

            if (theseLPOs.size() == cmap.size() || theseLPOs.isEmpty())
                continue;

            lpos.put(q, new ArrayList<>(theseLPOs));
        }
    }

    public void setParams() {
        for (Question q : survey.questions) {
            if (lpos.containsKey(q))
                mu += lpos.get(q).size() / (1.0 * q.options.size());
        }
        // The count we should not be exceeding.
        this.threshold = (1 + delta) * mu;
        // We will need to scale for incomplete surveys.
        // Note that some paths may
        this.percentage = this.threshold / (survey.questions.size() * 1.0);
        // P(X >= (1 + delta)*mu) <= exp(-delta^2*mu / 2 + delta)
        // i.e., the probability that X (the number of least popular options) is greater than an upper bound on the
        // expected number of least popular options due to chance needs to be sufficiently small
        this.alpha = Math.exp((-Math.pow(delta, 2.0) * mu) / (2.0 + delta));
        if (this.alpha < 0.5) {
            // Then this classifier is no better than random (which is worse than our known baseline, since we know that
            // the population is unlikely to be 50% bad actors)
            while (this.delta < 1.0 && this.alpha > 0.5) {
                this.delta += 0.05;
                this.alpha = Math.exp((-Math.pow(delta, 2) * mu) / (2.0 + delta));
            }
        }
    }

    /**
     * Computes the validity of the input responses, based on the "Least popular option" metric.
     * @param responses The survey respondents' responses.
     * @throws SurveyException
     */
    public void lpoClassification(List<? extends SurveyResponse> responses) throws SurveyException {
        for (SurveyResponse sr : responses) {
            double ct = getScoreForResponse(sr);
            sr.setThreshold(percentage * sr.resultsAsMap().size());
            sr.setScore(ct);
        }
    }

    @Override
    public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
        double ct = 0;
        for (IQuestionResponse questionResponse : responses) {
            Question q = questionResponse.getQuestion();
            if (lpos.containsKey(q)) {
                List<SurveyDatum> theseLPOs = lpos.get(questionResponse.getQuestion());
                if ((q.exclusive && theseLPOs.contains(questionResponse.getAnswer())) ||
                        (!q.exclusive && theseLPOs.containsAll(questionResponse.getAnswers())))
                    ct += 1;
            }
        }
        return ct;
    }

    /**
     * This score is the number of least popular options chosen.
     * @param surveyResponse The survey response to score.
     * @return A double representing the count of the least popular options chosen.
     * @throws SurveyException
     */
    @Override
    public double getScoreForResponse(SurveyResponse surveyResponse) throws SurveyException {
        return getScoreForResponse(surveyResponse.getAllResponses());
    }

    @Override
    public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {
        this.makeProbabilities(responses);
        makeLPOs();
        setParams();
        lpoClassification(responses);
    }

    /**
     * Returns true if the number of least popular options is below the appropriate threshold.
     * @param response  The response we want to classify.
     * @return true if the response is valid, false if not.
     * @throws SurveyException
     */
    @Override
    public boolean classifyResponse(SurveyResponse response) throws SurveyException {
        assert response.getThreshold() < Double.POSITIVE_INFINITY;
        return response.getScore() < response.getThreshold();
    }

}

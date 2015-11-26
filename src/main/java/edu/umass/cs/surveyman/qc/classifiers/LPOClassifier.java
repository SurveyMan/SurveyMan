package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
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

    /**
     * A tunable parameter for defining the least popular option.
     *  <ol>
     *   <li>Sorts the answers according to frequency.</li>
     *   <li>Working backwards from the most frequent response, selects the set of least popular
     *   responses after the first multiplicative difference of size <i>epsilon</i></li>
     *  </ol>
     */
    public double epsilon = 0.5;

    private double delta = 0.5;
    private double mu = 0.0;
    private double threshold;
    private double percentage;

    private Map<Question, List<SurveyDatum>> lpos = null;

    public void makeLPOs() throws SurveyException {

        if (answerProbabilityMap == null) {
            throw new RuntimeException("Must populate probability map before running this.");
        }

        lpos = new HashMap<>();

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
    }

    public void setParams() {
        for (Question q : survey.questions) {
            if (lpos.containsKey(q))
                mu += lpos.get(q).size() / (1.0 * q.options.size());
        }
        this.threshold = (1 - delta) * mu;
        this.percentage = threshold / lpos.size();
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

    @Override
    public boolean classifyResponse(SurveyResponse response) throws SurveyException {
        return response.getScore() > response.getThreshold();
    }

}

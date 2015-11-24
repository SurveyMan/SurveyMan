package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.*;

/**
 * Created by etosch on 11/23/15.
 */
public class LPOClassifier extends AbstractClassifier {

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

} else if (classifier.equals(Classifier.LPO)) {
        lpoClassification(responses, 0.5);
        for (SurveyResponse sr : responses)
        classificationStructs.add(new ClassificationStruct(sr, Classifier.LPO));
        return classificationStructs;

}

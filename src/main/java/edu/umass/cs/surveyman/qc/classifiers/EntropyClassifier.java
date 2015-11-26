package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class EntropyClassifier extends AbstractClassifier {

    public EntropyClassifier(Survey survey, boolean smoothing, double alpha, int numClusters) {
        super(survey, smoothing, alpha, numClusters);
    }

    public EntropyClassifier(Survey survey) {
        this(survey, false, 0.05, 2);
    }

    private List<Double> calculateEntropies(SurveyResponse base, List<? extends SurveyResponse> responses) throws SurveyException {
        this.makeProbabilities(responses);
        List<Double> retval = new LinkedList<>();
        for (SurveyResponse sr : responses) {
            retval.add(getScoreForResponse(getResponseSubset(base, sr)));
        }
        return retval;
    }

    @Override
    public double getScoreForResponse(SurveyResponse surveyResponse) throws SurveyException {
        return getScoreForResponse(surveyResponse.getAllResponses());
    }

    @Override
    public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
        if (this.answerProbabilityMap == null) {
            throw new ClassifierException("Cannot compute the log likelihood of a response without computing the empirical distribution of responses.");
        }
        double ent = 0.0;
        for (IQuestionResponse questionResponse : responses) {
            Question question = questionResponse.getQuestion();
            if (!QCMetrics.isAnalyzable(question)) continue;
            String qid = question.id;
            for (String cid : OptTuple.getCids(questionResponse.getOpts())) {
                double p = this.answerProbabilityMap.get(qid).get(cid);
                assert p > 0.0;
                ent += p * QCMetrics.log2(p);
            }
        }
        return -ent;
    }

    @Override
    public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {
        if (answerProbabilityMap == null) {
            makeProbabilities(responses);
        }
        // basically the same as logLikelihood, but scores are p * log p, rather than straight up p
        for (SurveyResponse sr : responses) {

            List<Double> ents = calculateEntropies(sr, responses);
            Set<Double> scoreSet = new HashSet<>(ents);
            if (scoreSet.size() > 5) {
                double thisEnt = getScoreForResponse(sr);
                List<Double> means = cacheMeans(sr, responses);
                double threshHold = means.get((int) Math.ceil(alpha * means.size()));
                sr.setThreshold(threshHold);
                sr.setScore(thisEnt);
            } else {
                SurveyMan.LOGGER.debug(String.format("Not enough samples to compute a distribution: %d", scoreSet.size()));
            }
        }
    }

    @Override
    public boolean classifyResponse(SurveyResponse response) {
        return response.getScore() < response.getThreshold();
    }

}

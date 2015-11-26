package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
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

public class LogLikelihoodClassifier extends AbstractClassifier {

    public LogLikelihoodClassifier(Survey survey, boolean smoothing, double alpha, int numClusters) {
        super(survey, smoothing, alpha, numClusters);
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
            retval.add(getScoreForResponse(questionResponses));
        }
        return retval;
    }

    @Override
    public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
        if (this.answerProbabilityMap == null) {
            throw new ClassifierException("Cannot compute the log likelihood of a response without computing the empirical distribution of responses.");
        }
        double ll = 0.0;
        for (IQuestionResponse questionResponse : responses) {
            Question question = questionResponse.getQuestion();
            if (!QCMetrics.isAnalyzable(question)) continue;
            String qid = question.id;
            for (String cid : OptTuple.getCids(questionResponse.getOpts())) {
                ll += Math.log(this.answerProbabilityMap.get(qid).get(cid));
            }
        }
        return ll;
    }

    @Override
    public double getScoreForResponse(SurveyResponse surveyResponse) throws SurveyException {
        return getScoreForResponse(surveyResponse.getAllResponses());
    }

    @Override
    public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {
        if (answerProbabilityMap == null) {
            makeProbabilities(responses);
        }

        for (SurveyResponse sr : responses) {
            List<Double> lls = calculateLogLikelihoods(sr, responses);
            Set<Double> llSet = new HashSet<>(lls);

            if (llSet.size() > 5) {

                double thisLL = getScoreForResponse(sr.getNonCustomResponses());
                List<Double> means = cacheMeans(sr, responses);
                //SurveyMan.LOGGER.info(String.format("Range of means: [%f, %f]", means.get(0), means.get(means.size() -1)));
                double threshHold = means.get((int) Math.floor(alpha * means.size()));
                //SurveyMan.LOGGER.info(String.format("Threshold: %f\tLL: %f", threshHold, thisLL));
                sr.setScore(thisLL);
                sr.setThreshold(threshHold);
            } else {
                SurveyMan.LOGGER.debug(String.format("Not enough samples to compute a distribution: %d", llSet.size()));
            }
        }
    }

    @Override
    public boolean classifyResponse(SurveyResponse response) {
        return response.getScore() > response.getThreshold();
    }

}

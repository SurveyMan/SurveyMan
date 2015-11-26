package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MahalanobisClassifier extends AbstractClassifier {

    private double threshold;
    private RealVector means;
    private RealMatrix covarianceMatrix;
    private Map<ImmutablePair<Question, Question>, Map<ImmutablePair<Double, Double>, Integer>> jointEmpiricalFreq;
    private Map<ImmutablePair<Question, Question>, Map<ImmutablePair<Double, Double>, Double>> jointEmpiricalProb;


    public MahalanobisClassifier(Survey survey, boolean smoothing, double alpha, int numClusters) {
        super(survey, smoothing, alpha, numClusters);
    }

    private void computeJointEmpiricalProbs(List<? extends SurveyResponse> responses) throws SurveyException {
        // for every pair of questions, compute the joint probability

        // Make an entry for all possible combinations
        for (Question q1 : survey.getQuestionListByIndex()) {
            for (Question q2 : survey.getQuestionListByIndex()) {
                ImmutablePair<Question, Question> key = new ImmutablePair<>(q1, q2);
                Map<ImmutablePair<Double, Double>, Integer> val = new HashMap<>();
                for (List<OptTuple> ans1 : q1.getAllAnswerOptions()) {
                    for (List<OptTuple> ans2 : q2.getAllAnswerOptions()) {
                        double score1 = q1.responseToDouble(ans1, false);
                        double score2 = q2.responseToDouble(ans2, false);
                        val.put(new ImmutablePair<>(score1, score2), 0);
                    }
                }
                jointEmpiricalFreq.put(key, val);
            }
        }

        // Compute the frequencies
        for (SurveyResponse sr : responses) {
            List<IQuestionResponse> qrs = sr.getNonCustomResponses();
            for (IQuestionResponse qr1 : qrs) {
                for (IQuestionResponse qr2 : qrs) {
                    Question q1 = qr1.getQuestion();
                    Question q2 = qr2.getQuestion();
                    double score1 = q1.responseToDouble(qr1.getOpts(), false);
                    double score2 = q2.responseToDouble(qr2.getOpts(), false);
                    Map<ImmutablePair<Double, Double>, Integer> q1q2Freq = jointEmpiricalFreq.get(new ImmutablePair<>(q1, q2));
                    ImmutablePair<Double, Double> key = new ImmutablePair<>(score1, score2);
                    q1q2Freq.put(key, q1q2Freq.get(key) + 1);
                }
            }
        }

        // Compute the probabilities
        for (Map.Entry<ImmutablePair<Question, Question>, Map<ImmutablePair<Double, Double>, Integer>> entry : jointEmpiricalFreq.entrySet()) {
            Map<ImmutablePair<Double, Double>, Double> probs = new HashMap<>();
            int denom = 0;
            for (Integer i : entry.getValue().values()) {
                denom += i;
            }
            for (Map.Entry<ImmutablePair<Double, Double>, Integer> pmap : entry.getValue().entrySet()) {
                probs.put(pmap.getKey(), pmap.getValue() * 1.0 / denom * 1.0);
            }
            jointEmpiricalProb.put(entry.getKey(), probs);
        }

    }

    private void computeMeans(List<? extends SurveyResponse> responses) {
        for (SurveyResponse sr : responses) {
            this.means.add(new ArrayRealVector(sr.getPoint()));
        }
        means.mapDivide(responses.size());
    }

    private void computeCovarianceMatrix() throws SurveyException {
        Question[] questions = survey.getQuestionListByIndex();
        double[][] cov = new double[questions.length][questions.length];
        for (int i = 0; i < questions.length; i++) {
            Question q1 = questions[i];
            for (int j = 0; j < questions.length; j++) {
                Question q2 = questions[j];
                ImmutablePair<Question, Question> outerKey = new ImmutablePair<>(q1, q2);
                // need to compute E[(X1 - E(X1))(X2 - E(X2))]
                // let g(x1, x2) = (X1 - E(X1))(X2 - E(X2))
                // need to compute f(x1, x2) => the joint probability of each question's responses.
                double mu_q1 = this.means.getEntry(i);
                double mu_q2 = this.means.getEntry(j);
                double mu_q1_q2 = 0.0;
                for (List<OptTuple> ans1 : q1.getAllAnswerOptions()) {
                    for (List<OptTuple> ans2 : q2.getAllAnswerOptions()) {
                        double x1 = q1.responseToDouble(ans1, this.smoothing);
                        double x2 = q2.responseToDouble(ans2, this.smoothing);
                        ImmutablePair<Double, Double> innerKey = new ImmutablePair<>(x1, x2);
                        mu_q1_q2 += (x1 - mu_q1) * (x2 - mu_q2) * jointEmpiricalProb.get(outerKey).get(innerKey);
                    }
                }
                cov[i][j] = mu_q1_q2;
            }
        }
        this.covarianceMatrix = new BlockRealMatrix(cov);
    }

    @Override
    public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
        throw new RuntimeException("Consider deprecating this method.");
    }

    @Override
    public double getScoreForResponse(SurveyResponse surveyResponse) throws SurveyException {
        RealMatrix obs = new BlockRealMatrix(new double[][]{surveyResponse.getPoint()});
        RealMatrix diff = obs.subtract(new BlockRealMatrix(new double[][]{this.means.toArray()}));
        RealMatrix inner = diff.multiply(covarianceMatrix).multiply(diff.transpose());
        assert inner.getColumnDimension() == 1 : "Expecting one column";
        assert inner.getRowDimension() == 1 : "Expecting one row";
        return Math.sqrt(inner.getEntry(0,0));
    }

    @Override
    public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {
        computeMeans(responses);
        computeJointEmpiricalProbs(responses);
        computeCovarianceMatrix();
        double[] scores = new double[responses.size()];
        for (int i = 0 ; i < responses.size(); i++) {
            SurveyResponse sr = responses.get(i);
            double score = getScoreForResponse(sr);
            sr.setScore(score);
            scores[i] = score;
        }
        double mean = new ArrayRealVector(scores).getL1Norm() / scores.length;
        double sd = new ArrayRealVector(scores).mapSubtract(mean).getNorm();
        this.threshold = 3.0 * sd;
    }

    @Override
    public boolean classifyResponse(SurveyResponse response) throws SurveyException {
        // Valid if less than 3 standard deviations from the mean.
        return response.getScore() > this.threshold;
    }


}

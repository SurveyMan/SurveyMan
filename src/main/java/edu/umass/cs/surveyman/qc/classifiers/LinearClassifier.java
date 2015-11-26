package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Arrays;
import java.util.List;

public class LinearClassifier extends AbstractClassifier {

    public LinearClassifier() {
        super(null, false, 0.0, 0);
        throw new RuntimeException("Linear Classifier not yet implemented.");
    }

    private void linearlyClassifyResponses(List<? extends SurveyResponse> responses){
        Survey survey = responses.get(0).getSurvey();
        // represent scores as matrices
        int N = responses.size();
        int n = survey.questions.size();
        // N x n Matrix of responses and per-question scores.
        double[][] scores = new double[N][n];
        for (int i = 0 ; i < n; i++) {
            SurveyResponse sr = responses.get(i);
            scores[i] = sr.getPoint();
        }
        // calculate means: (scores)^T * ones(N) ./ N
        double[][] ones = new double[N][1];
        for (double[] d : ones)
            Arrays.fill(d, 1.0);

        RealMatrix xs = new BlockRealMatrix(scores); // N x n

        RealMatrix mus = xs // 1 x n
                .transpose()
                .multiply(new BlockRealMatrix(ones))
                .scalarMultiply(1.0 / N);

        // Normalize
        RealMatrix delts = xs.subtract(new BlockRealMatrix(ones).multiply(mus)); // N x n

        RealMatrix squareDelts = delts.transpose().multiply(delts); // n x n
        EigenDecomposition e = new EigenDecomposition(squareDelts);
        // Get the first principal component -- the question that provides us with the most
        RealVector firstEigenVector = e.getEigenvector(0); // n x 1
        double[][] reduced = new double[1][n]; // 1 x n
        reduced[0] = firstEigenVector.toArray(); // 1 x n
        // am i not *Actually* interested in the lowest variance in the data?
        // BlockRealMatrix reducedData = m.multiply(new BlockRealMatrix(reduced).transpose()); // D x 1
        // use the learned basis vectors to find a partition
        //TODO(etosch): finish this.
        throw new RuntimeException("Linear classifier not implemented");
    }

    @Override
    public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
        return 0;
    }

    @Override
    public double getScoreForResponse(SurveyResponse surveyResponse) throws SurveyException {
        return 0;
    }

    @Override
    public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {

    }

    @Override
    public boolean classifyResponse(SurveyResponse response) throws SurveyException {
        return false;
    }
}

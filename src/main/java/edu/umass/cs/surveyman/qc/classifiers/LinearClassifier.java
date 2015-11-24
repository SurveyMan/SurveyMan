package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.SurveyResponse;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealVector;

import java.util.List;

/**
 * Created by etosch on 11/23/15.
 */
public class LinearClassifier {


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

} else if (classifier.equals(Classifier.LINEAR)) {
        linearlyClassifyResponses(responses);

}

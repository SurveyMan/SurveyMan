package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.HammingDistance;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import java.util.ArrayList;
import java.util.List;

/**
 * This is simple cluster classifier. It injects enough uniform random respondents to get at least 10% bad actors and
 * then picks the most common label for the cluster. Maybe labels are interpreted as valid labels.
 */
public class ClusterClassifier extends AbstractClassifier {

    public ClusterClassifier(Survey survey, boolean smoothing, double alpha, int numClusters) {
        super(survey, smoothing, alpha, numClusters);
    }

    public void labelValidity(List<CentroidCluster<SurveyResponse>> clusters) {
        // get max representative validity for each cluster and label responses according to that.
        for (CentroidCluster cluster : clusters) {
            int numValid = 0;
            int numInvalid = 0;
            int numMaybe = 0;
            for (Object point : cluster.getPoints()) {
                switch (((SurveyResponse) point).getKnownValidityStatus()) {
                    case MAYBE:
                        numMaybe++; break;
                    case YES:
                        numValid++; break;
                    case NO:
                        numInvalid++; break;
                }
            }
            int maxCt = Math.max(Math.max(numInvalid, numValid), numMaybe);
            KnownValidityStatus status = maxCt == numValid || maxCt == numMaybe ? KnownValidityStatus.YES : KnownValidityStatus.NO;
            for (Object point : cluster.getPoints()) {
                SurveyResponse sr = (SurveyResponse) point;
                sr.setComputedValidityStatus(status);
            }
        }
    }

    public List<CentroidCluster<SurveyResponse>> clusterResponses(List<? extends SurveyResponse> responses) {
        int maxIterations = 50;
        HammingDistance hamming = new HammingDistance();
        KMeansPlusPlusClusterer<SurveyResponse> responseClusters = new KMeansPlusPlusClusterer<>(
                numClusters, maxIterations, hamming);
        List<CentroidCluster<SurveyResponse>> clusters = responseClusters.cluster(new ArrayList<>(responses));

        for (int i = 0; i < clusters.size(); i++) {
            CentroidCluster cluster = clusters.get(i);
            Clusterable center = cluster.getCenter();
            for (Object point : cluster.getPoints()) {
                SurveyResponse sr = (SurveyResponse) point;
                sr.center = center;
                sr.clusterLabel = "cluster_" + i;
            }
        }
        return clusters;
    }

    @Override
    public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
        throw new RuntimeException("No distance metric defined for ClusterClassifier.");
    }

    @Override
    public double getScoreForResponse(SurveyResponse response) throws SurveyException {
        // Compute the Euclidean distance
        RealVector center = new ArrayRealVector(response.center.getPoint());
        RealVector score = new ArrayRealVector(response.getPoint());
        return Math.sqrt(center.subtract(score).getNorm());
    }

    public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {
        List<SurveyResponse> tmpList = injectRandomRespondents(responses);
        List<CentroidCluster<SurveyResponse>> clusters = clusterResponses(tmpList);
        labelValidity(clusters);
        for (SurveyResponse sr : responses) {
            sr.setThreshold(0);
            sr.setScore(sr.getComputedValidityStatus().equals(KnownValidityStatus.NO) ? -1 : +1);
        }
    }

    public boolean classifyResponse(SurveyResponse surveyResponse) {
        return surveyResponse.getScore() > surveyResponse.getThreshold();
    }

}

package edu.umass.cs.surveyman.qc.classifiers;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.HammingDistance;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import java.util.ArrayList;
import java.util.List;

/**
 * This is simple cluster classifier. It injects enough uniform random respondents to get at least 10% bad actors and
 * then picks the most common label for the cluster. Maybe lables are interpreted as valid labels.
 */
public class ClusterClassifier extends AbstractClassifier {

    private static void labelValidity(List<CentroidCluster<SurveyResponse>> clusters) {
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
                sr.setThreshold(0);
                sr.setScore(status.equals(KnownValidityStatus.NO) ? -1 : +1);
            }
        }
    }

    private void clusterResponses(List<? extends SurveyResponse> responses) {
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
        labelValidity(clusters);
    }

    @Override
    public double getScoreForResponse(List<IQuestionResponse> responses) throws SurveyException {
        throw new RuntimeException("No distance metric defined for ClusterClassifier.");
    }

    @Override
    public double getScoreForResponse(SurveyResponse response) throws SurveyException {
        throw new RuntimeException("No distance metric defined for ClusterClassifier.");
    }

    public void computeScoresForResponses(List<? extends SurveyResponse> responses) throws SurveyException {
        // For cluster, we inject enough responses to ensure that there are at least 10% bots
        int numBotsToInject = (int) Math.floor((0.1 / 0.9) * responses.size());
        SurveyMan.LOGGER.info(String.format("Injecting %d uniform random bad actors", numBotsToInject));
        // Need a temporary list to widen the type.
        List<SurveyResponse> tmpList = new ArrayList<>(responses);
        Survey survey = responses.get(0).getSurvey();
        // Add the random respondents with known validity statuses.
        while (numBotsToInject > 0) {
            RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
            tmpList.add(rr.getResponse());
            numBotsToInject--;
        }
        clusterResponses(tmpList);
        tmpList.clear();
    }

    public boolean classifyResponse(SurveyResponse surveyResponse) {
        return surveyResponse.getScore() > surveyResponse.getThreshold();
    }

}

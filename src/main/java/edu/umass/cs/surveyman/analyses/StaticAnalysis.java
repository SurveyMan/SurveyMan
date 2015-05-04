package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.output.CorrelationStruct;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StaticAnalysis {

    public static class Report {

        public final String surveyName;
        public final String surveyId;
        public final double avgPathLength;
        public final double maxPossibleEntropy;
        public final int maxPathLength;
        public final int minPathLength;
        public final Map<Question, Map<Question, CorrelationStruct>> frequenciesOfRandomCorrelations;
        public final List<Simulation.ROC> rocList;

        Report(String surveyName,
               String surveyId,
               int minPathLength,
               int maxPathLength,
               double avgPathLength,
               double maxPossibleEntropy,
               Map<Question, Map<Question, CorrelationStruct>> frequenciesOfRandomCorrelations,
               List<Simulation.ROC> rocList) {
            this.surveyName = surveyName;
            this.surveyId = surveyId;
            this.avgPathLength = avgPathLength;
            this.maxPossibleEntropy = maxPossibleEntropy;
            this.maxPathLength = maxPathLength;
            this.minPathLength = minPathLength;
            this.frequenciesOfRandomCorrelations = frequenciesOfRandomCorrelations;
            this.rocList = rocList;
        }

        private double getFrequencyOfRandomCorrelation() {
            double strongThreshhold = 0.8;
            int ctAboveString = 0;
            int numComparisons = 0;
            for (Map<Question, CorrelationStruct> entry : this.frequenciesOfRandomCorrelations.values()) {
                for (CorrelationStruct correlationStruct : entry.values()) {
                    numComparisons++;
                    if (correlationStruct.coefficientValue > strongThreshhold) {
                        ctAboveString++;
                    }
                }
            }
            return ctAboveString / (1.0 * numComparisons);
        }

        public void print(OutputStream stream) {
            OutputStreamWriter osw = new OutputStreamWriter(stream);
            try {
                osw.write(String.format(
                        "Min Path Length:\t%d\n" +
                        "Max Path Length:\t%d\n" +
                        "Average Path Length:\t%f\n" +
                        "Max Possible Entropy:\t%f\n" +
                        "Prob. False Correlation:\t%f\n",
                        this.minPathLength,
                        this.maxPathLength,
                        this.avgPathLength,
                        this.maxPossibleEntropy,
                        this.getFrequencyOfRandomCorrelation()
                ));
                osw.write("percentBots,entropy,TP,FP,TN,FN\n");
                for (Simulation.ROC roc : rocList)
                    osw.write(roc.toString());
                osw.close();
            } catch (IOException e) {
                SurveyMan.LOGGER.warn(e);
            }
        }

        public String jsonizeBadActors() {
            StringBuilder json = new StringBuilder();
            List<Double> percBots = new ArrayList<Double>();
            List<Double> empiricalEntropy = new ArrayList<Double>();
            List<Integer> truePositives = new ArrayList<Integer>();
            List<Integer> falsePositives = new ArrayList<Integer>();
            List<Integer> trueNegatives = new ArrayList<Integer>();
            List<Integer> falseNegatives = new ArrayList<Integer>();
            for (Simulation.ROC roc : rocList) {
                percBots.add(roc.percBots);
                empiricalEntropy.add(roc.empiricalEntropy);
                trueNegatives.add(roc.trueNegative);
                truePositives.add(roc.truePositive);
                falseNegatives.add(roc.falseNegative);
                falsePositives.add(roc.falsePositive);
            }
            json.append(String.format(
                    "{" +
                            "\"percbots\" : [ %s ]," +
                            "\"empiricalentropy\" : [ %s ]," +
                            "\"truepositives\" : [ %s ]," +
                            "\"falsepositives\" : [ %s ]," +
                            "\"truenegatives\" : [ %s ]," +
                            "\"falsenegatives\" : [ %s ]" +
                            "}",
                    StringUtils.join(percBots, ","),
                    StringUtils.join(empiricalEntropy, ","),
                    StringUtils.join(truePositives, ","),
                    StringUtils.join(falsePositives, ","),
                    StringUtils.join(trueNegatives, ","),
                    StringUtils.join(falseNegatives, ",")
                )
            );
            return String.format("{ %s }", json);
        }

        public String jsonize() {
            String json = String.format(
                    "{" +
                            "\"surveyname\" : \"%s\", " +
                            "\"minpathlength\" : %d," +
                            "\"maxpathlength\" : %d," +
                            "\"avgpathlength\" : %f," +
                            "\"maxpossibleentropy\" : %f," +
                            "\"probfalsecorr\" : %f," +
                            "\"badactors : %s \"" +
                    "}",
                    this.surveyName,
//                    this.surveyId,
                    this.minPathLength,
                    this.maxPathLength,
                    this.avgPathLength,
                    this.maxPossibleEntropy,
                    this.getFrequencyOfRandomCorrelation(),
                    this.jsonizeBadActors()
                    );
            return json;
        }
    }

    public static void wellFormednessChecks(Survey survey) throws SurveyException{
        SurveyMan.LOGGER.info(String.format("Testing %d rules...", AbstractRule.getRules().size()));
        for (AbstractRule rule : AbstractRule.getRules()) {
            SurveyMan.LOGGER.info(rule.getClass().getName());
            rule.check(survey);
        }
        SurveyMan.LOGGER.info("Finished wellformedness checks.");
    }

    public static Report staticAnalysis(
            Survey survey,
            Classifier classifier,
            int n,
            double granularity,
            double alpha) throws SurveyException {
        wellFormednessChecks(survey);
        List<Simulation.ROC> rocList = new ArrayList<Simulation.ROC>();
        for (double percRandomRespondents = 0.0 ; percRandomRespondents <= 1.0 ; percRandomRespondents += granularity) {
            List<SurveyResponse> srs = Simulation.simulate(survey, n, percRandomRespondents);
            rocList.add(Simulation.analyze(survey, srs, classifier, alpha));
        }
        SurveyMan.LOGGER.info("Finished simulation.");
        return new Report(
                survey.sourceName,
                survey.sid,
                QCMetrics.minimumPathLength(survey),
                QCMetrics.maximumPathLength(survey),
                QCMetrics.averagePathLength(survey),
                QCMetrics.getMaxPossibleEntropy(survey),
                QCMetrics.getFrequenciesOfRandomCorrelation(survey, n, alpha),
                rocList
        );
    }
}

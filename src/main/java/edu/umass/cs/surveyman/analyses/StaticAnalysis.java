package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.qc.*;
import edu.umass.cs.surveyman.output.CorrelationStruct;
import edu.umass.cs.surveyman.qc.classifiers.AbstractClassifier;
import edu.umass.cs.surveyman.qc.respondents.AbstractRespondent;
import edu.umass.cs.surveyman.qc.respondents.NoisyLexicographicRespondent;
import edu.umass.cs.surveyman.qc.respondents.NonRandomRespondent;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
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
        public final List<Simulation.ROC> rocListBest;
        public final List<Simulation.ROC> rocListWorst;
        public final double CORR_COEFF_THRESHOLD = 0.6;

        Report(String surveyName,
               String surveyId,
               int minPathLength,
               int maxPathLength,
               double avgPathLength,
               double maxPossibleEntropy,
               Map<Question, Map<Question, CorrelationStruct>> frequenciesOfRandomCorrelations,
               List<Simulation.ROC> rocListBest,
               List<Simulation.ROC> rocListWorst
        ) {
            this.surveyName = surveyName;
            this.surveyId = surveyId;
            this.avgPathLength = avgPathLength;
            this.maxPossibleEntropy = maxPossibleEntropy;
            this.maxPathLength = maxPathLength;
            this.minPathLength = minPathLength;
            this.frequenciesOfRandomCorrelations = frequenciesOfRandomCorrelations;
            this.rocListBest = rocListBest;
            this.rocListWorst = rocListWorst;
        }

        private double getFrequencyOfRandomCorrelation() {
            int ctAboveString = 0;
            int numComparisons = 0;
            for (Map<Question, CorrelationStruct> entry : this.frequenciesOfRandomCorrelations.values()) {
                for (CorrelationStruct correlationStruct : entry.values()) {
                    numComparisons++;
                    if (correlationStruct.coefficientValue > CORR_COEFF_THRESHOLD) {
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
                osw.write("group,percentBots,entropy,TP,FP,TN,FN\n");
                for (Simulation.ROC roc : rocListBest)
                    osw.write("best," + roc.toString());
                for (Simulation.ROC roc : rocListWorst)
                    osw.write("worst," + roc.toString());
                osw.close();
            } catch (IOException e) {
                SurveyMan.LOGGER.warn(e);
            }
        }

        public String jsonizeBadActors(List<Simulation.ROC> rocList) {
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
                            "\"badactors\" : { \"best\" : %s, \"worst\" : %s } \"" +
                    "}",
                    this.surveyName,
                    this.minPathLength,
                    this.maxPathLength,
                    this.avgPathLength,
                    this.maxPossibleEntropy,
                    this.getFrequencyOfRandomCorrelation(),
                    this.jsonizeBadActors(this.rocListBest),
                    this.jsonizeBadActors(this.rocListWorst)
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
            AbstractClassifier classifier,
            double granularity,
            RandomRespondent.AdversaryType adversaryType
    ) throws SurveyException {

        long begin = System.currentTimeMillis();

        wellFormednessChecks(survey);

        List<Simulation.ROC> rocListBest = new ArrayList<>();
        List<Simulation.ROC> rocListWorst = new ArrayList<>();

        AbstractRespondent weakPopulation = new NonRandomRespondent(survey);
        AbstractRespondent strongPopulation = new NoisyLexicographicRespondent(survey, 0.1);
        AbstractRespondent rando = new RandomRespondent(survey, adversaryType);

        QCMetrics qcMetrics = new QCMetrics(survey, classifier);

        List<SurveyResponse> srsBest = new ArrayList<>();
        List<SurveyResponse> srsWorst = new ArrayList<>();
        List<SurveyResponse> randos = new ArrayList<>();

        for (long i = qcMetrics.getSampleSize().getLeft(); i > 0 ; i--) {
            SurveyResponse sr;
            // Generate the responses and set their validity.
            sr = strongPopulation.getResponse();
            sr.setKnownValidityStatus(KnownValidityStatus.YES);
            srsBest.add(sr);
            sr = weakPopulation.getResponse();
            sr.setKnownValidityStatus(KnownValidityStatus.YES);
            srsWorst.add(sr);
            sr = rando.getResponse();
            sr.setKnownValidityStatus(KnownValidityStatus.NO);
            randos.add(sr);
        }

        for (double percAdversary = 0.0 ; percAdversary <= 1.0 ; percAdversary += granularity) {

            Collections.shuffle(srsBest);
            Collections.shuffle(srsWorst);
            Collections.shuffle(randos);

            int numRando = (int) Math.floor(percAdversary * randos.size());
            int numReal = srsBest.size() - numRando;

            List<SurveyResponse> combo1 = new ArrayList<>(randos.subList(0, numRando));
            List<SurveyResponse> combo2 = new ArrayList<>(combo1);
            combo1.addAll(srsBest.subList(0, numReal));
            combo2.addAll(srsWorst.subList(0, numReal));

            rocListBest.add(Simulation.analyze(survey, combo1, classifier));
            rocListWorst.add(Simulation.analyze(survey, combo2, classifier));
        }
        SurveyMan.LOGGER.info("Finished simulation.");
        Report report =  new Report(
                survey.sourceName,
                survey.sid,
                qcMetrics.minimumPathLength(),
                qcMetrics.maximumPathLength(),
                qcMetrics.averagePathLength(),
                qcMetrics.getMaxPossibleEntropy(),
                qcMetrics.getFrequenciesOfRandomCorrelation(),
                rocListBest,
                rocListWorst
        );
        long end = System.currentTimeMillis();
        SurveyMan.LOGGER.info(String.format("Static analysis completed in %ds.", end - begin));
        return report;
    }
}

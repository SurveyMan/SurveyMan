package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.rules.AbstractRule;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;


public class StaticAnalysis {

    public static class Report {

        public final double avgPathLength;
        public final double maxPossibleEntropy;
        public final int maxPathLength;
        public final int minPathLength;

        Report(int minPathLength,
               int maxPathLength,
               double avgPathLength,
               double maxPossibleEntropy) {
            this.avgPathLength = avgPathLength;
            this.maxPossibleEntropy = maxPossibleEntropy;
            this.maxPathLength = maxPathLength;
            this.minPathLength = minPathLength;
        }

        public void print(OutputStream stream) {
            OutputStreamWriter osw = new OutputStreamWriter(stream);
            try {
                osw.write(String.format(
                        "Min Path Length:\t%d\n" +
                        "Max Path Length:\t%d\n" +
                        "Average Path Length:\t%f\n" +
                        "Max Possible Entropy:\t%f\n",
                        this.minPathLength,
                        this.maxPathLength,
                        this.avgPathLength,
                        this.maxPossibleEntropy
                ));
            } catch (IOException e) {
                SurveyMan.LOGGER.warn(e);
            }
            try {
                osw.close();
            } catch (IOException e) {
                SurveyMan.LOGGER.warn(e);
            }
        }
    }

    public static void wellFormednessChecks(Survey survey) throws SurveyException{
        SurveyMan.LOGGER.info(String.format("Testing %d rules...", AbstractRule.getRules().size()));
        for (AbstractRule rule : AbstractRule.getRules()) {
            SurveyMan.LOGGER.info(rule.getClass().getName());
            rule.check(survey);
        }
    }

    public static Report staticAnalysis(Survey survey) throws SurveyException {
        wellFormednessChecks(survey);
        return new Report(
                QCMetrics.minimumPathLength(survey),
                QCMetrics.maximumPathLength(survey),
                QCMetrics.averagePathLength(survey),
                QCMetrics.getMaxPossibleEntropy(survey)
        );
    }
}

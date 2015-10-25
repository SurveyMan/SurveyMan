package edu.umass.cs.surveyman.qc.biases;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.output.CorrelationStruct;
import edu.umass.cs.surveyman.output.OrderBiasStruct;
import edu.umass.cs.surveyman.qc.CoefficentsAndTests;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by etosch on 10/24/15.
 */
public class OrderBias {
    /**
     * Searches for significant order biases observed in survey responses.
     * @param survey The survey these respondents answered.
     * @param responses The list of actual or simulated responses to the survey.
     * @param alpha The cutoff used for determining whether the bias is significant.
     * @return An OrderBiasStruct object containing all of the values just computed.
     * @throws SurveyException
     */
    public static OrderBiasStruct calculateOrderBiases(
            Survey survey,
            List<? extends SurveyResponse> responses,
            double alpha)
            throws SurveyException
    {
        OrderBiasStruct retval = new OrderBiasStruct(survey, alpha);
        int numImbalances = 0;
        int numComparisons = 0;
        for (int i = 0; i < survey.questions.size() - 1; i++) {
            Question q1 = survey.questions.get(i);
            if (!q1.exclusive) {
                SurveyMan.LOGGER.info(String.format("Skipping comparison with Question %s: not exclusive.", q1.id));
                continue;
            }
            for (int j = i + 1; j < survey.questions.size(); j++) {
                Question q2 = survey.questions.get(j);
                if (!q2.exclusive) {
                    SurveyMan.LOGGER.info(String.format("Skipping comparison with Question %s: not exclusive", q1.id));
                    continue;
                }
                // q1 answers when q1 comes first
                List<SurveyDatum> q1q2 = new ArrayList<>();
                // q1 answers when q1 comes second
                List<SurveyDatum> q2q1 = new ArrayList<>();
                for (SurveyResponse sr : responses) {
                    if (sr.hasResponseForQuestion(q1) && sr.hasResponseForQuestion(q2)) {
                        IQuestionResponse qr1 = sr.getResponseForQuestion(q1);
                        IQuestionResponse qr2 = sr.getResponseForQuestion(q2);
                        if (qr1.getIndexSeen() < qr2.getIndexSeen())
                            // add the response to question 1 to the list of q1s that precede q2s
                            q1q2.add(qr1.getOpts().get(0).c);
                        else if (qr1.getIndexSeen() > qr2.getIndexSeen())
                            // add the response to question 1 to the list of q1s that succeed q2s
                            q2q1.add(qr1.getOpts().get(0).c);
                    }
                }
                if (q1.ordered && q2.ordered)
                    retval.update(q1, q2, new CorrelationStruct(
                            CoefficentsAndTests.U,
                            QCMetrics.mannWhitney(q1, q2, q1q2, q2q1),
                                    -0.0,
                            q1,
                            q2,
                            q1q2.size(),
                            q2q1.size())
                    );
                else {
                    SurveyDatum[] categoryA = q1.getOptListByIndex();
                    int[][] contingencyTable = new int[categoryA.length][2];
                    for (int k = 0 ; k < categoryA.length ; k++ ){
                        contingencyTable[k][0] = 0;
                        contingencyTable[k][1] = 0;
                    }
                    // if the difference in the observations is large, the orderings is incomparable
                    // make this more principled in the future.
                    int numq1q2 = q1q2.size(), numq2q1 = q2q1.size();
                    double ratio = numq1q2 / (double) numq2q1;
                    numComparisons++;
                    if (numq1q2 < 5 || numq2q1 < 5 || (ratio < 0.5 || ratio > 1.5)) {
                        numImbalances++;
                        SurveyMan.LOGGER.warn(String.format("Difference in observations is imbalanced: %d vs. %d (%f)", numq1q2, numq2q1, ratio));
                        continue;
                    }
                    for (SurveyDatum c : q1q2)
                        contingencyTable[Arrays.asList(categoryA).indexOf(c)][0] += 1;
                    for (SurveyDatum c : q2q1)
                        contingencyTable[Arrays.asList(categoryA).indexOf(c)][1] += 1;

                    int df = categoryA.length - 1;
                    double testStatistic = QCMetrics.chiSquared(contingencyTable, categoryA, new List[]{q1q2, q2q1});
                    double pvalue = QCMetrics.chiSquareTest(df, testStatistic);
                    retval.update(q1, q2, new CorrelationStruct(
                            CoefficentsAndTests.CHI,
                            testStatistic,
                            pvalue,
                            q1,
                            q2,
                            numq1q2,
                            numq2q1));
                }
            }
        }
        retval.setNumImbalances(numImbalances);
        retval.setNumComparisons(numComparisons);
        return retval;
    }
}

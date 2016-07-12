package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.CoefficentsAndTests;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;
import edu.umass.cs.surveyman.utils.Tabularable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

public class OrderBiasStruct extends BiasStruct implements Jsonable, Tabularable {

    protected final QuestionCorrelationStruct biases;
    final public double alpha;
    final public int minSamples = 10;
    final public double ratioRange = 0.25;
    private int numImbalances = 0;
    private int numComparisons = 0;

    private OrderBiasStruct(Survey survey, double alpha) {
        this.alpha = alpha;
        this.biases = new QuestionCorrelationStruct();
        QuestionCorrelationStruct.populateStruct(survey, this);
    }

    /**
     * Searches for significant order biases observed in survey responses.
     * @param qcMetrics
     * @param responses The list of actual or simulated responses to the survey.
     * @param alpha The cutoff used for determining whether the bias is significant.
     * @return An OrderBiasStruct object containing all of the values just computed.
     * @throws SurveyException
     */
    public static OrderBiasStruct makeStruct(
            QCMetrics qcMetrics,
            List<? extends SurveyResponse> responses,
            double alpha)
            throws SurveyException
    {
        Survey survey = qcMetrics.survey;
        OrderBiasStruct retval = new OrderBiasStruct(survey, alpha);
        for (int i = 0; i < survey.questions.size() - 1; i++) {
            Question q1 = survey.questions.get(i);
            if (!QCMetrics.isAnalyzable(q1) || !q1.exclusive) continue;
            for (int j = i + 1; j < survey.questions.size(); j++) {
                Question q2 = survey.questions.get(j);
                if (!QCMetrics.isAnalyzable(q2) || !q2.exclusive) continue;
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
                // if the difference in the observations is large, the orderings is incomparable
                // make this more principled in the future.
                int numq1q2 = q1q2.size(), numq2q1 = q2q1.size();
                double ratio = numq1q2 / (double) numq2q1;
                if (((!q1.ordered || !q2.ordered) && (numq1q2 < 5 || numq2q1 < 5)) || (ratio < 0.5 || ratio > 1.5)) {
                    retval.numImbalances++;
                    SurveyMan.LOGGER.warn(java.lang.String.format("Difference in observations is imbalanced: %d vs. %d (%f)", numq1q2, numq2q1, ratio));
                    continue;
                } else {
                    retval.numComparisons++;
                }
                if (q1.ordered && q2.ordered) {
                    ImmutablePair<Double, Double> pair = QCMetrics.mannWhitney(q1, q2, q1q2, q2q1);
                    retval.update(q1, q2, new CorrelationStruct(
                                    CoefficentsAndTests.U,
                                    pair.getLeft(),
                                    pair.getRight(),
                                    q1,
                                    q2,
                                    q1q2.size(),
                                    q2q1.size())
                    );
                } else {
                    SurveyDatum[] categoryA = q1.getOptListByIndex();
                    int[][] contingencyTable = new int[categoryA.length][2];
                    for (int k = 0 ; k < categoryA.length ; k++ ){
                        contingencyTable[k][0] = 0;
                        contingencyTable[k][1] = 0;
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
        return retval;
    }

    public void update(Question q1, Question q2, CorrelationStruct correlationStruct) {
        this.biases.get(q1).put(q2, correlationStruct);
    }

    private boolean flagCondition(CorrelationStruct struct)
    {
        double ratio = struct.numSamplesA / (double) struct.numSamplesB;
        return  struct.coefficientPValue > 0.0 &&
                struct.coefficientPValue < this.alpha &&
                struct.numSamplesA > minSamples &&
                struct.numSamplesB > minSamples &&
                ratio >  1.0 - ratioRange &&
                ratio < 1.0 + ratioRange;
    }

    @Override
    public java.lang.String tabularize() {
        List<java.lang.String> biases = new ArrayList<>();
        for (Question q1 : this.biases.keySet()) {
            for (Question q2: this.biases.get(q1).keySet()) {
                CorrelationStruct structs = this.biases.get(q1).get(q2);
                if (structs.empty)
                    continue;
                java.lang.String data = java.lang.String.format(
                        "\"%s\"\t\"%s\"\t\"%s\"\t%f\t%f\t%d\t%d",
                        q1.data,
                        q2.data,
                        structs.coefficientType.name(),
                        structs.coefficientValue,
                        structs.coefficientPValue,
                        structs.numSamplesA,
                        structs.numSamplesB);
                if (flagCondition(structs))
                    biases.add(data);
            }
        }
        return "question1\tquestion2\tcoefficient\tvalue\tpvalue\tnumquestion1\tnumquestion2\n" +
        StringUtils.join(biases, "\n");
    }

    @Override
    public java.lang.String toString()
    {
        return "Order Biases\n" +
                "Num Imbalances: " + this.numImbalances + "\n" +
                "Num Comparisons: " + this.numComparisons + "\n" +
                tabularize() + "\n";
    }

    @Override
    public java.lang.String jsonize() throws SurveyException
    {
        return BiasStruct.jsonize(this.biases);
    }
}

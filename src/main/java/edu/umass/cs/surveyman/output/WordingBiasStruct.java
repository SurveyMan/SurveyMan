package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.CoefficentsAndTests;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;
import edu.umass.cs.surveyman.utils.Tabularable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordingBiasStruct extends BiasStruct implements Jsonable, Tabularable {

    protected BlockCorrelationStruct biases;
    final public double alpha;
    private int numImbalances = 0;
    private int numComparisons = 0;


    public WordingBiasStruct(Survey survey, double alpha) {
        this.alpha = alpha;
        this.biases = new BlockCorrelationStruct();
        BlockCorrelationStruct.populateStruct(survey, this);
    }


    private static Set<Set<Question>> getAllVariants(Survey survey)
    {
        Set<Set<Question>> allVariants = new HashSet<>();
        for (Question question : survey.questions) {
            List<Question> variants = question.getVariants();
            if (variants.size() > 1) {
                allVariants.add(new HashSet<>(variants));
            }
        }
        return allVariants;
    }

    private static boolean variantSetIsAnalyzable(Set<Question> variantSet)
    {
        for (Question q : variantSet) {
            if (!QCMetrics.isAnalyzable(q)) return false;
        }
        return true;
    }

    private static boolean variantSetIsExclusive(Set<Question> variantSet)
    {
        for (Question q : variantSet) {
            if (!q.exclusive) return false;
        }
        return true;
    }

    public static boolean imbalanced(Question q1, Question q2, int numq1answers, int numq2answers)
    {
        double ratio = numq1answers / (double) numq2answers;
        if (((!q1.ordered || !q2.ordered) && (numq1answers < 5 || numq2answers < 5)) || (ratio < 0.5 || ratio > 1.5)) {
            SurveyMan.LOGGER.warn(java.lang.String.format("Difference in observations is imbalanced: %d vs. %d (%f)", numq1answers, numq2answers, ratio));
            return true;
        } else return false;
    }

    /**
     * Searches for significant wording biases observed in survey responses.
     * @param responses The list of actual or simulated responses to the survey.
     * @param alpha The cutoff used for determining whether the bias is significant.
     * @return A WordingBiasStruct object containing all of the values just computed.
     * @throws SurveyException
     */
    public static WordingBiasStruct makeStruct(QCMetrics qcMetrics, List<? extends SurveyResponse> responses, double alpha) throws SurveyException {
        WordingBiasStruct retval = new WordingBiasStruct(qcMetrics.survey, alpha);
        for (Set<Question> variantSet : getAllVariants(qcMetrics.survey)) {
            if (! variantSetIsAnalyzable(variantSet)) continue;
            if (! variantSetIsExclusive(variantSet)) continue;
            List<Question> variants = new ArrayList<>(variantSet);
            for (int k = 0; k < variants.size() - 1; k++) {
                Question q1 = variants.get(k);
                for (int j = k + 1; j < variants.size(); j++) {
                    Question q2 = variants.get(j);
                    List<SurveyDatum> q1answers = new ArrayList<>();
                    List<SurveyDatum> q2answers = new ArrayList<>();
                    for (SurveyResponse sr : responses) {
                        if (sr.hasResponseForQuestion(q1))
                            q1answers.add(sr.getResponseForQuestion(q1).getOpts().get(0).c);
                        if (sr.hasResponseForQuestion(q2))
                            q2answers.add(sr.getResponseForQuestion(q2).getOpts().get(0).c);
                    }
                    if (imbalanced(q1, q2, q1answers.size(), q2answers.size())) {
                        retval.numImbalances++; continue;
                    } else {
                        retval.numComparisons++;
                    }
                    if (q1.ordered && q2.ordered) {
                        ImmutablePair<Double, Double> pair = QCMetrics.mannWhitney(q1, q2, q1answers, q2answers);
                        retval.update(q1.block, q1, q2, new CorrelationStruct(
                                            CoefficentsAndTests.U,
                                            pair.getLeft(),
                                            pair.getRight(),
                                            q1,
                                            q2,
                                            q1answers.size(),
                                            q2answers.size())
                            );
                        } else {
                            // sort by their source rows
                            List<SurveyDatum> categoryA = Arrays.asList(q1.getOptListByIndex());
                            List<SurveyDatum> categoryB = Arrays.asList(q2.getOptListByIndex());
                            Collections.sort(categoryA);
                            Collections.sort(categoryB);
                            int[][] contingencyTable = new int[categoryA.size()][2];
                            // initialize the contingency table
                            for (int i = 0 ; i < categoryA.size() ; i++) {
                                contingencyTable[i][0] = 0;
                                contingencyTable[i][1] = 0;
                            }

                            for (SurveyDatum c : q1answers)
                                contingencyTable[categoryA.indexOf(c)][0] += 1;
                            for (SurveyDatum c : q2answers) {
                                SurveyDatum c_ = q1.getVariantOption(q2, c);
                                System.out.println( c.toString() + " "
                                        + categoryB.indexOf(c) + " "
                                        + c_.toString() + " "
                                        + categoryA.indexOf(c_));
                                contingencyTable[categoryB.indexOf(c)][1] += 1;
                            }

                            int df = categoryA.size() - 1;
                            double testStatistic = QCMetrics.chiSquared(contingencyTable, categoryA.toArray(), new List[]{q1answers, q2answers});
                            double pvalue = QCMetrics.chiSquareTest(df, testStatistic);

                            retval.update(q1.block, q1, q2, new CorrelationStruct(
                                    CoefficentsAndTests.CHI,
                                    testStatistic,
                                    pvalue,
                                    q1,
                                    q2,
                                    q1answers.size(),
                                    q2answers.size())
                            );
                        }
                    }
                }
            }
        return retval;
    }

    public void update(Block b, Question q1, Question q2, CorrelationStruct correlationStruct) {
        this.biases.get(b).get(q1).put(q2, correlationStruct);
    }

    private boolean flagCondition(CorrelationStruct struct) {
//        double ratio = struct.numSamplesA / (double) struct.numSamplesB;
//        return struct.coefficientValue > 0.0 &&
//                struct.coefficientValue < this.alpha &&
//                struct.numSamplesA > minSamples &&
//                struct.numSamplesB > minSamples &&
//                ratio >  1.0 - ratioRange &&
//                ratio < 1.0 + ratioRange;
        return struct.coefficientPValue < this.alpha;
    }

    @Override
    public java.lang.String jsonize() throws SurveyException
    {
        return BiasStruct.jsonize(biases);
    }

    @Override
    public java.lang.String tabularize() {
        List<java.lang.String> biases = new ArrayList<>();
        for (QuestionCorrelationStruct variants: this.biases.values()) {
            for (Question q1 : variants.keySet()) {
                for (Question q2: variants.get(q1).keySet()) {
                    CorrelationStruct structs = variants.get(q1).get(q2);
                    if (structs.empty)
                        continue;
                    java.lang.String data = java.lang.String.format(
                            "\"%s\"\t\"%s\"\t%s\t%f\t%f\t%d\t%d",
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
        }
        return StringUtils.join(biases, "\n");
    }

    @Override
    public java.lang.String toString()
    {
        return "Wording Biases\n" +
                "Num Imbalances: " + this.numImbalances + "\n" +
                "Num Comparisons: " + this.numComparisons + "\n" +
                "question1\tquestion2\tcoefficient\tvalue\tpvalue\tnumq1q2\tnumq2q1\n" +
                 this.tabularize() +
                "\n";
    }


}

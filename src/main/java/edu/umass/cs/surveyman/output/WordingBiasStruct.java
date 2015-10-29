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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

public class WordingBiasStruct {

    private Map<Block, Map<Question, Map<Question, CorrelationStruct>>> biases = new HashMap<>();
    final public double alpha;
    private int numImbalances = 0;
    private int numComparisons = 0;


    public WordingBiasStruct(Survey survey, double alpha) {
        this.alpha = alpha;
        for (Block b : survey.getAllBlocks()) {
            if (b.getBranchParadigm().equals(Block.BranchParadigm.ALL)) {
                Map<Question, Map<Question, CorrelationStruct>> outermap = new HashMap<>();
                for (Question q1 : b.questions) {
                    Map<Question, CorrelationStruct> innermap = new HashMap<>();
                    for (Question q2 : b.questions)
                        innermap.put(q2, null);
                    outermap.put(q1, innermap);
                }
            biases.put(b, outermap);
            }
        }
    }

    /**
     * Searches for significant wording biases observed in survey responses.
     * @param responses The list of actual or simulated responses to the survey.
     * @param alpha The cutoff used for determining whether the bias is significant.
     * @return A WordingBiasStruct object containing all of the values just computed.
     * @throws SurveyException
     */
    public static WordingBiasStruct calculateWordingBiases(QCMetrics qcMetrics, List<? extends SurveyResponse> responses, double alpha) throws SurveyException {
        WordingBiasStruct retval = new WordingBiasStruct(qcMetrics.survey, alpha);
        // get variants
        for (Block b : qcMetrics.survey.getAllBlocks()) {
            if (b.getBranchParadigm().equals(Block.BranchParadigm.ALL)) {
                List<Question> variants = b.branchQ.getVariants();
                for (int k = 0; k < variants.size() - 1; k++) {
                    Question q1 = variants.get(k);
                    if (!QCMetrics.isAnalyzable(q1) || !q1.exclusive) continue;
                    for (int j = k + 1; j < variants.size(); j++) {
                        Question q2 = variants.get(j);
                        assert q2.exclusive : "All question variants must have the same parameter settings.";
                        List<SurveyDatum> q1answers = new ArrayList<>();
                        List<SurveyDatum> q2answers = new ArrayList<>();
                        for (SurveyResponse sr : responses) {
                            if (sr.hasResponseForQuestion(q1))
                                q1answers.add(sr.getResponseForQuestion(q1).getOpts().get(0).c);
                            if (sr.hasResponseForQuestion(q2))
                                q2answers.add(sr.getResponseForQuestion(q2).getOpts().get(0).c);
                        }
                        //  make sure we don't have imbalances
                        int numq1answers = q1answers.size(), numq2answers = q2answers.size();
                        double ratio = numq1answers / (double) numq2answers;
                        if (((!q1.ordered || !q2.ordered) && (numq1answers < 5 || numq2answers< 5)) || (ratio < 0.5 || ratio > 1.5)) {
                            retval.numImbalances++;
                            SurveyMan.LOGGER.warn(String.format("Difference in observations is imbalanced: %d vs. %d (%f)", numq1answers, numq2answers, ratio));
                            continue;
                        } else {
                            retval.numComparisons++;
                        }
                        if (q1.ordered && q2.ordered) {
                            ImmutablePair<Double, Double> pair = QCMetrics.mannWhitney(q1, q2, q1answers, q2answers);
                            retval.update(b, q1, q2, new CorrelationStruct(
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
                                contingencyTable[0][categoryA.indexOf(c)] += 1;
                            for (SurveyDatum c : q2answers)
                                contingencyTable[0][categoryA.indexOf(c)] += 1;

                            int df = categoryA.size() - 1;
                            double testStatistic = QCMetrics.chiSquared(contingencyTable, categoryA.toArray(), new List[]{q1answers, q2answers});
                            double pvalue = QCMetrics.chiSquareTest(df, testStatistic);

                            retval.update(b, q1, q2, new CorrelationStruct(
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

    public String jsonize()
    {
        List<String> wayOuterMap = new ArrayList<>();
        for (Map.Entry<Block, Map<Question, Map<Question, CorrelationStruct>>> a : this.biases.entrySet()){
            String blockId = a.getKey().getStrId();
            List<String> outerMap = new ArrayList<>();
            for (Map.Entry<Question, Map<Question, CorrelationStruct>> b : a.getValue().entrySet()) {
                String outerQuid = b.getKey().id;
                // list of inner quids to corr objects
                List<String> innerMap = new ArrayList<>();
                for (Map.Entry<Question, CorrelationStruct> c : b.getValue().entrySet()) {
                    String innerQuid = c.getKey().id;
                    if (c.getValue() == null)
                        continue;
                    String corrJson = c.getValue().jsonize();
                    innerMap.add(String.format("\"%s\" : %s",
                            innerQuid,
                            corrJson));
                }
                outerMap.add(String.format("\"%s\" : { %s }",
                        outerQuid,
                        StringUtils.join(innerMap, ", ")));
            }
            wayOuterMap.add(String.format("\"%s\" : { %s }",
                    blockId,
                    StringUtils.join(outerMap, ", ")));
        }
        return String.format("{ %s }", StringUtils.join(wayOuterMap, ", "));
    }

    @Override
    public String toString()
    {
        List<String> biases = new ArrayList<>();
        for (Map<Question, Map<Question, CorrelationStruct>> variants: this.biases.values()) {
            for (Question q1 : variants.keySet()) {
                for (Question q2: variants.get(q1).keySet()) {
                    CorrelationStruct structs = variants.get(q1).get(q2);
                    if (structs == null)
                        continue;
                    String data = String.format(
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
        return "Wording Biases\n" +
                "Num Imbalances: " + this.numImbalances + "\n" +
                "Num Comparisons: " + this.numComparisons + "\n" +
                "question1\tquestion2\tcoefficient\tvalue\tpvalue\tnumq1q2\tnumq2q1\n" +
                StringUtils.join(biases, "\n") +
                "\n";
    }


}

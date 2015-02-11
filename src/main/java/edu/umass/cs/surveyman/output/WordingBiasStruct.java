package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.output.CorrelationStruct;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordingBiasStruct {

    private Map<Block, Map<Question, Map<Question, CorrelationStruct>>> biases =
            new HashMap<Block, Map<Question, Map<Question, CorrelationStruct>>>();
    final public double alpha;
    final public int minSamples = 10;
    final public double ratioRange = 0.25;


    public WordingBiasStruct(Survey survey, double alpha) {
        this.alpha = alpha;
        for (Block b : survey.getAllBlocks()) {
            if (b.branchParadigm.equals(Block.BranchParadigm.ALL)) {
                Map<Question, Map<Question, CorrelationStruct>> outermap =
                        new HashMap<Question, Map<Question, CorrelationStruct>>();
                for (Question q1 : b.questions) {
                    Map<Question, CorrelationStruct> innermap =
                            new HashMap<Question, CorrelationStruct>();
                    for (Question q2 : b.questions)
                        innermap.put(q2, null);
                    outermap.put(q1, innermap);
                }
            biases.put(b, outermap);
            }
        }
    }

    public void update(Block b, Question q1, Question q2, CorrelationStruct correlationStruct) {
        this.biases.get(b).get(q1).put(q2, correlationStruct);
    }

    private boolean flagCondition(CorrelationStruct struct) {
        double ratio = struct.numSamplesA / (double) struct.numSamplesB;
        return struct.coefficientValue > 0.0 &&
                struct.coefficientValue < this.alpha &&
                struct.numSamplesA > minSamples &&
                struct.numSamplesB > minSamples &&
                ratio >  1.0 - ratioRange &&
                ratio < 1.0 + ratioRange;
    }

    @Override
    public String toString() {
        List<String> biases = new ArrayList<String>();
        for (Map<Question, Map<Question, CorrelationStruct>> variants: this.biases.values()) {
            for (Question q1 : variants.keySet()) {
                for (Question q2: variants.get(q1).keySet()) {
                    CorrelationStruct structs = variants.get(q1).get(q2);
                    if (structs == null)
                        continue;
                    String data = String.format(
                            "\"%s\"\t\"%s\"\t%s\t%f\t%d\t%d",
                            q1.data,
                            q2.data,
                            structs.coefficientType.name(),
                            structs.coefficientValue,
                            structs.numSamplesA,
                            structs.numSamplesB);
                    SurveyMan.LOGGER.debug(data);
                    if (flagCondition(structs))
                        biases.add(data);
                }
            }
        }
        return "Wording Biases\n" +
                "question1\tquestion2\tcoefficient\tpvalue\tnumq1q2\tnumq2q1\n" +
                StringUtils.join(biases, "\n") +
                "\n";
    }



}

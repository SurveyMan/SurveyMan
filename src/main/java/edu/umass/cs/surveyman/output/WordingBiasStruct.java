package edu.umass.cs.surveyman.output;

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

    public WordingBiasStruct(Survey survey, double alpha) {
        this.alpha = alpha;
        for (Block b : survey.blocks.values()) {
            if (b.branchParadigm.equals(Block.BranchParadigm.ALL)) {
                Map<Question, Map<Question, CorrelationStruct>> m =
                        new HashMap<Question, Map<Question, CorrelationStruct>>();
                biases.put(b, m);

            }
        }
    }

    public void update(Block b, Question q1, Question q2, CorrelationStruct correlationStruct) {
        this.biases.get(b).get(q1).put(q2, correlationStruct);
    }

    @Override
    public String toString() {
        List<String> biases = new ArrayList<String>();
        for (Map<Question, Map<Question, CorrelationStruct>> variants: this.biases.values()) {
            for (Question q1 : variants.keySet()) {
                for (Question q2: variants.get(q1).keySet()) {
                    CorrelationStruct structs = variants.get(q1).get(q2);
                    if (structs!=null && structs.coefficientValue > this.alpha)
                        biases.add(String.format(
                                "\"%s\"\t\"%s\"\t%s\t%f",
                                q1.data,
                                q2.data,
                                structs.coefficientType.name(),
                                structs.coefficientValue)
                        );
                }
            }
        }
        return "Discovered Wording Biases\n"+StringUtils.join(biases, "\n");
    }



}

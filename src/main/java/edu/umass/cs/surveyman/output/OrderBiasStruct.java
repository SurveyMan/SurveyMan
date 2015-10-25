package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderBiasStruct {

    private Map<Question, Map<Question, CorrelationStruct>> biases = new HashMap<>();
    final public double alpha;
    final public int minSamples = 10;
    final public double ratioRange = 0.25;
    private int numImbalances;
    private int numComparisons;

    public OrderBiasStruct(Survey survey, double alpha) {
        this.alpha = alpha;
        for (int i = 0; i < survey.questions.size() - 1; i++) {
            Question q1 = survey.questions.get(i);
            this.biases.put(q1, new HashMap<Question, CorrelationStruct>());
            for (int j = i + 1; j < survey.questions.size(); j++) {
                Question q2 = survey.questions.get(j);
                this.biases.get(q1).put(q2, null);
            }
        }
    }

    public void update(Question q1, Question q2, CorrelationStruct correlationStruct) {
        this.biases.get(q1).put(q2, correlationStruct);
    }

    public void setNumImbalances(int imbalances) {
        this.numImbalances = imbalances;
    }

    public void setNumComparisons(int comparisons) {
        this.numComparisons = comparisons;
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
    public String toString()
    {
        List<String> biases = new ArrayList<>();
        for (Question q1 : this.biases.keySet()) {
            for (Question q2: this.biases.get(q1).keySet()) {
                CorrelationStruct structs = this.biases.get(q1).get(q2);
                if (structs == null)
                    continue;
                String data = String.format(
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
        return "Order Biases\n" +
                "Num Imbalances: " + this.numImbalances + "\n" +
                "Num Comparisons: " + this.numComparisons + "\n" +
                "question1\tquestion2\tcoefficient\tvalue\tpvalue\tnumquestion1\tnumquestion2\n" +
                StringUtils.join(biases, "\n") +
                "\n";
    }

    public String jsonize()
    {
        List<String> outerVals = new ArrayList<String>();
        for (Question q1 : this.biases.keySet()) {
            List<String> innerVals = new ArrayList<String>();
            for (Question q2: this.biases.get(q1).keySet()) {
                CorrelationStruct structs = this.biases.get(q1).get(q2);
                innerVals.add(String.format(
                        "\"%s\" : %s",
                        q2.id,
                        structs==null? "null" : structs.jsonize())
                );
            }
            outerVals.add(String.format(
                    "\"%s\" : { %s }",
                    q1.id,
                    StringUtils.join(innerVals, ", ")));
        }
        return String.format("{ %s }", StringUtils.join(outerVals, ", "));
    }

}

package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderBiasStruct {

    private Map<Question, Map<Question, CorrelationStruct>> biases =
            new HashMap<Question, Map<Question, CorrelationStruct>>();
    final public double alpha;
    final public int minSamples = 10;
    final public double ratioRange = 0.25;

    public OrderBiasStruct(
            Survey survey,
            double alpha)
    {
        this.alpha = alpha;
        for (Question q1 : survey.questions) {
            this.biases.put(q1, new HashMap<Question, CorrelationStruct>());
            for (Question q2 : survey.questions)
                this.biases.get(q1).put(q2, null);
        }
    }

    public void update(
            Question q1,
            Question q2,
            CorrelationStruct correlationStruct)
    {
        this.biases.get(q1).put(q2, correlationStruct);
    }

    private boolean flagCondition(
            CorrelationStruct struct)
    {
        double ratio = struct.numSamplesA / (double) struct.numSamplesB;
        return struct.coefficientValue > 0.0 &&
                struct.coefficientValue < this.alpha &&
                struct.numSamplesA > minSamples &&
                struct.numSamplesB > minSamples &&
                ratio >  1.0 - ratioRange &&
                ratio < 1.0 + ratioRange;
    }

    @Override
    public String toString()
    {
        List<String> biases = new ArrayList<String>();
        for (Question q1 : this.biases.keySet()) {
            for (Question q2: this.biases.get(q1).keySet()) {
                CorrelationStruct structs = this.biases.get(q1).get(q2);
                if (structs == null)
                    continue;
                String data = String.format(
                    "\"%s\"\t\"%s\"\t\"%s\"\t%f\t%d\t%d",
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
        return "Order Biases\n" +
                "question1\tquestion2\tcoefficient\tpvalue\tnumquestion1\tnumquestion2\n" +
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
                        q2.quid,
                        structs==null? "null" : structs.jsonize())
                );
            }
            outerVals.add(String.format(
                    "\"%s\" : { %s }",
                    q1.quid,
                    StringUtils.join(innerVals, ", ")));
        }
        return String.format("{ %s }", StringUtils.join(outerVals, ", "));
    }

}

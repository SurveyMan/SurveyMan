package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.output.CorrelationStruct;
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

    public OrderBiasStruct(Survey survey, double alpha) {
        this.alpha = alpha;
        for (Question q1 : survey.questions) {
            this.biases.put(q1, new HashMap<Question, CorrelationStruct>());
            for (Question q2 : survey.questions)
                this.biases.get(q1).put(q2, null);
        }
    }

    public void update(Question q1, Question q2, CorrelationStruct correlationStruct) {
        this.biases.get(q1).put(q2, correlationStruct);
    }

    @Override
    public String toString() {
        List<String> biases = new ArrayList<String>();
        for (Question q1 : this.biases.keySet()) {
            for (Question q2: this.biases.get(q1).keySet()) {
                CorrelationStruct structs = this.biases.get(q1).get(q2);
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
        return "Discovered Order Biases\n"+StringUtils.join(biases, "\n");
    }



    public String jsonize() {
        List<String> outerVals = new ArrayList<String>();
        for (Question q1 : this.biases.keySet()) {
            List<String> innerVals = new ArrayList<String>();
            for (Question q2: this.biases.get(q1).keySet()) {
                CorrelationStruct structs = this.biases.get(q1).get(q2);
                innerVals.add(String.format(
                        "\"%s\" : %s",
                        q2.quid,
                        structs==null? "" : structs.jsonize())
                );
            }
            outerVals.add(String.format(
                    "%s : { %s }",
                    q1.quid,
                    StringUtils.join(innerVals, ",")));
        }
        return String.format("{ %s }", StringUtils.join(outerVals, ","));
    }


}

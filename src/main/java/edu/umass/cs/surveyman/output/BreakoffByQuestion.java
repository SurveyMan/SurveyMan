package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.jsonify.Jsonify;
import edu.umass.cs.surveyman.utils.Tuple;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BreakoffByQuestion extends BreakoffStruct<Question> {

    public BreakoffByQuestion(Survey survey) {
        for (Question q : survey.questions) {
            this.put(q, 0);
        }
    }

    /**
     * Aggregates the breakoff according to which question was last answered.
     * @param responses The list of actual or simulated responses to the survey.
     * @return A BreakoffByQuestion object containing all of the values just computed.
     */
    public static BreakoffByQuestion makeStruct(QCMetrics qcMetrics, List<? extends SurveyResponse> responses) {
        BreakoffByQuestion breakoffMap = new BreakoffByQuestion(qcMetrics.survey);
        for (SurveyResponse sr : responses) {
            IQuestionResponse lastQuestionResponse = sr.getLastQuestionAnswered();
            if (!qcMetrics.isFinalQuestion(lastQuestionResponse.getQuestion(), sr))
                breakoffMap.update(lastQuestionResponse.getQuestion());
        }
        return breakoffMap;
    }

    @Override
    public void update(Question question) {
        this.put(question, this.get(question) + 1);
    }

    @Override
    public java.lang.String tabularize() {
        List<java.lang.String> retval = new ArrayList<>();
        List<Tuple> pairs = new ArrayList<>();
        for (Map.Entry<Question, Integer> entry : this.entrySet()) {
            Question question = entry.getKey();
            int ct = entry.getValue();
            if (ct > 0) {
                pairs.add(new Tuple(question, ct));
            }
        }
        Collections.sort(pairs);
        for (Tuple p : pairs)
            retval.add(java.lang.String.format("%s\t%d", p.fst, p.snd));
        return "Question Text\tCount\n" + StringUtils.join(retval, "\n") + "\n";
    }

    @Override
    public java.lang.String jsonize() throws SurveyException {
        List<Object> m = new ArrayList<>();
        for (Map.Entry<Question, Integer> e : this.entrySet()) {
            java.lang.String key = e.getKey().getId();
            Object val = e.getValue();
            m.add(key);
            m.add(val);
        }
        return Jsonify.jsonify(Jsonify.mapify(m.toArray()));
    }

    @Override
    public java.lang.String toString() {
        return this.tabularize();
    }
}

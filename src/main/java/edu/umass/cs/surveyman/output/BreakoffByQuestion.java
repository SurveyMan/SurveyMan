package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
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
    public static BreakoffByQuestion calculateBreakoffByQuestion(QCMetrics qcMetrics, List<? extends SurveyResponse> responses) {
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
        this.put(question, this.get(question)+1);
    }

    @Override
    public String jsonize()
    {
        List<String> retval = new ArrayList<String>();
        for (Map.Entry<Question, Integer> entry : this.entrySet())
            retval.add(String.format("\"%s\" : %d", entry.getKey().id, entry.getValue()));
        return String.format("{ %s }", StringUtils.join(retval, ", "));
    }

    @Override
    public String toString()
    {
        List<String> retval = new ArrayList<String>();
        List<Pair> pairs = new ArrayList<Pair>();
        for (Map.Entry<Question, Integer> entry : this.entrySet()) {
            Question question = entry.getKey();
            int ct = entry.getValue();
            if (ct > 0) {
                pairs.add(new Pair(question, ct));
            }
        }
        Collections.sort(pairs);
        for (Pair p : pairs)
            retval.add(String.format("%s\t%d", p.thing.data, p.frequency));
        return "Question Text\tCount\n" + StringUtils.join(retval, "\n") + "\n";
    }

}

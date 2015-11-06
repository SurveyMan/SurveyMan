package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.qc.SurveyDAG;
import edu.umass.cs.surveyman.survey.Survey;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BreakoffByPosition extends BreakoffStruct<Integer> {

    public BreakoffByPosition(Survey survey) {
        SurveyDAG surveyDAG = SurveyDAG.getDag(survey);
        int maxpos = surveyDAG.maximumPathLength();
        for (int i = 0 ; i < maxpos ; i++)
            this.put(i, 0);
    }

    /**
     * Aggregates the breakoff according to the last position answered.
     * @param responses The list of actual or simulated responses to the survey.
     * @return A BreakoffByPosition object containing all of the values just computed.
     */
    public static BreakoffByPosition calculateBreakoffByPosition(QCMetrics qcMetrics, List<? extends SurveyResponse> responses) {
        // for now this just reports breakoff, rather than statistically significant breakoff
        BreakoffByPosition breakoffMap = new BreakoffByPosition(qcMetrics.survey);
        for (SurveyResponse sr : responses) {
            IQuestionResponse qr = sr.getLastQuestionAnswered();
            if (!qcMetrics.isFinalQuestion(qr.getQuestion(), sr)) {
                breakoffMap.update(qr.getIndexSeen());
            }
        }
        return breakoffMap;
    }

    @Override
    public void update(Integer integer) {
        this.put(integer, this.get(integer) + 1);
    }

    @Override
    public String jsonize() {
        List<String> retval = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : this.entrySet())
            retval.add(String.format("\"%d\" : %d", entry.getKey(), entry.getValue()));
        return String.format("{ %s }", StringUtils.join(retval, ", "));
    }

    @Override
    public String toString() {
        List<String> retval = new ArrayList<>();
        List<Pair> pairs = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : this.entrySet()) {
            int pos = entry.getKey(), ct = entry.getValue();
            if (ct > 0) {
                pairs.add(new Pair(pos + 1, ct));
            }
        }
        Collections.sort(pairs);
        for (Pair p : pairs)
            retval.add(String.format("%d\t%d", p.thing, p.frequency));
        return "Position\tCount\n" + StringUtils.join(retval, "\n") + "\n";
    }

}

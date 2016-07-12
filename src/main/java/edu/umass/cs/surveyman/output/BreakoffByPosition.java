package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.qc.SurveyDAG;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Tuple;
import edu.umass.cs.surveyman.utils.jsonify.Jsonify;
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
            this.put(new Integer(i), new Integer(0));
    }

    /**
     * Aggregates the breakoff according to the last position answered.
     * @param responses The list of actual or simulated responses to the survey.
     * @return A BreakoffByPosition object containing all of the values just computed.
     */
    public static BreakoffByPosition makeStruct(QCMetrics qcMetrics, List<? extends SurveyResponse> responses) {
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
    public void update(Integer i) {
        this.put(i, this.get(i) + 1);
    }

    @Override
    public java.lang.String jsonize() throws SurveyException {
        List<Object> m = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : this.entrySet()) {
            m.add(e.getKey().toString());
            m.add(e.getValue());
        }
        return Jsonify.jsonify(Jsonify.mapify(m.toArray()));
    }

    @Override
    public java.lang.String toString() {
        return this.tabularize();
    }

    @Override
    public java.lang.String tabularize() {

        List<java.lang.String> retval = new ArrayList<>();
        List<Tuple<Integer, Integer>> pairs = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : this.entrySet()) {
            Integer pos = entry.getKey();
            int ct = entry.getValue();
            if (ct > 0) {
                // Positions are 0-indexed. We need to make them 1-indexed.
                pairs.add(new Tuple<>(pos + 1, ct));
            }
        }
        Collections.sort(pairs);
        for (Tuple p : pairs)
            retval.add(java.lang.String.format("%s\t%s", p.fst.toString(), p.snd.toString()));
        return "Position\tCount\n" + StringUtils.join(retval, "\n") + "\n";
    }

}

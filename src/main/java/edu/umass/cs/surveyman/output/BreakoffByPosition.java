package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Survey;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BreakoffByPosition extends BreakoffStruct<Integer> {

    public BreakoffByPosition(Survey survey) {
        int maxpos = QCMetrics.maximumPathLength(survey);
        for (int i = 1 ; i <= maxpos ; i++)
            this.put(i, 0);
    }

    @Override
    public void update(Integer integer) {
        this.put(integer, this.get(integer) + 1);
    }

    @Override
    public String jsonize() {
        List<String> retval = new ArrayList<String>();
        for (Map.Entry<Integer, Integer> entry : this.entrySet())
            retval.add(String.format("%s : %d", entry.getKey(), entry.getValue()));
        return String.format("{ %s }", StringUtils.join(retval, ","));
    }

    @Override
    public String toString() {
        List<String> retval = new ArrayList<String>();
        List<Pair> pairs = new ArrayList<Pair>();
        for (Map.Entry<Integer, Integer> entry : this.entrySet())
            pairs.add(new Pair(entry.getKey(), entry.getValue()));
        Collections.sort(pairs);
        for (Pair p : pairs)
            retval.add(String.format("%d\t%d", p.thing, p.frequency));
        return "Position\tCount\n" + StringUtils.join(retval, "\n") + "\n";
    }

}

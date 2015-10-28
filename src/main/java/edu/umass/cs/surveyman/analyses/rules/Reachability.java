package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.qc.Interpreter;
import edu.umass.cs.surveyman.qc.SurveyDAG;
import edu.umass.cs.surveyman.qc.SurveyPath;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.survey.exceptions.UnreachableBlockException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ensures that there exists a path through every block in the survey, such that every question has the possibility of
 * being asked.
 */
public class Reachability extends AbstractRule {

    /**
     Adds itself to the {@link edu.umass.cs.surveyman.analyses.AbstractRule rule} registry.
     */
    public Reachability() {
        AbstractRule.registerRule(this);
    }

    @Override
    public void check(Survey survey) throws SurveyException {
        // only need to check stationary top-level blocks
        List<Block> stationaryBlocks = Interpreter.partitionBlocks(survey).get(false);
        SurveyDAG dag = SurveyDAG.getDag(survey);
        Set<Block> allVisited = new HashSet<>();
        for (SurveyPath path : dag)
            for (Block b : path)
                allVisited.add(b);
        for (Block b : stationaryBlocks)
            if (!allVisited.contains(b))
                throw new UnreachableBlockException(b);
    }
}

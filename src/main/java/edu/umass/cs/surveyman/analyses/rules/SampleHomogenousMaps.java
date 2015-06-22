package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.input.exceptions.BranchException;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.Collection;

/**
 * Ensures that the maps in the survey are homogenous -- there is some conceptual overlap with {@link edu.umass.cs.surveyman.analyses.rules.BranchConsistency BranchConsistency}.
 */
public class SampleHomogenousMaps extends AbstractRule {

    /**
     Adds itself to the {@link edu.umass.cs.surveyman.analyses.AbstractRule rule} registry.
     */
    public SampleHomogenousMaps() {
        AbstractRule.registerRule(this);
    }

    private static void ensureSampleHomogenousMaps(Block block) throws SurveyException{
        if (block.getBranchParadigm().equals(Block.BranchParadigm.ALL)){
            assert(block.subBlocks.size()==0);
            Collection<Block> dests = block.getBranchDestinations();
            for (Question q : block.questions){
                Collection<Block> qDests = q.getBranchDestinations();
                if (!qDests.containsAll(dests) || !dests.containsAll(qDests))
                    throw new BranchException(String.format("Question %s has branch map %s; was expecting %s", q, qDests, dests));
            }
        } else {
            for (Block b : block.subBlocks)
                ensureSampleHomogenousMaps(b);
        }
    }

    @Override
    public void check(Survey survey) throws SurveyException {
        for (Block b : survey.topLevelBlocks)
            ensureSampleHomogenousMaps(b);
    }
}

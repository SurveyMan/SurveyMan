package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.input.exceptions.BranchException;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.Collection;

public class SampleHomogenousMaps extends AbstractRule {

    public SampleHomogenousMaps() {
        AbstractRule.registerRule(this);
    }

    private static void ensureSampleHomogenousMaps(Block block) throws SurveyException{
        if (block.branchParadigm.equals(Block.BranchParadigm.ALL)){
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

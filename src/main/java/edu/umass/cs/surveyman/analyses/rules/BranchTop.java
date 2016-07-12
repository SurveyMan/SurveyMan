package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

/**
 * Ensures that all "true" (i.e., branch-ONE) branching occurs from top-level, static blocks.
 */
public class BranchTop extends AbstractRule {

    /**
     Adds itself to the {@link edu.umass.cs.surveyman.analyses.AbstractRule rule} registry.
     */
    public BranchTop() {
        AbstractRule.registerRule(this);
    }

    @Override
    public void check(Survey survey) throws SurveyException {
        for (Block b : survey.topLevelBlocks) {
            if (b.isRandomized())
                // no branching from this block
                assert(!b.getBranchParadigm().equals(Block.BranchParadigm.ONE));
            else {
                // no branching to randomizable blocks
                if (b.getBranchParadigm().equals(Block.BranchParadigm.ONE)){
                    assert b.branchQ!=null : String.format("Branch ONE from block %s does not have branchQ set", b.getId());
                    Question branchQ = b.branchQ;
                    assert branchQ.isBranchQuestion() : String.format("Branch map for question %s is empty", branchQ.id);
                    for (Block dest : branchQ.block.getBranchDestinations())
                        if (dest!=null)
                            assert(!dest.isRandomized());
                }
            }
        }
    }
}

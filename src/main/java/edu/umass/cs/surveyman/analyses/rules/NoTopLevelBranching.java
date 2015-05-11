package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public class NoTopLevelBranching extends AbstractRule {

    public NoTopLevelBranching() {
        AbstractRule.registerRule(this);
    }

    @Override
    public void check(Survey survey) throws SurveyException {
        for (Block b : survey.topLevelBlocks) {
            if (b.isRandomized())
                // no branching from this block
                assert(!b.branchParadigm.equals(Block.BranchParadigm.ONE));
            else {
                // no branching to randomizable blocks
                if (b.branchParadigm.equals(Block.BranchParadigm.ONE)){
                    assert b.branchQ!=null : String.format("Branch ONE from block %s does not have branchQ set", b.getStrId());
                    Question branchQ = b.branchQ;
                    assert branchQ.getBranchDestinations().size() > 0 : String.format("Branch map for question %s is empty", branchQ.id);
                    for (Block dest : branchQ.getBranchDestinations())
                        if (dest!=null)
                            assert(!dest.isRandomized());
                }
            }
        }
    }

}

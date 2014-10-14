package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.BlockException;
import edu.umass.cs.surveyman.survey.exceptions.BranchConsistencyException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public class BranchParadigm extends AbstractRule {

    public BranchParadigm() {
        AbstractRule.registerRule(this);
    }

    public static int ensureBranchParadigms(Block b) throws SurveyException {
        switch (b.branchParadigm) {
            case NONE:
                // all of its children have the branch paradigm NONE or ALL
                for (Block sb : b.subBlocks) {
                    if (sb.branchParadigm.equals(Block.BranchParadigm.ONE))
                        throw new BranchConsistencyException(String.format("Parent block %s has paradigm %s. Ancestor block %s has paradigm %s."
                                , b.getStrId(), b.branchParadigm.name(), sb.getStrId(), sb.branchParadigm.name()));
                    ensureBranchParadigms(sb);
                }
                break;
            case ALL:
                if (!b.subBlocks.isEmpty())
                    throw new BlockException(String.format("Blocks with the branch-all paradigm cannot have subblocks. " +
                            "(This is semantically at odds with what branch-all does.)"));
                break;
            case ONE:
                int ones = 0;
                for (Block sb : b.subBlocks) {
                    if (sb.branchParadigm.equals(Block.BranchParadigm.NONE))
                        ensureBranchParadigms(sb);
                    else {
                        ones++;
                        int kidsOnes = ensureBranchParadigms(sb);
                        if (ones > 1 || kidsOnes > 1)
                            throw new BlockException(String.format("Blocks can only have one branching subblock. " +
                                    "Block %s has %d immediate branching blocks and at least %d branching blocks in one of its children"
                                    , b.getStrId(), ones, kidsOnes));
                    }
                    return ones;
                }
        }
        return 0;
    }

    public void check(Survey survey) throws SurveyException {
        for (Block b : survey.topLevelBlocks) {
            ensureBranchParadigms(b);
        }
    }
}

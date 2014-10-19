package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.input.exceptions.BranchException;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public class BranchForward extends AbstractRule {

    public BranchForward() {
        AbstractRule.registerRule(this);
    }

    private static void ensureBranchForward(int[] toBlock, Question q, Survey survey) throws SurveyException {
        int[] fromBlock = q.block.getBlockId();
        for (int i=0; i<toBlock.length; i++)
            if (fromBlock[i]>toBlock[i]) {
                SurveyException e = new BranchException(q.block.getStrId(), Block.idToString(toBlock, survey.blocks));
                LOGGER.warn(e);
                throw e;
            }
    }

    @Override
    public void check(Survey survey) throws SurveyException {
        for (Question q : survey.questions) {
            if (!q.isBranchQuestion())
                continue;
            for (Block b : q.getBranchDestinations()) {
                if (b!=null) // if we aren't sampling
                    ensureBranchForward(b.getBlockId(), q, survey);
            }
        }
    }
}

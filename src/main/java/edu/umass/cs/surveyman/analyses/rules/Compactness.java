package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.List;
import java.util.Map;

public class Compactness extends AbstractRule {

    public Compactness() {
        AbstractRule.registerRule(this);
    }

    @Override
    public void check(Survey survey) throws SurveyException {
        //first check the top level
        List<Block> topLevelBlocks = survey.topLevelBlocks;
        Map<String, Block> allBlockLookUp = survey.blocks;
        Block[] temp = new Block[topLevelBlocks.size()];
        for (Block b : topLevelBlocks) {
            int[] id = b.getBlockId();
            if (temp[id[0]-1]==null)
                temp[id[0]-1]=b;
            else {
                SurveyException e = new SyntaxException(String.format("Block %s is noncontiguous.", b.getStrId()));
                LOGGER.warn(e);
                throw e;
            }
        }
        if (allBlockLookUp==null)
            return;
        for (Block b : allBlockLookUp.values())
            if (b.subBlocks!=null)
                for (Block bb : b.subBlocks)
                    if (bb==null) {
                        SurveyException e = new SyntaxException(String.format("Detected noncontiguous subblock in parent block %s", b.getStrId()));
                        LOGGER.warn(e);
                        throw e;
                    }
    }
}

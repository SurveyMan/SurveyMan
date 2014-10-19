package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.BranchConsistencyException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public class BranchConsistency extends AbstractRule {

    public BranchConsistency() {
        AbstractRule.registerRule(this);
    }

    public void check(Survey survey) throws SurveyException {
        for (Block b : survey.blocks.values()) {
            switch (b.branchParadigm) {
                case NONE:
                    if (b.branchQ!=null)
                        throw new BranchConsistencyException(String.format("Block (%s) is set to have no branching but has its branch question set to (%s)", b, b.branchQ));
                    break;
                case ALL:
                    for (Question q : b.questions)
                        if (!q.isBranchQuestion())
                            throw new BranchConsistencyException(String.format("Block (%s) is set to have all branching but question (%s) does not have its branch map set.", b, q));
                    break;
                case ONE:
                    Question branchQ = null;
                    for (Question q : b.questions)
                        if (q.isBranchQuestion())
                            if (branchQ==null)
                                branchQ = q;
                            else throw new BranchConsistencyException(String.format("Block (%s) expected to have exactly one branch question, but both questions (%s) and (%s) are set to  branch.", b, q, branchQ));
                    if (branchQ!=null && !branchQ.equals(b.branchQ))
                        throw new BranchConsistencyException(String.format("Block (%s) expected (%s) to be the branch question, but found question (%s) instead.", b, b.branchQ, branchQ));
                    break;
            }
        }
    }
}

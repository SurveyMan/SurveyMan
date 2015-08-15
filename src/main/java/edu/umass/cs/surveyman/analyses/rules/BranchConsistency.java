package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.BranchConsistencyException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

/**
 * Ensures that branch rules are obeyed on a per-block basis:
 * <ul>
 *     <li><b>NONE:</b> If a block is marked as having no branch target, then it should not have a branch question
 *     saved.</li>
 *     <li><b>ALL:</b> If a block is marked as {@link edu.umass.cs.surveyman.survey.Block.BranchParadigm ALL}, then
 *     three requirements must be met:
 *     <ol>
 *         <li>All questions are branch questions.</li>
 *         <li>All questions have equal number of options.</li>
 *         <li>All questions have equivalent branch maps.</li>
 *     </ol>
 *     </li>
 *     <li><b>ONE:</b> If a block is marked as {@link edu.umass.cs.surveyman.survey.Block.BranchParadigm ONE}, then
 *     we make sure there are no other branch questions and that the stored branch question is the appropriate one. </li>
 * </ul>
 */
public class BranchConsistency extends AbstractRule {

    /**
        Adds itself to the {@link edu.umass.cs.surveyman.analyses.AbstractRule rule} registry.
     */
    public BranchConsistency() {
        AbstractRule.registerRule(this);
    }

    @Override
    public void check(Survey survey) throws SurveyException {
        for (Block b : survey.blocks.values()) {
            switch (b.getBranchParadigm()) {
                case NONE:
                    if (b.branchQ!=null)
                        throw new BranchConsistencyException(String.format("Block (%s) is set to have no branching but has its branch question set to (%s)", b, b.branchQ));
                    break;
                case ALL:
                    Block[] orderedTargets = null;
                    int numOptions = 0;
                    for (Question q : b.questions) {
                        if (!q.isBranchQuestion())
                            throw new BranchConsistencyException(String.format("Block \n%s\nis set to have all branching but question \n\t%s\n does not have its branch map set.", b, q));
                        SurveyDatum[] orderedSources = q.getOptListByIndex();
                        if (numOptions == 0) {
                            numOptions = q.options.size();
                            orderedTargets = new Block[numOptions];
                            for (int i = 0; i < orderedSources.length; i++)
                                orderedTargets[i] = q.getBranchDest(orderedSources[i]);
                        }
                        if (q.options.size() != numOptions)
                            throw new BranchConsistencyException(String.format("Branch-All Block contains questions having both %d and %d options.",
                                    numOptions, q.options.size()));
                        for (int i = 0; i < numOptions; i++) {
                            if (!orderedTargets[i].equals(q.getBranchDest(orderedSources[i])))
                                throw new BranchConsistencyException("Branch-All Block contains questions whose branch maps are not aligned.");
                        }
                    }
                    break;
                case ONE:
                    Question branchQ = null;
                    for (Question q : b.questions)
                        if (q.isBranchQuestion())
                            if (branchQ==null)
                                branchQ = q;
                            else if (! branchQ.equals(q))
                                throw new BranchConsistencyException(String.format("Block (%s) expected to have exactly one branch question, but both questions (%s) and (%s) are set to  branch.", b, q, branchQ));
                    if (branchQ!=null && !branchQ.equals(b.branchQ))
                        throw new BranchConsistencyException(String.format("Block (%s) expected (%s) to be the branch question, but found question (%s) instead.", b, b.branchQ, branchQ));
                    break;
            }
        }
    }
}

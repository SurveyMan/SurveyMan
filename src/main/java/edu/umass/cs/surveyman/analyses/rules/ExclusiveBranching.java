package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.input.exceptions.BranchException;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

/**
 * Ensures that branching only occurs from questions marked "exclusive" -- that is, no checkbox nor freetext questions
 * may be branch questions.
 */
public class ExclusiveBranching extends AbstractRule {

    /**
     Adds itself to the {@link edu.umass.cs.surveyman.analyses.AbstractRule rule} registry.
     */
    public ExclusiveBranching() {
        AbstractRule.registerRule(this);
    }

    @Override
    public void check(Survey survey) throws SurveyException {
        for (Question q : survey.questions)
            if (q.isBranchQuestion() && !q.exclusive)
                throw new BranchException(String.format("Question %s is nonexclusive and branches.", q));
    }
}

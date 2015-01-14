package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.input.exceptions.BranchException;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public class ExclusiveBranching extends AbstractRule {

    public ExclusiveBranching() {
        AbstractRule.registerRule(this);
    }

    public void check(Survey survey) throws SurveyException {
        for (Question q : survey.questions)
            if (q.isBranchQuestion() && !q.exclusive)
                throw new BranchException(String.format("Question %s is nonexclusive and branches.", q));
    }
}

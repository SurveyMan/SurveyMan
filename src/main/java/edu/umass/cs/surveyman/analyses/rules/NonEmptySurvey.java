package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.EmptySurveyException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

/**
 * Surveys must have some content!
 */
public class NonEmptySurvey extends AbstractRule {

    public NonEmptySurvey() { AbstractRule.registerRule(this); }

    @Override
    public void check(Survey survey) throws SurveyException {
        if (survey.questions.isEmpty())
            throw new EmptySurveyException();
    }
}

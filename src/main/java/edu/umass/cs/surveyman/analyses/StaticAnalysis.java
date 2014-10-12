package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.analyses.rules.AbstractRule;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class StaticAnalysis {

    public void staticAnalysis(Survey survey) throws SurveyException {
        for (AbstractRule rule : AbstractRule.getRules()) {
            Logger.getAnonymousLogger().log(Level.INFO, rule.getClass().getName());
            rule.check(survey);
        }
    }
}

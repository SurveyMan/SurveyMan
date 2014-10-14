package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.rules.AbstractRule;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;


public class StaticAnalysis {

    public static void staticAnalysis(Survey survey) throws SurveyException {
        SurveyMan.LOGGER.info(String.format("Testing %d rules...", AbstractRule.getRules().size()));
        for (AbstractRule rule : AbstractRule.getRules()) {
            SurveyMan.LOGGER.info(rule.getClass().getName());
            rule.check(survey);
        }
    }
}

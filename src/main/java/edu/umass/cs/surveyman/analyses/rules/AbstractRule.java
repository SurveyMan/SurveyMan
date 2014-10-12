package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractRule {

    final protected static Logger LOGGER = Logger.getLogger(AbstractRule.class);
    private static final Set<AbstractRule> register = new HashSet<AbstractRule>();

    public static void registerRule(AbstractRule rule) {
        register.add(rule);
    }

    public static Set<AbstractRule> getRules() {
        return register;
    }

    public abstract void check(Survey survey) throws SurveyException;

}

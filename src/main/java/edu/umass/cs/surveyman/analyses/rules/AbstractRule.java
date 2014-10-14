package edu.umass.cs.surveyman.analyses.rules;

import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRule {

    final protected static Logger LOGGER = Logger.getLogger(AbstractRule.class);
    private static final List<AbstractRule> register = new ArrayList<AbstractRule>();

    public static void registerRule(AbstractRule rule) {
        register.add(rule);
    }

    public static void registerRule(Class<? extends AbstractRule> clz) {
        try {
            register.add(clz.newInstance());
        } catch (InstantiationException e) {
            LOGGER.error(e);
        } catch (IllegalAccessException e) {
            LOGGER.error(e);
        }
    }

    public static void unregisterRule(AbstractRule rule) {
        register.remove(rule);
    }

    public static void unregisterRule(Class<? extends AbstractRule> clz) {
        try {
            register.remove(clz.newInstance());
        } catch (InstantiationException e) {
            LOGGER.error(e);
        } catch (IllegalAccessException e) {
            LOGGER.error(e);
        }
    }

    public static List<AbstractRule> getRules() {
        return register;
    }

    public static List<AbstractRule> getDefaultRules() {
        new BranchConsistency();
        new BranchForward();
        new BranchParadigm();
        new BranchTop();
        new Compactness();
        new ExclusiveBranching();
        new NoTopLevelBranching();
        new Reachability();
        new SampleHomogenousMaps();
        return register;
    }

    public abstract void check(Survey survey) throws SurveyException;

}

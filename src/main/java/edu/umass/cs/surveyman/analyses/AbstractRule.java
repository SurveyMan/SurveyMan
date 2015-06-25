package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.analyses.rules.*;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.log4j.Logger;

import java.util.*;

public abstract class AbstractRule {

    final protected static Logger LOGGER = Logger.getLogger(AbstractRule.class);
    private static final Map<Class, AbstractRule> register = new HashMap<Class, AbstractRule>();

    public static void registerRule(AbstractRule rule) {
        register.put(rule.getClass(), rule);
    }

    public static void registerRule(Class<? extends AbstractRule> clz) {
        try {
            register.put(clz, clz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error(e);
        }
    }

    public static void unregisterRule(AbstractRule rule) {
        register.remove(rule.getClass());
    }

    public static void unregisterRule(Class<? extends AbstractRule> clz) {
        register.remove(clz);
    }

    public static Collection<AbstractRule> getRules() {
        return register.values();
    }

    public static Map<Class, AbstractRule> getDefaultRules() {
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

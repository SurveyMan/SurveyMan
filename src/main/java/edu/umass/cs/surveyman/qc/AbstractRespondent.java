package edu.umass.cs.surveyman.qc;

import clojure.lang.PersistentVector;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Base class for all respondents. Respondent constructors take the survey and set up all preferences.
 */
public abstract class AbstractRespondent {

    public static final Logger LOGGER = SurveyMan.LOGGER;
    protected static final MersenneTwister rng = Interpreter.random;

    /**
     * Method to obtain the simulated survey response for the survey that this respondent was instantiated with.
     * @return The respondent's answer to the survey.
     */
    public abstract SurveyResponse getResponse();

    /**
     * Gets the size of the response space for a given question. For example, if the question is exclusive (i.e., a
     * radio button question), this will return the number of possible options. If the question is not exclusive (i.e.,
     * a checkbox question), this will return 2^|number of options|.
     * @param q The question whose sample space we wish to know.
     * @return An integer corresponding to the size of the sample space.
     * @throws java.lang.RuntimeException Cannot be called on freetext questions.
     */
    protected int getDenominator(Question q){
        // if the question is not exclusive, get the power set minus one, since they can't answer with zero.
        if (q.freetext)
            throw new RuntimeException("Cannot call getDenominator() on a freetext question.");
        if (q.exclusive == null)
            q.exclusive = false;
        return q.exclusive ? q.options.size() : (int) Math.pow(2.0, q.options.size()) - 1;
    }

    /**
     * Generates a string for freetext questions. If the input question's freetextPattern field is not null, it will
     * generate a random regular expression. If the freetextPattern field is null, but the freetextDefault field is not,
     * it will return the default string. Otherwise, it will return the string "DEFAULT".
     * @param q The question whose string response we wish to generate.
     * @return A string for this respondent's response.
     */
    protected String generateStringComponent(Question q) {
        if (q.freetextPattern!=null){
            String pat = String.format("(re-rand/re-rand #\"%s\")", q.freetextPattern.pattern());
            Var require = RT.var("clojure.core", "require");
            Var eval = RT.var("clojure.core", "eval");
            Var readString = RT.var("clojure.core", "read-string");
            require.invoke(Symbol.intern("re-rand"));
            Object str = eval.invoke(readString.invoke(pat));
            if (str instanceof String)
                return (String) str;
            return (String) ((PersistentVector) str).nth(0);
        } else if (q.freetextDefault!=null)
            return q.freetextDefault;
        else return "DEFAULT";
    }

    /**
     * Returns a copy of this respondent type. Use when running simulation.  Otherwise, the entire pool of respondents
     * will be the same object and quality control won't work.
     * @return New respondent, based on the same policy as this respondent.
     * @throws SurveyException
     */
    public abstract AbstractRespondent copy() throws SurveyException;

}

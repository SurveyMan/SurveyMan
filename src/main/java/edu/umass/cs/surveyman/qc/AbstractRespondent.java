package edu.umass.cs.surveyman.qc;

import clojure.lang.PersistentVector;
import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;
import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.AbstractSurveyResponse;
import edu.umass.cs.surveyman.survey.Question;
import org.apache.logging.log4j.Logger;

import java.util.Random;

/**
 * Base class for all respondents. Respondent constructors take the survey and set up all preferences.
 */
public abstract class AbstractRespondent {

    public static final Logger LOGGER = SurveyMan.LOGGER;
    protected static final Random rng = Interpreter.random;

    public abstract AbstractSurveyResponse getResponse();

    protected int getDenominator(Question q){
        // if the question is not exclusive, get the power set minus one, since they can't answer with zero.
        if (q.exclusive == null)
            q.exclusive = false;
        return q.exclusive ? q.options.size() : (int) Math.pow(2.0, q.options.size()) - 1;
    }

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

    public abstract AbstractRespondent copy();

}

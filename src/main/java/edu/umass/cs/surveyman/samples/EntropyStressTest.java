package edu.umass.cs.surveyman.samples;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.analyses.rules.Compactness;
import edu.umass.cs.surveyman.qc.Analyses;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;

import java.io.File;
import java.io.IOException;

public class EntropyStressTest {

    static Gensym qGensym = new Gensym("q");
    static Gensym oGensym = new Gensym("o");

    public static Survey createSurvey(
            int numquestions,
            int optionsarity)
            throws SurveyException
    {
        Survey s = new Survey();
        for (int i = 0 ; i < numquestions; i++){
            Question q = new Question(qGensym.next());
            for (int j = 0 ; j < optionsarity ; j++)
                q.addOption(new StringDatum(oGensym.next()));
            s.addQuestion(q);
        }
        return s;
    }

    public static void addQuestion(
            Survey survey)
            throws SurveyException
    {
        int arity = survey.questions.get(0).options.size();
        Question q = new Question(qGensym.next());
        for (int i = 0 ; i < arity ; i++) {
            q.addOption(new StringDatum(oGensym.next()));
        }
        survey.addQuestion(q);
    }

    public static void addOption(
            Survey survey)
            throws SurveyException
    {
        int arity = survey.questions.get(0).options.size();
        // adds a new option to every question in the survey
        for (Question q : survey.questions)
            q.addOption(new StringDatum(oGensym.next()));
    }

    /**
     * Execute `java -cp /path/to/surveyman.jar edu.umass.cs.surveyman.samples.EntropyStressTest`.
     * @param args None required.
     * @throws SurveyException
     * @throws IOException
     * @throws ProcessingException
     */
    public static void main(
            String[] args)
            throws SurveyException, IOException, ProcessingException
    {
        // Generate a series of surveys with increasing entropy and output the static analysis
        Survey survey = createSurvey(5, 4);
        AbstractRule.getDefaultRules();
        AbstractRule.unregisterRule(Compactness.class);
        new File("entropy_stress_test").mkdir();
        // start by seeing how adding more questions changes things.
        for (int i = 0 ; i < 100 ; i++) {
            SurveyMan.analyze(survey,
                    Analyses.STATIC,
                    Classifier.ENTROPY,
                    1000,
                    0.05,
                    0.05,
                    String.format("entropy_stress_test/%s_%d", Classifier.ENTROPY, i),
                    "",
                    false
                    );
            SurveyMan.analyze(survey,
                    Analyses.STATIC,
                    Classifier.LOG_LIKELIHOOD,
                    1000,
                    0.05,
                    0.05,
                    String.format("entropy_stress_test/%s_%d", Classifier.LOG_LIKELIHOOD, i),
                    "",
                    false
            );
            addQuestion(survey);
        }
        survey = createSurvey(5, 2);
        for (int i = 0; i < 10; i++) {
            SurveyMan.analyze(survey,
                    Analyses.STATIC,
                    Classifier.ENTROPY,
                    100,
                    0.05,
                    0.05,
                    String.format("entropy_stress_test/%s_%d", Classifier.ENTROPY, i),
                    "",
                    false
            );
            SurveyMan.analyze(survey,
                    Analyses.STATIC,
                    Classifier.LOG_LIKELIHOOD,
                    100,
                    0.05,
                    0.05,
                    String.format("entropy_stress_test/%s_%d", Classifier.LOG_LIKELIHOOD, i),
                    "",
                    false
            );
            addOption(survey);
        }
    }
}

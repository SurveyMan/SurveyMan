package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.analyses.ISurveyResponse;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates a cluster of responses.
 */
public class NonRandomRespondent extends AbstractRespondent {

    private Survey survey;
    private Map<Question, Component> answers = new HashMap<Question, Component>();
    private Map<Component, Double> strength = new HashMap<Component, Double>();

    public NonRandomRespondent(Survey survey)  {
        this.survey = survey;
        for (Question q : survey.questions) {
            if (!q.freetext && !q.options.isEmpty()) {
                List<Component> possibleAnswers = new ArrayList<Component>(q.options.values());
                Component answer = possibleAnswers.get(Interpreter.random.nextInt(possibleAnswers.size()));
                answers.put(q, answer);
                double uni = 1.0 / possibleAnswers.size();
                double pref = Interpreter.random.nextDouble() * (1.0 - uni);
                assert pref < (1 - uni);
                strength.put(answer, uni + pref);
            }
        }
    }

    @Override
    public ISurveyResponse getResponse() {
        Interpreter interpreter = new Interpreter(survey);
        try{
            do {
                Question q = interpreter.getNextQuestion();
                Component c = answers.get(q);
                List<Component> ans = new ArrayList<Component>();
                // calculate our answer
                if (!q.freetext && q.options.size() > 0 ) {
                    double prob = rng.nextDouble();
                    double threshold = strength.get(answers.get(q));
                    if (prob > threshold) {
                        // uniformly select from the other options
                        List<Component> otherAns = new ArrayList<Component>();
                        for (Component cc : q.options.values()) {
                            if (!c.equals(cc))
                                otherAns.add(cc);
                        }
                        ans.add(otherAns.get(rng.nextInt(otherAns.size())));
                    } else {
                        ans.add(c);
                    }
                    interpreter.answer(q, ans);
                }
            } while (!interpreter.terminated());
            return interpreter.getResponse();
        } catch (SurveyException se) {
            se.printStackTrace();
        }
        return null;
    }
}

package edu.umass.cs.surveyman.qc.respondents;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.Interpreter;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.List;

public class NoisyLexicographicRespondent extends LexicographicRespondent {

    private double epsilon;

    public NoisyLexicographicRespondent(Survey survey, double epsilon) throws SurveyException {
        this.survey = survey;
        this.epsilon = epsilon;
    }

    private SurveyDatum getNextAnswer(Question q, double epsilon) {

        if (q.freetext) return new StringDatum("default NoisyLexicographic");
        List<SurveyDatum> possibleAnswers = new ArrayList<>(q.options.values());
        sortByData(possibleAnswers);
        int nextAnswer = rng.nextInt(possibleAnswers.size() - 1) + 1;
        double coin = rng.nextInt(100) / 100.0;
        return coin < epsilon ?
                possibleAnswers.get(nextAnswer) :
                possibleAnswers.get(0);

    }

    @Override
    public SurveyResponse getResponse() {
        Interpreter interpreter = new Interpreter(survey);
        try {
            do {
                Question q = interpreter.getNextQuestion();
                if (q.isInstructional()) continue;
                List<SurveyDatum> ans = new ArrayList<>();
                SurveyDatum c = getNextAnswer(q, epsilon);
                ans.add(c);
                interpreter.answer(q, ans);
            } while (!interpreter.terminated());
                return interpreter.getResponse();
        } catch (SurveyException e) {
            SurveyMan.LOGGER.fatal(e);
        }
        System.exit(1); return null;
    }
}

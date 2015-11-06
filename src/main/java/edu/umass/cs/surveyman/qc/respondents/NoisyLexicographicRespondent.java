package edu.umass.cs.surveyman.qc.respondents;

import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.qc.Interpreter;
import edu.umass.cs.surveyman.survey.StringDatum;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.List;

public class NoisyLexicographicRespondent extends LexicographicRespondent {

    public NoisyLexicographicRespondent(Survey survey, double epsilon)
    {
        super(null);
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
            this.surveyResponse = interpreter.getResponse();
            this.surveyResponse.setKnownValidityStatus(KnownValidityStatus.YES);
        } catch (SurveyException e) {
            e.printStackTrace();
        }
        this.survey = survey;
    }

    private SurveyDatum getNextAnswer(Question q, double epsilon) {

        if (q.freetext) return new StringDatum("default NoisyLexicographic");
        List<SurveyDatum> possibleAnswers = new ArrayList<>(q.options.values());
        sortByData(possibleAnswers);
        int nextAnswer = AbstractRespondent.rng.nextInt(possibleAnswers.size());
        assert nextAnswer >= 0 : String.format("nextAnswer index < 0 : %d", nextAnswer);
        return rng.nextDouble() < epsilon ?
                possibleAnswers.get(nextAnswer) :
                possibleAnswers.get(0);

    }
}

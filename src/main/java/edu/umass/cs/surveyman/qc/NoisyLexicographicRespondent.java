package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NoisyLexicographicRespondent extends LexicographicRespondent {

    public NoisyLexicographicRespondent(Survey survey, double epsilon)
    {
        super(null);
        Random random = new Random();
        Interpreter interpreter = new Interpreter(survey);
        try {
            do {
                Question q = interpreter.getNextQuestion();
                List<SurveyDatum> possibleAnswers = new ArrayList<SurveyDatum>(q.options.values());
                sortByData(possibleAnswers);
                SurveyDatum c = random.nextDouble() < epsilon ?
                        possibleAnswers.get(random.nextInt(possibleAnswers.size())) :
                        possibleAnswers.get(0);
                List<SurveyDatum> ans = new ArrayList<SurveyDatum>();
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
}

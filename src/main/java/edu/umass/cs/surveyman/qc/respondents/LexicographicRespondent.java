package edu.umass.cs.surveyman.qc.respondents;

import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.Interpreter;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.List;

public class LexicographicRespondent extends AbstractRespondent {

    protected SurveyResponse surveyResponse;
    protected Survey survey;

    public LexicographicRespondent() {
    }

    static protected void sortByData(List<SurveyDatum> surveyDatumList) {
        for (int i = 0 ; i < surveyDatumList.size()-1 ; i++) {
            for (int j = i + 1 ; j < surveyDatumList.size() ; j++) {
                SurveyDatum datum1 = surveyDatumList.get(i);
                SurveyDatum datum2 = surveyDatumList.get(j);
                String comp1 = (datum1 instanceof StringDatum) ? ((StringDatum) datum1).data : ((HTMLDatum) datum1).data;
                String comp2 = (datum2 instanceof StringDatum) ? ((StringDatum) datum2).data : ((HTMLDatum) datum2).data;
                if (comp1.compareTo(comp2) > 0) {
                    surveyDatumList.set(i, datum2);
                    surveyDatumList.set(j, datum1);
                }
            }
        }
    }

    protected SurveyResponse simulate(Survey survey) throws SurveyException {
        Interpreter interpreter = new Interpreter(survey);
        do {
            Question q = interpreter.getNextQuestion();
            List<SurveyDatum> possibleAnswers = new ArrayList<>(q.options.values());
            sortByData(possibleAnswers);
            SurveyDatum c = possibleAnswers.get(0);
            List<SurveyDatum> ans = new ArrayList<>();
            ans.add(c);
            interpreter.answer(q, ans);
        } while (!interpreter.terminated());
        return interpreter.getResponse();
    }


    public LexicographicRespondent(Survey survey) throws SurveyException {
        if (survey==null) return;
        this.survey = survey;
        this.surveyResponse = simulate(survey);
    }

    private LexicographicRespondent(final LexicographicRespondent lexicographicRespondent) {
        this.survey = lexicographicRespondent.survey;
        this.surveyResponse = lexicographicRespondent.getResponse().copy();
    }

    @Override
    public SurveyResponse getResponse() {
        return surveyResponse;
    }

    @Override
    public AbstractRespondent copy()
    {
        return new LexicographicRespondent(this);
    }
}
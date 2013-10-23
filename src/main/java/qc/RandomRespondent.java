package qc;

import scalautils.OptData;
import scalautils.Response;
import survey.*;
import system.Gensym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomRespondent {

    public enum AdversaryType { UNIFORM, INNER, FIRST, LAST; }

    public static final Gensym gensym = new Gensym("rand");
    protected static final Random rng = new Random();

    public final Survey survey;
    public final AdversaryType adversaryType;
    public final String id = gensym.next();
    private double[][] posPref;
    private final double UNSET = -1.0;
    private List<SurveyResponse> responseList = new ArrayList<SurveyResponse>();

    public RandomRespondent(Survey survey, AdversaryType adversaryType) throws SurveyException {
        this.survey = survey;
        this.adversaryType = adversaryType;
        posPref = new double[survey.questions.size()][];
        for (int i = 0 ; i < survey.questions.size() ; i++) {
            Question q = survey.questions.get(i);
            posPref[i] = new double[q.options.size()];
            Arrays.fill(posPref[i], UNSET);
        }
        populatePosPreferences();
        populateResponseList();
    }

    private void populatePosPreferences() {
        for (int questionPos = 0 ; questionPos < posPref.length ; questionPos++) {
            if (adversaryType==AdversaryType.INNER) {
                int filled = (int) Math.ceil((double) posPref[questionPos].length / 2.0) - 1;
                int pieces = 2 * (int) Math.pow(2, filled) - 1;
                for (int i = 0 ; i <= filled ; i++) {
                    double prob = ((double) 1 + i) / (double) pieces;
                    posPref[questionPos][i] = prob;
                    int j = posPref[questionPos].length - i - 1;
                    if (posPref[questionPos][j] == UNSET)
                        posPref[questionPos][j] = prob;
                    else posPref[questionPos][j] += prob;
                }
            } else {
                for (int optionPos = 0 ; optionPos < posPref[questionPos].length ; optionPos++ ) {
                    switch (adversaryType) {
                        case UNIFORM:
                            posPref[questionPos][optionPos] = 1.0 / (double) posPref[questionPos].length;
                            break;
                        case FIRST:
                            if (optionPos==0)
                                posPref[questionPos][optionPos] = 1.0;
                            else posPref[questionPos][optionPos] = 0.0;
                            break;
                        case LAST:
                            if (optionPos==posPref[questionPos].length-1)
                                posPref[questionPos][optionPos] = 1.0;
                            else posPref[questionPos][optionPos] = 0.0;
                            break;
                    }
                }
            }
        }
    }

    private void populateResponseList() throws SurveyException {
        SurveyResponse sr = new SurveyResponse(id);
        sr.real = false;
        for (int i = 0 ; i < survey.questions.size() ; i++) {
            Question q = survey.questions.get(i);
            Component[] options = q.getOptListByIndex();
            double prob = rng.nextDouble();
            double cumulativeProb = 0.0;
            for (int j = 0 ; j < options.length ; j++) {
                cumulativeProb += posPref[i][j];
                if (prob < cumulativeProb) {
                    OptData choice = new OptData(options[j].getCid(), options[j].index);
                    Response r = new Response(q.quid, q.index, Arrays.asList(new OptData[]{ choice }));
                    SurveyResponse.QuestionResponse qr = new SurveyResponse.QuestionResponse(r, this.survey);
                    break;
                }
            }
        }
    }
}

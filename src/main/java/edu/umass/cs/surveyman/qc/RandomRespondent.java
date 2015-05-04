package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.StringComponent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class RandomRespondent extends AbstractRespondent {

    public enum AdversaryType { UNIFORM, INNER, FIRST, LAST }

    public static final Gensym gensym = new Gensym("rand");

    public final Survey survey;
    public final AdversaryType adversaryType;
    public final String id = gensym.next();
    private SurveyResponse response = null;
    private HashMap<Question, double[]> posPref;
    private final double UNSET = -1.0;

    // random respondent currently returns the same response every time. it should be updated to behave more like
    // nonrandom respondent and hold its profile
    public RandomRespondent(Survey survey, AdversaryType adversaryType)
            throws SurveyException
    {
        this.survey = survey;
        this.adversaryType = adversaryType;
        posPref = new HashMap<Question, double[]>();
        for (int i = 0 ; i < survey.questions.size() ; i++) {
            Question q = survey.questions.get(i);
            int denom = getDenominator(q);
            double[] prefs = new double[denom];
            Arrays.fill(prefs, UNSET);
            posPref.put(q, prefs);
        }
        populatePosPreferences();
        populateResponses();
    }

    private RandomRespondent(RandomRespondent randomRespondent)
            throws SurveyException
    {
        this.survey = randomRespondent.survey;
        this.adversaryType = randomRespondent.adversaryType;
        this.posPref = new HashMap<Question, double[]>(randomRespondent.posPref);
        populatePosPreferences();
        populateResponses();
    }

    @Override
    public SurveyResponse getResponse() {
        return this.response;
    }

    @Override
    public AbstractRespondent copy()
            throws SurveyException
    {
        return new RandomRespondent(this);
    }

    private void populatePosPreferences() {
        for (Question q : posPref.keySet()) {
            if (adversaryType==AdversaryType.INNER) {
                int filled = (int) Math.ceil((double) posPref.get(q).length / 2.0) - 1;
                int pieces = 2 * (int) Math.pow(2, filled) - 1;
                for (int i = 0 ; i <= filled ; i++) {
                    double prob = ((double) 1 + i) / (double) pieces;
                    posPref.get(q)[i] = prob;
                    int j = posPref.get(q).length - i - 1;
                    if (posPref.get(q)[j] == UNSET)
                        posPref.get(q)[j] = prob;
                    else posPref.get(q)[j] += prob;
                }
            } else {
                for (int optionPos = 0 ; optionPos < posPref.get(q).length ; optionPos++ ) {
                    switch (adversaryType) {
                        case UNIFORM:
                            posPref.get(q)[optionPos] = (1.0 / (double) posPref.get(q).length);
                            break;
                        case FIRST:
                            if (optionPos==0)
                                posPref.get(q)[optionPos] = 1.0;
                            else posPref.get(q)[optionPos] = 0.0;
                            break;
                        case LAST:
                            if (optionPos==posPref.get(q).length-1)
                                posPref.get(q)[optionPos] = 1.0;
                            else posPref.get(q)[optionPos] = 0.0;
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private List<Component> selectOptions(int i, Component[] options){
        List<Component> retval = new ArrayList<Component>();
        if (i >= options.length) {
            String binstr = Integer.toBinaryString(i);
            assert binstr.length() <= options.length : String.format("binary string : %s; total option size : %d", binstr, options.length);
            char[] selections = StringUtils.leftPad(binstr, options.length, '0').toCharArray();
            assert i < Math.pow(2.0, (double) options.length);
            assert selections.length == options.length : String.format("Told to select from %d options; actual number of options is %d", selections.length, options.length);
            for (int j = 0 ; j < selections.length ; j++){
                if (selections[j]=='1')
                    retval.add(options[j]);
            }
        } else retval.add(options[i]);
        return retval;
    }

    private void populateResponses() throws SurveyException {
        Interpreter interpreter = new Interpreter(survey);
        do {
            Question q = interpreter.getNextQuestion();
            Component[] c = q.getOptListByIndex();
            List<Component> answers = new ArrayList<Component>();
            // calculate our answer
            int denom = getDenominator(q);
            if (q.freetext || denom < 1 ) {
                answers.add(new StringComponent(generateStringComponent(q), -1, -1));
            } else {
                double prob = rng.nextDouble();
                double cumulativeProb = 0.0;
                for (int j = 0 ; j < denom ; j++) {
                    assert posPref.get(q).length == denom :
                            String.format("Expected position preference question options and denom to be equal (%d = %d)",
                            posPref.get(q).length, denom);
                    cumulativeProb += posPref.get(q)[j];
                    if (prob < cumulativeProb) {
                        answers.addAll(selectOptions(j, c));
                        break;
                    }
                }
            }
            interpreter.answer(q, answers);
        } while (!interpreter.terminated());
        this.response = interpreter.getResponse();
        this.response.setKnownValidityStatus(KnownValidityStatus.NO);
    }

}

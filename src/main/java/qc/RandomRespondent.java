package qc;

import survey.*;
import system.Gensym;

import java.util.*;

public class RandomRespondent {

    public enum AdversaryType { UNIFORM, INNER, FIRST, LAST }

    public static final Gensym gensym = new Gensym("rand");
    protected static final Random rng = new Random();

    public final Survey survey;
    public final AdversaryType adversaryType;
    public final String id = gensym.next();
    public SurveyResponse response = null;
    private double[][] posPref;
    private final double UNSET = -1.0;

    public RandomRespondent(Survey survey, AdversaryType adversaryType) throws SurveyException {
        this.survey = survey;
        this.adversaryType = adversaryType;
        posPref = new double[survey.questions.size()][];
        for (int i = 0 ; i < survey.questions.size() ; i++) {
            Question q = survey.questions.get(i);
            int denom = getDenominator(q);
            posPref[i] = new double[denom];
            Arrays.fill(posPref[i], UNSET);
        }
        populatePosPreferences();
        populateResponses();
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
                            posPref[questionPos][optionPos] = (1.0 / (double) posPref[questionPos].length);
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

    private List<SurveyResponse.OptTuple> getOptTuple(Question q, int k) throws SurveyException {
        List<SurveyResponse.OptTuple> retval = new ArrayList<SurveyResponse.OptTuple>();
        Component[] options = q.getOptListByIndex();
        if (q.exclusive)
            retval.add(new SurveyResponse.OptTuple(options[k], k));
        else {
            int denom = getDenominator(q);
            for ( int i = 1 ; i < denom ; i++ ) {
                String s = Integer.toBinaryString(i);
                for ( int j = 0 ; j < s.length() ; j++ )
                    if ( s.charAt(j) == '1' )
                        retval.add(new SurveyResponse.OptTuple(options[j], j));
            }
        }
        return retval;
    }

    public int getDenominator(Question q){
        // if the question is not exclusive, get the power set minus one, since they can't answer with zero.
        return q.exclusive ? q.options.size() : (int) Math.pow(2.0, q.options.size()) - 1;
    }

    private void populateResponses() throws SurveyException {
        SurveyResponse sr = new SurveyResponse(id);
        sr.real = false;
        for (int i = 0 ; i < survey.questions.size() ; i++) {
            Question q = survey.questions.get(i);
            int denom = getDenominator(q);
            if (q.freetext || denom < 2 )
                continue;
            double prob = rng.nextDouble();
            double cumulativeProb = 0.0;
            SurveyResponse.QuestionResponse qr = new SurveyResponse.QuestionResponse(survey, q.quid, q.index);
            for (int j = 0 ; j < denom ; j++) {
                cumulativeProb += posPref[i][j];
                if (prob < cumulativeProb) {
                    List<SurveyResponse.OptTuple> choices = getOptTuple(q, j); //new SurveyResponse.OptTuple(options[j], j);
                    for (SurveyResponse.OptTuple choice : choices)
                        qr.add(q.quid, choice, null);
                    qr.indexSeen = j;
                    sr.responses.add(qr);
                    System.out.println(String.format("index seen : %d\tprob : %f\tcumulative prob : %f", qr.indexSeen, prob, cumulativeProb));
                    break;
                }
            }
            assert qr.opts.size() > 0 : String.format("Did not add question response (%s) to this survey response (%s) for survey %s. %d"
                    , qr.q.toString(), sr.srid, survey.sourceName, denom);
        }
        this.response = sr;
    }

    public static AdversaryType selectAdversaryProfile(QCMetrics qcMetrics) {
        int totalAdversaries = 0;
        for (Integer i : qcMetrics.adversaryComposition.values()) {
            totalAdversaries += i;
        }
        int which = rng.nextInt(totalAdversaries);
        for (Map.Entry<AdversaryType, Integer> entry : qcMetrics.adversaryComposition.entrySet()) {
            if (which < entry.getValue())
                return entry.getKey();
        }
        return null;
    }

}

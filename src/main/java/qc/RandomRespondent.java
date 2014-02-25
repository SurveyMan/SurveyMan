package qc;

import org.apache.log4j.Logger;
import sun.reflect.generics.reflectiveObjects.LazyReflectiveObjectGenerator;
import survey.*;
import system.Gensym;

import java.util.*;

public class RandomRespondent {

    public enum AdversaryType { UNIFORM, INNER, FIRST, LAST }
    public static final Logger LOGGER = Logger.getLogger("qc");

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

    private Block branchTo, currentBlock;
    private List<Question> topLevelQuestionsForBlock;
    int toplevelblocks;
    boolean branched = false;

    private Question getNextQuestion(Question lastQuestion, Component answers) throws Survey.BlockNotFoundException {

        if (currentBlock==null) {
            currentBlock = lastQuestion.getFurthestAncestor(survey);
            topLevelQuestionsForBlock = currentBlock.getAllQuestions();
            topLevelQuestionsForBlock.remove(0);
            toplevelblocks = 1;
        }

        Question q = null;

        switch (currentBlock.branchParadigm) {
            case NONE :
                // if the branch map is empty, just move to the next question in the block, or the next block
                if (topLevelQuestionsForBlock.isEmpty())
                    // if toplevelquetsionsforblock is empty, move to the next block
                    if (branchTo!=null) {
                        // if branchTo is not null, we branched
                        currentBlock = branchTo;
                        topLevelQuestionsForBlock = branchTo.getAllQuestions();
                        branchTo = null;
                        toplevelblocks++;
                        q = topLevelQuestionsForBlock.remove(0);
                    } else {
                        // otherwise, we just take the next block in order
                        try {
                            currentBlock = survey.getBlockById(new int[]{ currentBlock.getBlockId()[0]+1 });
                            topLevelQuestionsForBlock = currentBlock.getAllQuestions();
                            toplevelblocks++;
                            q = topLevelQuestionsForBlock.remove(0);
                        } catch (Survey.BlockNotFoundException e) {
                            if (!branched)
                                assert(toplevelblocks==survey.topLevelBlocks.size()) :
                                    String.format("Counted %d top level blocks, but survey %s has %d top level blocks"
                                            , toplevelblocks, survey.sourceName, survey.topLevelBlocks.size());
                            LOGGER.info(e);
                        }
                    }
                else q = topLevelQuestionsForBlock.remove(0);
                break;
            case ALL :
                branched = true;
                // leave branchTo alone, change blocks
                currentBlock = (Block) lastQuestion.branchMap.get(answers);
                toplevelblocks++;
                topLevelQuestionsForBlock = currentBlock.getAllQuestions();
                q = topLevelQuestionsForBlock.remove(0);
                break;
            case ONE:
                branched = true;
                if (currentBlock.branchQ.equals(lastQuestion))
                    branchTo = lastQuestion.branchMap.get(answers);
                if (topLevelQuestionsForBlock.isEmpty()) {
                    currentBlock = branchTo;
                    branchTo = null;
                    topLevelQuestionsForBlock = currentBlock.getAllQuestions();
                    toplevelblocks++;
                } else
                    q = topLevelQuestionsForBlock.remove(0);
                break;
        }
        return q;
    }

    private void populateResponses() throws SurveyException {
        SurveyResponse sr = new SurveyResponse(id);
        sr.real = false;
        // get the first question
        Question q = survey.getQuestionsByIndex()[0];
        Component a = null;
        int i = 0;
        // loop through questions in the top level block
        do {
            int denom = getDenominator(q);
            if (q.freetext || denom < 2 )
                continue;
            double prob = rng.nextDouble();
            double cumulativeProb = 0.0;
            SurveyResponse.QuestionResponse qr = new SurveyResponse.QuestionResponse(survey, q.quid, q.index);
            for (int j = 0 ; j < denom ; j++) {
                cumulativeProb += posPref[i][j];
                if (prob < cumulativeProb) {
                    List<SurveyResponse.OptTuple> choices = getOptTuple(q, j);
                    if (choices.size() == 1)
                        a = choices.get(0).c;
                    for (SurveyResponse.OptTuple choice : choices)
                        qr.add(q.quid, choice, null);
                    qr.indexSeen = i;
                    sr.responses.add(qr);
                    System.out.println(String.format("question id : %s index seen : %d\tprob : %f\tcumulative prob : %f"
                            , q.quid, qr.indexSeen, prob, cumulativeProb));
                    break;
                }
            }
            // if this was a branch question, advance to the appropriate block
            assert qr.opts.size() > 0 : String.format("Did not create options for question response (%s) with question id (%s) to this survey response for survey %s.\n" +
                    "num options : %d\tprob : %f\tcumulativeProb : %f"
                    , q.quid, qr.q.toString(), survey.sourceName, denom, prob, cumulativeProb);
            i++;
        } while ((q=getNextQuestion(q, a)) != null);
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

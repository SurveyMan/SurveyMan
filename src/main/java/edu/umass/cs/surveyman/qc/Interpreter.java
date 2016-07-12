package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.MersenneRandom;

import java.util.*;

public class Interpreter {
    private static class QuestionResponse implements IQuestionResponse {

        private final List<Question> questions;
        private final Map.Entry<Question, List<SurveyDatum>> e;

        public QuestionResponse(Map<Question, List<SurveyDatum>> responseMap, Map.Entry<Question, List<SurveyDatum>> e) {
            this.questions = new ArrayList<>(responseMap.keySet());
            this.e = e;
        }

        @Override
        public Question getQuestion() {
            return e.getKey();
        }

        @Override
        public List<OptTuple> getOpts() {
            List<OptTuple> retval = new ArrayList<>();
            for (SurveyDatum c : e.getValue()) {
                retval.add(new OptTuple(c, c.getIndex()));
            }
            return retval;
        }

        @Override
        public int getIndexSeen() {
            return questions.indexOf(e.getKey());
        }

        @Override
        public SurveyDatum getAnswer() throws SurveyException
        {
            if (this.getQuestion().exclusive)
                return this.getOpts().get(0).c;
            else throw new RuntimeException("Cannot call getAnswer() on non-exclusive questions. Try getAnswers() instead.");
        }

        @Override
        public List<SurveyDatum> getAnswers() throws SurveyException
        {
            if (this.getQuestion().exclusive)
                throw new RuntimeException("Cannot call getAnswers() on exclusive questions. Try getAnswer() instead.");
            List<SurveyDatum> answers = new ArrayList<>();
            for (OptTuple optTuple : this.getOpts())
                answers.add(optTuple.c);
            return answers;
        }

        @Override
        public int compareTo(Object o)
        {
            if (o instanceof IQuestionResponse) {
                IQuestionResponse that = (IQuestionResponse) o;
                return this.getQuestion().compareTo(that.getQuestion());
            } else throw new RuntimeException(String.format("Cannot compare classes %s and %s",
                    this.getClass().getName(), o.getClass().getName()));
        }
    }
    // emulates the JS interpreter. We use this class to simulate the survey
    public Survey survey;
    private ArrayList<Block> topLevelBlockStack;
    private ArrayList<Question> questionStack;
    private Block branchTo = null;
    private Map<Question, List<SurveyDatum>> responseMap = new HashMap<>();
    private List<Question> questionList = new ArrayList<>();
    private MersenneRandom random = new MersenneRandom();

    /**
     * Constructs an interpreter for a given survey.
     * @param survey The survey we would like a respondent to take.
     */
    public Interpreter(Survey survey) {
        this.survey = survey;
        this.topLevelBlockStack = new ArrayList<>(getShuffledTopLevel(survey));
        assert(!this.topLevelBlockStack.isEmpty());
        this.questionStack = new ArrayList<>(getQuestionsForBlock(topLevelBlockStack.remove(0)));
        assert(!this.questionStack.isEmpty());
    }

    /**
     * Returns an empty survey response.
     * @return SurveyResponse object.
     * @throws SurveyException
     */
    public SurveyResponse getResponse() throws SurveyException {
        final Map<Question, List<SurveyDatum>> responseMap = this.responseMap;
        final List<IQuestionResponse> questionResponses = new ArrayList<>();
        for (final Map.Entry<Question, List<SurveyDatum>> e : responseMap.entrySet()) {
            questionResponses.add(new QuestionResponse(responseMap, e));
        }
        return new SurveyResponse(survey, questionResponses, SurveyResponse.gensym.next(), 0.0, 0.0, KnownValidityStatus.MAYBE);
    }

    /**
     * Answers question q with some list of answers.
     * @param q Question of interest.
     * @param aList List of components (i.e., valid answer(s) to the input question) .
     * @throws SurveyException
     */
    public void answer(Question q, List<SurveyDatum> aList) throws SurveyException {
        responseMap.put(q, aList);
        questionList.add(q);
        if (q.isBranchQuestion()){
            //assert branchTo==null : String.format("branchTo set to block %s when setting branching for question %s", branchTo.strId, q);
            branchTo = q.getBranchDest(aList.get(0));
        }
    }

    /**
     * Returns the next question, according to the status of the interpreter.
     * @return The next question that needs to be answered.
     * @throws SurveyException
     */
    public Question getNextQuestion() throws SurveyException {
        Question next = nextQ();
        // shuffle option indices
        SurveyDatum[] options = next.getOptListByIndex();
        if (next.randomize)
            if (next.ordered) {
                if (random.nextBoolean())
                    for (int i = 0 ; i < options.length/2 ; i++) {
                        SurveyDatum foo = options[i];
                        options[i] = options[options.length - i - 1];
                        options[options.length - i - 1] = foo;
                    }
            } else {
                random.shuffle(options);
            }
        for (int i = 0 ; i < options.length ; i++)
            options[i].setIndex(i);
        return next;
    }

    private Question nextQ()
    {

        if (!questionStack.isEmpty())
            return questionStack.remove(0);

        Block top = topLevelBlockStack.get(0);

        if (top.isRandomized() || branchTo==null){
            Block pop = topLevelBlockStack.remove(0);
            questionStack = getQuestionsForBlock(pop);
            assert questionStack.size() > 0 : String.format("Survey %s in error : block %s has no questions", survey.sourceName, pop.getId());
            return questionStack.remove(0);
        } else if (top.equals(branchTo)) {
            questionStack = getQuestionsForBlock(topLevelBlockStack.remove(0));
            branchTo = null;
            return questionStack.get(0);
        } else {
            topLevelBlockStack.remove(0);
            return nextQ();
        }

    }

    /**
     * Indicates whether we have reached a terminal node in the survey graph.
     * @return boolean indicating whether we've reached a terminal node.
     */
    public boolean terminated(){
        return topLevelBlockStack.size()==0 && questionStack.size()==0;
    }

    private ArrayList<Question> getQuestionsForBlock(Block block){
        SurveyObj[] contents = getShuffledComponents(block);
        assert contents.length > 0 : String.format("Contents of block %s in survey %s is %d", block.getId(), survey.sourceName, contents.length);
        ArrayList<Question> retval = new ArrayList<>();
        for (SurveyObj content : contents) {
            if (content instanceof Question)
                retval.add((Question) content);
            else if (content instanceof Block) {
                Block b = (Block) content;
                if (b.getBranchParadigm().equals(Block.BranchParadigm.ALL))
                    retval.add(b.questions.get(random.nextInt(b.questions.size())));
                else retval.addAll(getQuestionsForBlock(b));
            } else
                throw new RuntimeException(String.format("Block %s has unknown type %s", block.getId(), content
                        .getClass()));
        }
        return retval;
    }

    private SurveyObj[] getShuffledComponents(Block block){
        int size = block.questions.size() + block.subBlocks.size();
        assert size > 0 : String.format("Block %s in survey %s has no contents", block.getId(), survey.sourceName);
        SurveyObj[] retval = new SurveyObj[size];
        List<Block> randomizable = new ArrayList<>();
        List<Block> nonRandomizable = new ArrayList<>();
        for (Block b : block.subBlocks)
            if (b.isRandomized())
                randomizable.add(b);
            else nonRandomizable.add(b);
        // get the number of randomizable components
        // generate our index list
        Integer[] allIndices = new Integer[size];
        for (int i = 0 ; i < size ; i++)
            allIndices[i] = i;
        // shuffle
        random.shuffle(allIndices);
        // select locations
        List<Integer> qIndices = Arrays.asList(allIndices).subList(0, block.questions.size());
        List<Integer> bIndices = Arrays.asList(allIndices).subList(block.questions.size(), block.questions.size() + randomizable.size());
        // fill retval at these indices
        for (int i = 0 ; i < qIndices.size() ; i++)
            retval[qIndices.get(i)] = block.questions.get(i);
        for (int i = 0 ; i < bIndices.size() ; i++)
            retval[bIndices.get(i)] = randomizable.get(i);
        for (int i = 0 ; i < retval.length ; i++)
            if (retval[i]==null)
                retval[i] = nonRandomizable.remove(0);
        return retval;
    }

    private List<Block> getShuffledTopLevel(Survey survey) {
        return Arrays.asList(Block.shuffle(survey.topLevelBlocks));
    }

    /**
     * Partitions the top-level blocks in the survey into those that are floating and those that are static. Values at
     * key Boolean.TRUE are floating; values are Boolean.FALSE are static.
     * @param survey The survey whose top-level blocks we want to partition.
     * @return The partition.
     */
    public static Map<Boolean, List<Block>> partitionBlocks(Survey survey) {
        Map<Boolean, List<Block>> retval = new HashMap<>();
        List<Block> rand = new ArrayList<>();
        List<Block> nonRand = new ArrayList<>();
        for (Block b : survey.topLevelBlocks)
            if (b.isRandomized())
                rand.add(b);
            else nonRand.add(b);
        retval.put(true, rand);
        retval.put(false, nonRand);
        return retval;
    }



}

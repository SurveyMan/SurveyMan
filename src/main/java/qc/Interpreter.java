package qc;

import interstitial.ISurveyResponse;
import interstitial.IQuestionResponse;
import interstitial.OptTuple;
import survey.*;
import survey.exceptions.SurveyException;
import util.Gensym;

import java.io.Reader;
import java.util.*;

public class Interpreter {
    // emulates the JS interpreter. We use this class to simulate the survey


    public Survey survey;
    private ArrayList<Block> topLevelBlockStack;
    private ArrayList<Question> questionStack;
    private Block branchTo = null;
    private Map<Question, List<Component>> responseMap = new HashMap<Question, List<Component>>();
    public static final Random random = new Random(System.currentTimeMillis());

    public Interpreter(Survey survey){
        this.survey = survey;
        this.topLevelBlockStack = new ArrayList<Block>(getShuffledTopLevel(survey));
        assert(!this.topLevelBlockStack.isEmpty());
        this.questionStack = new ArrayList<Question>(getQuestionsForBlock(topLevelBlockStack.remove(0)));
        assert(!this.questionStack.isEmpty());
    }

    public ISurveyResponse getResponse() throws SurveyException {
        final Map<Question, List<Component>> responseMap = this.responseMap;
        final Gensym gensym = new Gensym("sr");
        return new ISurveyResponse() {
            String srid = gensym.next();
            @Override
            public List<IQuestionResponse> getResponses() {
                List<IQuestionResponse> retval = new ArrayList<IQuestionResponse>();
                for (final Map.Entry<Question, List<Component>> e : responseMap.entrySet()) {
                    retval.add(new IQuestionResponse() {
                        List<Question> questions = new ArrayList<Question>(responseMap.keySet());
                        @Override
                        public Question getQuestion() {
                            return e.getKey();
                        }
                        @Override
                        public List<OptTuple> getOpts() {
                            List<OptTuple> retval = new ArrayList<OptTuple>();
                            for (Component c : e.getValue()){
                                retval.add(new OptTuple(c, c.index));
                            }
                            return retval;
                        }
                        @Override
                        public int getIndexSeen() {
                            return questions.indexOf(e.getKey());
                        }
                    });
                }
                return retval;
            }

            @Override
            public void setResponses(List<IQuestionResponse> responses) {

            }

            @Override
            public boolean isRecorded() {
                return false;
            }
            @Override
            public String srid() {
                return srid;
            }
            @Override
            public void setSrid(String srid) {
                this.srid = srid;
            }
            @Override
            public String workerId() {
                return srid;
            }
            @Override
            public void setRecorded(boolean recorded) {

            }
            @Override
            public Map<String, IQuestionResponse> resultsAsMap() {
                Map<String, IQuestionResponse> retval = new HashMap<String, IQuestionResponse>();
                for (final Map.Entry<Question, List<Component>> e : responseMap.entrySet()) {
                    retval.put(e.getKey().quid, new IQuestionResponse() {
                        List<Question> questions = new ArrayList<Question>(responseMap.keySet());
                        @Override
                        public Question getQuestion() {
                            return e.getKey();
                        }
                        @Override
                        public List<OptTuple> getOpts() {
                            List<OptTuple> retval = new ArrayList<OptTuple>();
                            for (Component c : e.getValue()) {
                                retval.add(new OptTuple(c, c.index));
                            }
                            return retval;
                        }
                        @Override
                        public int getIndexSeen() {
                            return questions.indexOf(e.getKey());
                        }
                    });
                }
                return retval;
            }

            @Override
            public List<ISurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException {
                return null;
            }

            @Override
            public void setScore(double score) {

            }

            @Override
            public double getScore() {
                return 0;
            }

            @Override
            public void setThreshold(double pval) {

            }

            @Override
            public double getThreshold() {
                return 0;
            }

        };
    }

    public void answer(Question q, List<Component> aList) {
        responseMap.put(q, aList);
        if (!q.branchMap.isEmpty()){
            //assert branchTo==null : String.format("branchTo set to block %s when setting branching for question %s", branchTo.strId, q);
            branchTo = q.branchMap.get(aList.get(0));
        }
    }

    public Question getNextQuestion() throws SurveyException {
        Question next = nextQ();
        // shuffle option indices
        Component[] options = next.getOptListByIndex();
        if (next.randomize)
            if (next.ordered) {
                if (random.nextBoolean())
                    for (int i = 0 ; i < options.length/2 ; i++) {
                        Component foo = options[i];
                        options[i] = options[options.length - i - 1];
                        options[options.length - i - 1] = foo;
                    }
            } else {
                List<Component> stuff = Arrays.asList(options);
                Collections.shuffle(stuff);
                options = stuff.toArray(options);
            }
        for (int i = 0 ; i < options.length ; i++)
            options[i].index = i;
        return next;
    }

    private Question nextQ() {

        if (!questionStack.isEmpty())
            return questionStack.remove(0);

        Block top = topLevelBlockStack.get(0);

        if (top.isRandomized() || branchTo==null){
            Block pop = topLevelBlockStack.remove(0);
            questionStack = getQuestionsForBlock(pop);
            assert questionStack.size() > 0 : String.format("Survey %s in error : block %s has no questions", survey.sourceName, pop.getStrId());
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

    public boolean terminated(){
        return topLevelBlockStack.size()==0 && questionStack.size()==0;
    }

    private ArrayList<Question> getQuestionsForBlock(Block block){
        SurveyObj[] contents = getShuffledComponents(block);
        assert contents.length > 0 : String.format("Contents of block %s in survey %s is %d", block.getStrId(), survey.sourceName, contents.length);
        ArrayList<Question> retval = new ArrayList<Question>();
        for (int i = 0 ; i < contents.length ; i++) {
            if (contents[i].getClass().equals(Question.class))
                retval.add((Question) contents[i]);
            else if (contents[i].getClass().equals(Block.class)) {
                Block b = (Block) contents[i];
                if (b.branchParadigm.equals(Block.BranchParadigm.ALL))
                    retval.add(b.questions.get(random.nextInt(b.questions.size())));
                else retval.addAll(getQuestionsForBlock(b));
            } else throw new RuntimeException(String.format("Block %s has unknown type %s", block.getStrId(), contents[i].getClass()));
        }
        return retval;
    }

    private SurveyObj[] getShuffledComponents(Block block){
        int size = block.questions.size() + block.subBlocks.size();
        assert size > 0 : String.format("Block %s in survey %s has no contents", block.getStrId(), survey.sourceName);
        SurveyObj[] retval = new SurveyObj[size];
        List<Block> randomizable = new ArrayList<Block>();
        List<Block> nonRandomizable = new ArrayList<Block>();
        for (Block b : block.subBlocks)
            if (b.isRandomized())
                randomizable.add(b);
            else nonRandomizable.add(b);
        // get the number of randomizable components
        // generate our index list
        List<Integer> allIndices = new ArrayList<Integer>();
        for (int i = 0 ; i < size ; i++)
            allIndices.add(i);
        // shuffle
        Collections.shuffle(allIndices);
        // select locations
        List<Integer> qIndices = allIndices.subList(0, block.questions.size());
        List<Integer> bIndices = allIndices.subList(block.questions.size(), block.questions.size() + randomizable.size());
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


    public static Map<Boolean, List<Block>> partitionBlocks(Survey survey) {
        Map<Boolean, List<Block>> retval = new HashMap<Boolean, List<Block>>();
        List<Block> rand = new ArrayList<Block>();
        List<Block> nonRand = new ArrayList<Block>();
        for (Block b : survey.topLevelBlocks)
            if (b.isRandomized())
                rand.add(b);
            else nonRand.add(b);
        retval.put(true, rand);
        retval.put(false, nonRand);
        return retval;
    }

}

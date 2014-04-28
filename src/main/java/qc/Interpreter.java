package qc;

import survey.*;
import survey.exceptions.SurveyException;

import java.util.*;

public class Interpreter {
    // emulates the JS interpreter. We use this class to simulate the survey


    public Survey survey;
    private ArrayList<Block> topLevelBlockStack;
    private ArrayList<Question> questionStack;
    private Block branchTo = null;
    private Map<Question, List<Component>> responseMap = new HashMap<Question, List<Component>>();
    private Random random = new Random();

    public Interpreter(Survey survey){
        this.survey = survey;
        topLevelBlockStack = new ArrayList<Block>(Arrays.asList(getShuffledTopLevel(survey)));
        questionStack = new ArrayList<Question>(getQuestionsForBlock(topLevelBlockStack.remove(0)));
    }

    public ISurveyResponse getResponse() throws SurveyException {
        final Survey s = this.survey;
        final Map<Question, List<Component>> responseMap = this.responseMap;
        final Gensym gensym = new Gensym("sr");
        return new ISurveyResponse() {
            String srid = gensym.next();
            @Override
            public List<IQuestionResponse> getResponses() {
                List<IQuestionResponse> retval = new ArrayList<IQuestionResponse>();
                for (final Map.Entry<Question, List<Component>> e : responseMap.entrySet()) {
                    retval.add(new IQuestionResponse() {
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
                            return e.getKey().index;
                        }
                    });
                }
                return retval;
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
            public String outputResponse(Survey survey, String sep) {
                return null;
            }

            @Override
            public void setRecorded(boolean recorded) {

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
        next.index = responseMap.size();
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
        for (int i = 0; i < retval.size() ; i++)
            retval.get(i).index = i;
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

    private Block[] getShuffledTopLevel(Survey survey) {
        Block[] retval = new Block[survey.topLevelBlocks.size()];
        Map<Boolean, List<Block>> partition = partitionBlocks(survey);
        List<Integer> lst = new ArrayList<Integer>();
        for (int i = 0 ; i < retval.length ; i ++)
            lst.add(i);
        Collections.shuffle(lst);
        List<Integer> indices = lst.subList(0, partition.get(true).size());
        List<Block> randomizable = partition.get(true);
        for (int i = 0 ; i < indices.size() ; i++)
            retval[indices.get(i)] = randomizable.get(i);
        List<Block> nonRandomizable = Block.sort(partition.get(false));
        for (int i = 0 ; i < retval.length ; i++)
            if (retval[i]==null)
                retval[i] = nonRandomizable.remove(0);
        assert(nonRandomizable.isEmpty());
        for (int i = 0 ; i < retval.length ; i++)
            retval[i].index = i;
        return retval;
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

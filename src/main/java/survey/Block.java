package survey;

import csv.CSVParser;
import system.Bug;
import system.Debugger;
import utils.Gensym;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block {


    public static class BlockContiguityException extends SurveyException implements Bug {
        Object caller;
        Method lastAction;

        public BlockContiguityException(int is, int shouldBe, Block parser, Method lastAction) {
            super(String.format("Gap in question index; is %s, should be %s.", is, shouldBe));
            this.caller = parser;
            this.lastAction = lastAction;
            Debugger.addBug(this);
        }

        BlockContiguityException(Question q0, Question q1, Block parser, Method lastAction) {
            super(String.format("Gap in question index between %s and %s", q0.toString(), q1.toString()));
            this.caller = parser;
            this.lastAction = lastAction;
            Debugger.addBug(this);
        }

        @Override
        public Object getCaller() {
            return caller;
        }

        @Override
        public Method getLastAction() {
            return lastAction;
        }
    }

    public static class MultBranchPerBlockException extends SurveyException implements Bug{
        Object caller;
        Method lastAction;

        public MultBranchPerBlockException(Block b, CSVParser parser, Method lastAction) {
            super(String.format("Block %s contains more than one branch question.", b.strId));
            this.caller = parser;
            this.lastAction = lastAction;
            Debugger.addBug(this);
        }

        @Override
        public Object getCaller() {
            return caller;
        }

        @Override
        public Method getLastAction() {
            return lastAction;
        }
    }

    private static final Gensym gensym = new Gensym("block");
    public final String bid = gensym.next();

    public String strId;
    // source lines come from the questions
    public List<Integer> sourceLines = new ArrayList<Integer>();
    public List<Question> questions = new ArrayList<Question>();
    // each block is allowed one branch question
    public Question branchQ = null;
    public ArrayList<Block> subBlocks = null;
    public Block parent = null;
    public int blockIndex = -1; //only used if randomize is true; is this block's index in its parent block
    private boolean randomize = false;
    private int[] id = null;

    public void setRandomizeFlagToTrue () {
        this.randomize = true;
        blockIndex = id [id.length-1];
    }

    public boolean isRandomized() {
        return this.randomize;
    }

    public void setIdArray(int[] id) {
        this.id = id;
    }

    public void sort() throws SurveyException {
        // more stupid sort

        for (int i = 1; i < questions.size() ; i ++) {
            Question a = questions.get(i-1);
            Question b = questions.get(i);
            if (a.index > b.index) {
                questions.set(i-1, b);
                questions.set(i, a);
                if (i>1) i-=2; 
            }
        }

        int base = questions.get(0).index, j = 0;

        for (int i = 1 ; i < questions.size() ; i++) {
            int thisIndex = questions.get(i).index;
            if (i+base != thisIndex)
                if (subBlocks!=null)
                    for (Block b : subBlocks.subList(j,subBlocks.size())) {
                        j+=1;
                        int jumpIndex = i + base + b.blockSize();
                        if (jumpIndex == thisIndex)
                            break;
                        else if (jumpIndex > thisIndex)
                            throw new BlockContiguityException(questions.get(i-1), questions.get(i), this, this.getClass().getEnclosingMethod());
                    }
                else throw new BlockContiguityException(questions.get(i-1), questions.get(i), this, this.getClass().getEnclosingMethod());
        }
    }

    public int blockSize(){
        //re-implement this is non-recursive later
        int size = questions.size();
        if (subBlocks!=null)
            for (Block b : subBlocks)
                size += b.blockSize();
        return size;
    }
    
    public void randomize() throws SurveyException{
        sort();
        Question[] qs = questions.toArray(new Question[questions.size()]);
        for (int i = qs.length ; i > 0 ; i--){
            int j = Question.rng.nextInt(i);
            int k = qs[j].index;
            qs[j].index = qs[i-1].index;
            qs[i-1].index = k;
        }
        for (Question q : qs)
            q.randomize();
        sort();
        // if there is a branch question, put it at the end by swapping indices with the last
        // question post sort
        if (branchQ != null) {
            Question lastQuestion = questions.get(questions.size()-1);
            int lastIndex = lastQuestion.index;
            lastQuestion.index = branchQ.index;
            branchQ.index = lastIndex;
            sort();
        }
        if (subBlocks != null)
            for (Block b : subBlocks)
                b.randomize();
    }
    
    public boolean equals(Block b) {
        return Arrays.equals(this.id, b.id);
    }
    
   @Override
    public String toString() {
        String indent = "";
        if (id!=null) {
            for (int i = 0 ; i < id.length ; i++)
                indent += "\t";
        }
        indent = "\n" + indent;
        String str = strId + ":" + indent;
        for (Question q : questions)
            str = str + "\n" + indent + q.toString();
        if (subBlocks!=null) {
            for (int i = 0 ; i < subBlocks.size(); i ++)
                str = str + subBlocks.get(i).toString();
        }
        return str;
    }
   
}

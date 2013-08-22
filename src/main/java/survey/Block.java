package survey;

import utils.Gensym;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Block {

    private static final Gensym gensym = new Gensym("block");
    public final String bid = gensym.next();

    public String strId;
    public int[] id = null;
    // source lines come from the questions
    public List<Integer> sourceLines = new ArrayList<Integer>();
    public List<Question> questions = new ArrayList<Question>();
    // each block is allowed one branch question
    public Question branchQ = null;
    public ArrayList<Block> subBlocks = null;

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
        int base = questions.get(0).index;
        for (int i = 1 ; i < questions.size() ; i++) {
            int thisIndex = questions.get(i).index;
            if (i+base != thisIndex)
                throw new BlockContiguityException(questions.get(i-1), questions.get(i));
        }
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
            System.out.println(lastQuestion.quid+"'s index:"+lastQuestion.index+"\t"+branchQ.quid+"'s index:"+branchQ.index);
            lastQuestion.index = branchQ.index;
            branchQ.index = lastIndex;
            System.out.println(lastQuestion.quid+"'s index:"+lastQuestion.index+"\t"+branchQ.quid+"'s index:"+branchQ.index);
            sort();
        }
        if (subBlocks != null)
            for (Block b : subBlocks)
                b.randomize();
    }
   
    public void ensureOneBranch()  throws SurveyException {
        boolean branch = false;
        for (Question q : questions) {
            if (branch && q.branchMap.size() > 0)
                throw new MultBranchPerBlockException(this);
            else if (!branch && q.branchMap.size() > 0)
                branch = true;
        }
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

class BlockContiguityException extends SurveyException {

    public BlockContiguityException(int is, int shouldBe) {
        super(String.format("Gap in question index; is %s, should be %s.", is, shouldBe));
    }

    BlockContiguityException(Question q0, Question q1) {
        super(String.format("Gap in question index between %s and %s", q0.toString(), q1.toString()));
    }
    
}

class MultBranchPerBlockException extends SurveyException {
 
    public MultBranchPerBlockException(Block b) {
        super(String.format("Block %s contains more than one branch question.", b.strId));
    }
}
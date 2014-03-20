package survey;

import csv.CSVParser;
import org.apache.commons.lang.StringUtils;
import system.Bug;
import system.Debugger;
import system.Rules;

import java.lang.reflect.Method;
import java.util.*;

public class Block extends SurveyObj{

    public enum BranchParadigm { SAMPLE, NONE, ONE; }

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

    public String strId;
    // source lines come from the questions
    public List<Integer> sourceLines = new ArrayList<Integer>();
    public List<Question> questions = new ArrayList<Question>();
    // each top-level block is allowed one branch question
    public Question branchQ = null;
    public BranchParadigm branchParadigm = BranchParadigm.NONE;
    public List<Block> subBlocks = new ArrayList<Block>();
    public Block parentBlock;
    private boolean randomize = false;
    protected int[] id = null;
    
    public Block() {
      
    }
    
    public Block(String strId) {
        this.id = Block.idToArray(strId);
        this.strId = strId;
    }

    public static int[] idToArray(String strId) {
        String[] pieces = strId.split("\\.");
        int[] retval = new int[pieces.length];
        for (int i = 0 ; i < pieces.length ; i ++) {
            String s = pieces[i].startsWith("_") ? pieces[i].substring(1) : pieces[i];
            retval[i] = Integer.parseInt(s);
        }
        return retval;
    }

    public String getParentStrId() {
        String[] pieces = this.strId.split("\\.");
        String[] parentStuff = Arrays.copyOfRange(pieces, 0, pieces.length - 1);
        return StringUtils.join(parentStuff, ".");
    }

    public static String idToString(int[] id){
        String s = Integer.toString(id[0]);
        for (int i = 1 ; i < id.length ; i++)
            s += "." + Integer.toString(id[i]);
        return s;
    }

    public void propagateBranchParadigm() throws SurveyException {

        if (parentBlock==null) return;

        if (branchParadigm.equals(BranchParadigm.ONE)) {
            parentBlock.branchParadigm = BranchParadigm.ONE;
            parentBlock.propagateBranchParadigm();
        }

        Block branchBlock = null;

        for (Block b : parentBlock.subBlocks) {
            switch (b.branchParadigm) {
                case ONE:
                    if (branchBlock!=null)
                        throw new Rules.BlockException(String.format("Block %s has two subblocks with branch ONE paradigm (%s and %s)"
                                , parentBlock.strId
                                , branchBlock.strId
                                , b.strId));
                    else {
                        branchBlock = b;
                        parentBlock.branchParadigm = BranchParadigm.ONE;
                    }
                    break;
                case SAMPLE:
                    if (b.subBlocks.size()!=0)
                        throw new Rules.BlockException(String.format("Block %s with branch SAMPLE paradigm has %d subblocks."
                                , b.strId, subBlocks.size()));
                    for (Question q : b.questions) {
                        if (q.branchMap.size()==0)
                            throw new Rules.BlockException(String.format("Block %s with branch SAMPLE paradigm has non-branhing question %s"
                                    , b.strId, q));
                    }
            }
        }

        if (branchBlock==null)
            parentBlock.branchParadigm = BranchParadigm.NONE;
    }

    public void setRandomizable() {
        String[] pieces = strId.split("\\.");
        if (pieces[pieces.length - 1].startsWith("_"))
            this.randomize = true;
    }

    public boolean removeQuestion(String quid) {
        boolean foundQ = false;
        for (Question q : questions) {
            if (q.quid.equals(quid)){
                foundQ = true;
                questions.remove(q);
                break;
            }
        }
        if (!subBlocks.isEmpty())
            for (Block b : subBlocks)
                b.removeQuestion(quid);
        return foundQ;
    }

    public void setRandomizeFlagToTrue () {
        this.randomize = true;
    }

    public boolean isRandomized() {
        return this.randomize;
    }

    public boolean isTopLevel() {
        return id.length == 1;
    }

    public boolean before(Block that) {
        // checks whether this precedes that
        if (this.isSubblockOf(that) || that.isSubblockOf(this))
            return false;
        return this.id[0] < that.getBlockId()[0];
    }

    public boolean isSubblockOf(Block b) {
        // test whether this is a subblock of b
        int[] yourId = b.getBlockId();
        for (int i = 0 ; i < yourId.length ; i++) {
            if (yourId[i] != this.id[i])
                return false;
        }
        return true;
    }

    public void setIdArray(int[] id) {
        this.id = id;
        this.index = id[id.length-1] - 1;
    }

    public int getBlockDepth(){
        return id.length;
    }

    public int[] getBlockId(){
        return id;
    }
    
    public Question[] getBlockQuestionsByID() {
      Question[] qArray = new Question[questions.size()];
      Collections.sort(questions);
      for (int i = 0 ; i < qArray.length ; i++)
          qArray[i] = questions.get(i);
      return qArray;
    }

    public static List<Block> sort(List<Block> blockList){
        List<Block> retval = new ArrayList<Block>();
        for (Block b : blockList) {
            int i = 0;
            for (Block sorted : retval) {
                if (b.before(sorted))
                    break;
                i++;
            }
            retval.add(i, b);
        }
        return retval;
    }

    public void sort() throws SurveyException {
        // more stupid sort
        Collections.sort(questions);
        Collections.sort(subBlocks);

        if (questions.isEmpty())
          return;
        
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

    public boolean equals(Block b) {
        return Arrays.equals(this.id, b.id);
    }

    public List<Question> getAllQuestions() {
        List<Question> qs = this.questions==null ? new ArrayList<Question>() : new ArrayList<Question>(this.questions);
        if (subBlocks==null)
            return qs;
        for (Block b : subBlocks) {
            qs.addAll(b.getAllQuestions());
        }
        return qs;
    }

    public int dynamicQuestionCount() {
        if (this.branchParadigm.equals(BranchParadigm.SAMPLE))
            return 1;
        int ct = this.questions.size();
        for (Block b : this.subBlocks) {
            ct += b.dynamicQuestionCount();
        }
        return ct;
    }

   @Override
    public String toString() {
        String[] tabs = new String[id.length];
        Arrays.fill(tabs, "\t");
        String indent = StringUtils.join(tabs, "");
        StringBuilder str = new StringBuilder(strId + ":\n" + indent);
        for (Question q : questions)
            str.append("\n" + indent + q.toString());
        if (subBlocks.size() > 0) {
            for (int i = 0 ; i < subBlocks.size(); i ++) {
                Block b = subBlocks.get(i);
                str.append(b.toString());
            }
        }
        return str.toString();
    }
   
}

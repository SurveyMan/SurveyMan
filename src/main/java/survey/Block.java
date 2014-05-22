package survey;

import input.exceptions.BranchException;
import org.apache.commons.lang.StringUtils;
import survey.exceptions.BlockContiguityException;
import survey.exceptions.SurveyException;

import java.util.*;

public class Block extends SurveyObj{

    public enum BranchParadigm {ALL, NONE, ONE; }

    private String strId;
    // source lines come from the questions
    public List<Integer> sourceLines = new ArrayList<Integer>();
    public List<Question> questions = new ArrayList<Question>();
    // each top-level block is allowed one branch question
    public Question branchQ = null;
    public BranchParadigm branchParadigm = BranchParadigm.NONE;
    public List<Block> subBlocks = new ArrayList<Block>();
    public Block parentBlock;
    private String parentStrId;
    private boolean randomize = false;
    private int[] id = null;
    
    public Block() {
      
    }
    
    public Block(String strId) {
        this.id = Block.idToArray(strId);
        this.strId = strId;
        if (isRandomizable(this.strId))
            this.randomize = true;
        this.index = this.id[this.id.length - 1] - 1;
    }

    public String getStrId(){
        return this.strId;
    }

    public void setStrId(String strId){
        this.strId = strId;
    }

    public static boolean isRandomizable(String strId) {
        String[] pieces = strId.split("\\.");
        return !Character.isDigit(pieces[pieces.length - 1].charAt(0));
    }

    public static int[] idToArray(String strId) {
        String[] pieces = strId.split("\\.");
        int[] retval = new int[pieces.length];
        for (int i = 0 ; i < pieces.length ; i ++) {
            String s = isRandomizable(pieces[i]) ? pieces[i].substring(1) : pieces[i];
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
        if (id.length==0)
            return "";
        StringBuilder s = new StringBuilder();
        s.append(id[0]);
        for (int i = 1 ; i < id.length ; i++)
            s.append(".").append(id[i]);
        return s.toString();
    }

    private void propagateUp() throws SurveyException {
        if (parentBlock==null)
            return;

        switch (this.branchParadigm){
            case ONE:
                switch (parentBlock.branchParadigm) {
                    case NONE:
                        parentBlock.branchParadigm = this.branchParadigm;
                        parentBlock.branchQ = this.branchQ;
                        parentBlock.propagateUp();
                        break;
                    case ONE:
                        if (parentBlock.branchQ==null)
                            parentBlock.branchQ = this.branchQ;
                        if (parentBlock.branchQ!=null && !parentBlock.branchQ.equals(this.branchQ))
                            throw new BranchException(String.format("Both block %s and %s are set to paradigm ONE and have unequal branch questions (%s and %s)"
                                    , this.strId, this.parentBlock.strId, this.branchQ, this.parentBlock.branchQ));
                        break;
                    case ALL:
                        throw new BranchException(String.format("Parent block %s is set to ALL; child block %s is set to ONE"
                                , this.parentBlock.strId, this.strId));
                }
            case NONE:
                break;
            case ALL:
                break;
        }
    }

    public void setParentPointer(){
        for (Block b : this.subBlocks){
            if (b.parentBlock==null)
                b.parentBlock = this;
            b.setParentPointer();
        }
    }

    public void propagateBranchParadigm() throws SurveyException {

        if (parentBlock==null) return;

        if (branchParadigm.equals(BranchParadigm.ONE))
            propagateUp();

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
                case ALL:
                    if (b.subBlocks.size()!=0)
                        throw new Rules.BlockException(String.format("Block %s with branch ALL paradigm has %d subblocks."
                                , b.strId, subBlocks.size()));
                    for (Question q : b.questions) {
                        if (q.branchMap.size()==0)
                            throw new Rules.BlockException(String.format("Block %s with branch ALL paradigm has non-branching question %s"
                                    , b.strId, q));
                    }
            }
        }
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
                            throw new BlockContiguityException(questions.get(i-1), questions.get(i));
                    }
                else throw new BlockContiguityException(questions.get(i-1), questions.get(i));
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

    public boolean equals(Object o) {
        assert(o instanceof Block);
        Block b = (Block) o;
        return Arrays.equals(this.id, b.id);
    }

    public int hashCode() {
        return Arrays.hashCode(id);
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
        if (this.branchParadigm.equals(BranchParadigm.ALL))
            return 1;
        int ct = this.questions.size();
        for (Block b : this.subBlocks) {
            ct += b.dynamicQuestionCount();
        }
        return ct;
    }

    private static void propagateBlockIndices(Block block) {
        int depth = block.getBlockDepth();
        int index = block.index;
        for (Block b : block.subBlocks){
            b.id[depth-1] = index;
            propagateBlockIndices(b);
        }
    }

    protected static void shuffleRandomizedBlocks(List<Block> blockCollection) {
        // get indices
        List<Integer> indices = new ArrayList<Integer>();
        for (Block b : blockCollection)
            indices.add(b.index);
        // shuffle index collection
        Collections.shuffle(indices, Question.rng);
        // reset indices
        for (int i = 0 ; i < blockCollection.size() ; i++)
            blockCollection.get(i).index = indices.get(i);
        //  propagate changes
        for (Block b : blockCollection){
            propagateBlockIndices(b);
        }
    }

    public void randomize() throws SurveyException {
        sort();
        List<Block> randomizedBlocks =  new LinkedList<Block>();
        for (Block b : this.subBlocks)
            if (b.randomize)
                randomizedBlocks.add(b);
        shuffleRandomizedBlocks(randomizedBlocks);
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
        if (subBlocks != null)
            for (Block b : subBlocks)
                b.randomize();
        sort();
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

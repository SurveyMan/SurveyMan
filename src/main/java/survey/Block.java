package survey;

import input.exceptions.BranchException;
import org.apache.commons.lang.StringUtils;
import survey.exceptions.SurveyException;

import java.util.*;

public class Block extends SurveyObj implements Comparable{

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
    private boolean randomize = false;
    private int[] id = null;
    
    public Block() {
      
    }
    
    public Block(String strId) {
        this.id = Block.idToArray(strId);
        this.strId = strId;
        if (isRandomizable(this.strId))
            this.randomize = true;
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
    }

    public int getBlockDepth(){
        return id.length;
    }

    public int[] getBlockId(){
        return id;
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

    /**
     * DO NOT CALL COLLECTIONS.SORT IF YOU HAVE FLOATING BLOCKS -- compareTo is transitive and you may get out-of-order
     * blocks. Call Block.sort instead.
     * @param o
     * @return
     */
    @Override
    public int compareTo(Object o) {
        Block that = (Block) o;
        if (this.randomize || that.randomize)
            throw new RuntimeException("DO NOT CALL COMPARETO ON RANDOMIZABLE BLOCKS");
        else {
            for (int i = 0 ; i < this.id.length ; i++) {
                if (this.id[i] > that.id[i])
                    return 1;
                else if (this.id[i] < that.id[i])
                    return -1;
            }
            return 0;
        }
    }

    public static Block[] shuffle(List<Block> blockList) {

        Block[] retval = new Block[blockList.size()];
        List<Block> floating = new ArrayList<Block>();
        List<Block> normal = new ArrayList<Block>();
        List<Integer> indices = new ArrayList<Integer>();

        for (Block b : blockList)
            if (b.randomize)
                floating.add(b);
            else normal.add(b);
        for (int i = 0 ; i < retval.length ; i++)
            indices.add(i);

        Collections.shuffle(floating);
        Collections.sort(normal);
        Collections.shuffle(indices);

        List<Integer> indexList1 = indices.subList(0, floating.size());
        List<Integer> indexList2 = indices.subList(floating.size(), blockList.size());
        Iterator<Block> blockIter1 = floating.iterator();
        Iterator<Block> blockIter2 = normal.iterator();

        Collections.sort(indexList2);

        for (Integer i : indexList1)
            retval[i] = blockIter1.next();
        for (Integer i : indexList2)
            retval[i] = blockIter2.next();

        return retval;
    }


}

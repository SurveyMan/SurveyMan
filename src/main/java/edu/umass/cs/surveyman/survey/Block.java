package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.input.exceptions.BranchException;
import edu.umass.cs.surveyman.qc.QCMetrics;
import org.apache.commons.lang3.StringUtils;
import edu.umass.cs.surveyman.survey.exceptions.BlockException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

/**
 * <p>
 * Blocks are the basic unit of state in the survey. They are used to specify control flow. Blocks may contain questions.
 * The order of these questions within the block may be randomized. If the user does not want questions randomized within
 * a block, they must place each question in its own, stationary (non-randomizable) block.
 * </p>
 *
 * <p>
 * The order of blocks may be randomized.
 * </p>
 *
 */
public class Block extends SurveyObj implements Comparable, Serializable {


    /**
     * <p>
     *     "Branching" refers to atypical control flow in the survey. Default control flow randomizes questions inside
     *     their containing blocks and floating blocks as appropriate. For example, a survey with the following structure:
     *     <br/><br/>
     *
     *     {@code Block, Question}<br/>
     *     {@code 1, Question 1}<br/>
     *     {@code 1, Question 2}<br/>
     *     {@code 1.1, Question 3}<br/>
     *     {@code 1.1, Question 4}<br/>
     *     {@code 1.2, Question 5}<br/>
     *     {@code 1.2, Question 6}<br/>
     *     <br/>
     *     permits the ordering<br/>
     *     <br/>
     *     {@code Question 3}<br/>
     *     {@code Question 4}<br/>
     *     {@code Question 1}<br/>
     *     {@code Question 5}<br/>
     *     {@code Question 6}<br/>
     *     {@code Question 2}<br/>
     *  <br/>
     *  but not<br/>
     *     <br/>
     *     {@code Question 3}<br/>
     *     {@code Question 5}<br/>
     *     {@code Question 6}<br/>
     *     {@code Question 3}<br/>
     *     {@code Question 4}<br/>
     *     {@code Question 2}.<br/>
     *  <br/>
     *  If we wanted the second ordering, we would need to set either or both of blocks 1.1 or/and 1.2 to randomizable.
     *  We can do this by prefixing the component of the identifier that denotes the containing block with an alpha-
     *  numeric character: 1.a1 or/and 1.b2. An error will be thrown if there is inconsistency in the identifiers.
     * </p>
     * <p>
     *     Basic, or "true" branching happens when a survey writer wants respondents who answer one way to see a different
     *     set of questions from respondents who answer another way (or any k number of ways). It could be that some
     *     respondents simply answer more questions, so that the set of questions seen by one group of respondents is a
     *     subset of the set of questions seen by another set, or it could be that respondents in each set diverge
     *     completely after answering a particular question. SurveyMan does not distinguish between these two scenarios.
     * </p>
     * <p>
     *     To implement "true" branching, a survey writer must specify a branch target for the responses to a branch
     *     question. SurveyMan only permits one branch question per top-level block. Branching is permitted out of any
     *     block, but only into top-level, stationary blocks. These policies ensure that randomization does not cause
     *     respondents who answer equivalently to see different questions.
     * </p>
     * <p>
     *     SurveyMan only permits one true branch question per block. If a block's sub-block contains a branch question,
     *     then the following rules hold:
     *     <ul>
     *         <li>None of the sub-block's descendant blocks may contain a true branch question.</li>
     *         <li>The containing block may not contain a true branch question.</li>
     *         <li>None of the descendants of the containing block's other sub-blocks may contain a true branch question.</li>
     *     </ul>
     * </p>
     * <p>
     *     SurveyMan also permits sampling, which is treated as a form a branching. A block is sampled when every question
     *     it it has an identical branch map. Sampling blocks may not contain sub-blocks. The branch map may contain
     *     true branching information, or it may contain the NEXT pointer. When a sampling block's questions all have
     *     true branch maps, the containing block will treat the entire block as a branch question and will propagate
     *     its branching policy as in the true branching case. When a sampling block's question all have the NEXT pointer,
     *     the block will be treated as a non-branching question by its containing block.
     * </p>
     * <p>
     *     Randomization is performed at runtime. For static blocks, SurveyMan can determine a partial order on questions
     *     at compile time. Branching occurs on the basis of input data and cannot be determined until runtime. True
     *     branching provides some limited information that could be used for speculative execution. Sampling with true
     *     branching further complicates static analyses, while sampling with the NEXT pointer pushes even more control
     *     flow logic to the runtime.
     * </p>
     */
    public enum BranchParadigm {
        /***
         * A block is set to ALL when every question in it has a branch map. Those branch maps must be identical.
         */
        ALL,
        /**
         * A block is set to NONE when none of its questions has a branch map, and when none of its sub-blocks are
         * set to ONE.
         */
        NONE,
        /**
         * A block is set to ONE when either it contains a question with a branch map, or when one of its descendant
         * blocks is set to ONE.
         */
        ONE,
        UNKNOWN
    }

    /**
     * The next pointer.
     */
    public static final String NEXT = "NEXT";

    /**
     * The source identifier
     */
    private String strId;
    /**
     * The questions that reside directly in this block (not in sub-blocks).
     */
    public List<Question> questions = new ArrayList<>();
    // each top-level block is allowed one branch question
    /**
     * The branch question associated with this block. If the branch question is derived from an ALL block, this is just
     * set to an arbitrary question within that block. It is used for debugging purposes.
     */
    public Question branchQ = null;
    /**
     * The branch paradigm associated with this block.
     */
    protected BranchParadigm branchParadigm = BranchParadigm.UNKNOWN;
    /**
     * The sub-blocks that this block contains.
     */
    public List<Block> subBlocks = new ArrayList<>();
    /**
     * The parent pointer for this block. This is null if the block is top-level.
     */
    public Block parentBlock;
    /**
     * Boolean indicating whether this block is floating.
     */
    private boolean randomize = false;
    /**
     * The internal block representation; used for sorting, suffling, and generating "phantom" blocks.
     */
    private int[] id = null;

    /**
     * Constructs a block with the appropriate string id.
     * @param strId The source identifier string.
     */
    public Block(String strId) {
        this.id = Block.idToArray(strId);
        this.strId = strId;
        if (isRandomizable(this.strId))
            this.randomize = true;
        this.branchParadigm = BranchParadigm.NONE;
    }

    /**
     * Returns the identifier for this block in the source file.
     * @return The String representation of the source identifier.
     */
    @Override
    public String getId(){
        return this.strId;
    }

    /**
     * Sets the source identifier for this block.
     * @param strId The source identifier string.
     */
    public void setId(String strId){
        this.strId = strId;
    }

    /**
     * Returns whether a block with the input id "float."
     * @param strId The source identifier string.
     * @return boolean indicating whether this is a floating block..
     */
    public static boolean isRandomizable(String strId) {
        String[] pieces = strId.split("\\.");
        return !Character.isDigit(pieces[pieces.length - 1].charAt(0));
    }

    /**
     * Returns a parsed, internal representation of the input identifier string.
     * @param strId The source identifier string.
     * @return array representation of the block id, where length is the depth of the block, and randomization flags
     * removed.
     */
    public static int[] idToArray(String strId) {
        String[] pieces = strId.split("\\.");
        int[] retval = new int[pieces.length];
        for (int i = 0 ; i < pieces.length ; i ++) {
            String s = isRandomizable(pieces[i]) ? pieces[i].substring(1) : pieces[i];
            retval[i] = Integer.parseInt(s);
        }
        return retval;
    }

    /**
     * Returns the parent block's source identifier string.
     * @return The source identifier string of the parent.
     */
    public String getParentStrId() {
        String[] pieces = this.strId.split("\\.");
        String[] parentStuff = Arrays.copyOfRange(pieces, 0, pieces.length - 1);
        return StringUtils.join(parentStuff, ".");
    }

    /**
     * Converts an internal block identifier to the string block identifier, according to the language/format specified
     * for CSVs.
     * @param id A SurveyMan internal block identifier.
     * @return A String of that identifier.
     */
    public static String idToString(int[] id, Map<String, Block> blockMap){

        String topLevel = Integer.toString(id[0]);
        String topLevelId = (blockMap.containsKey(topLevel) ? "" : "_") + topLevel;
        StringBuilder prefix = new StringBuilder(topLevelId);

        assert blockMap.containsKey(topLevelId) : String.format("Top level block with id %s not found.", topLevelId);

        for (int i = 1 ; i < id.length ; i++) {

            String thisIndex = Integer.toString(id[i]);
            if (blockMap.containsKey(prefix.toString() + "." + thisIndex)) {
                prefix.append(".").append(thisIndex);
            } else {
                Pattern regexp = Pattern.compile(prefix.toString() + "\\.(_|[a-z]+)" + thisIndex);
                for (String key : blockMap.keySet()) {
                    if (regexp.matcher(key).matches()) {
                        prefix = new StringBuilder(key);
                        break;
                    }
                }
            }

            assert blockMap.containsKey(prefix.toString()) : String.format("Ancestor block %s of %s not found. %s", prefix.toString(), topLevelId, Arrays.toString(blockMap.keySet().toArray()));

        }

        return prefix.toString();
    }

    /**
     * Sets the appropriate {@link edu.umass.cs.surveyman.survey.Block.BranchParadigm} for this block and its ancestors.
     * @throws SurveyException
     */
    private void propagateUp() throws SurveyException {
        if (parentBlock==null)
            return;

        switch (this.branchParadigm){
            case ONE:
                switch (parentBlock.branchParadigm) {
                    case NONE:
                        parentBlock.branchParadigm = BranchParadigm.ONE;
                        parentBlock.branchQ = this.branchQ;
                        parentBlock.propagateUp();
                        break;
                    case UNKNOWN:
                        parentBlock.branchParadigm = BranchParadigm.ONE;
                        parentBlock.branchQ = this.branchQ;
                        parentBlock.propagateUp();
                        break;
                    case ONE:
                        assert parentBlock.branchQ!=null:
                                String.format("Parent block %s was set to %s without setting branch question.",
                                        parentBlock.strId, parentBlock.branchParadigm.toString());
                        if (!parentBlock.branchQ.equals(this.branchQ))
                            throw new BranchException(String.format("Both block %s and %s are set to paradigm ONE and have unequal branch questions (%s and %s)"
                                    , this.strId, this.parentBlock.strId, this.branchQ, this.parentBlock.branchQ));
                        break;
                    case ALL:
                        throw new BranchException(String.format("Parent block %s is set to ALL; child block %s is set to ONE"
                                , this.parentBlock.strId, this.strId));
                }
            case NONE:
                switch (parentBlock.branchParadigm) {
                    case NONE: break;
                    case ONE: break;
                    case ALL:
                        throw new BranchException(String.format("Parent block %s is set to ALL; child block %s is set to NONE"
                                , this.parentBlock.strId, this.strId));
                }
            case ALL: break;
        }
    }

    /**
     * Sets the parent pointer of this block, according to its source string identifier.
     */
    public void setParentPointer(){
        for (Block b : this.subBlocks){
            if (b.parentBlock==null)
                b.parentBlock = this;
            b.setParentPointer();
        }
    }

    public void updateBranchParadigm(BranchParadigm branchParadigm)
    {
        this.branchParadigm = branchParadigm;
        if (this.parentBlock!=null) this.parentBlock.branchParadigm = BranchParadigm.UNKNOWN ;
    }

    /**
     * Gets the branch paradigm type.
     * @return BranchParadigm enum, corresponding to this block's branching type.
     */
    public BranchParadigm getBranchParadigm() {
        return this.branchParadigm;
    }

    /**
     * Sets the appropriate {@link edu.umass.cs.surveyman.survey.Block.BranchParadigm} for all connected blocks.
     * This method passes the branch paradigm up. It expects the branch paradigm for this current block to be set already.
     * The method returns when it hits a top-level block. Since branch-all and branch-none classifications don't
     * propagate up, this method only propagates branch-one, and asserts that its properties are not violated.
     * @throws SurveyException
     */
    public void propagateBranchParadigm()
            throws SurveyException {

        if (parentBlock==null) return;

        if (branchParadigm.equals(BranchParadigm.ONE))
            propagateUp();

        // propagateUp doesn't check siblings.
        for (Block sibling : parentBlock.subBlocks) {
            switch (sibling.branchParadigm) {
                case ONE:
                    if (!sibling.equals(this) && this.branchParadigm.equals(BranchParadigm.ONE))
                        throw new BlockException(String.format("Block %s has two subblocks with branch ONE paradigm (%s and %s)"
                                , parentBlock.strId
                                , this.strId
                                , sibling.strId));
                    break;
                case ALL:
                    if (sibling.subBlocks.size()!=0)
                        throw new BlockException(String.format("Block %s with branch ALL paradigm has %d subblocks."
                                , sibling.strId, subBlocks.size()));
            }
        }
    }

    /**
     * Returns whether this block may be shuffled along with other "floating" blocks and questions inside its containing
     * block.
     * @return Boolean indicating whether this block "floats".
     */
    public boolean isRandomized() {
        return this.randomize;
    }

    /**
     * Returns whether this block is at the top of the survey (has no parents).
     * @return Boolean for whether this block is top-level.
     */
    public boolean isTopLevel() {
        return id.length == 1;
    }

    /**
     * Compares this block to the input block. Only static blocks that are not directly related to each other (neither
     * ancestors nor descendants) can be compared. All other blocks return false (conflation of false and bottom).
     * @param that The block to compare.
     * @return Boolean indicating whether the input precedes this block.
     */
    public boolean before(Block that) {
        // checks whether this precedes that
        if (this.isSubblockOf(that) || that.isSubblockOf(this) || this.randomize || that.randomize)
            return false;
        for (int i = 0; i < Math.min(this.id.length, that.id.length); i++)
            if (this.id[i] != that.id[i])
                return this.id[0] < that.getBlockId()[0];
        return false;
    }

    /**
     * Compares this block with the input block and returns true if the input is a descendant of this block.
     * @param b The block to compare.
     * @return Boolean indicating whether the input is a descendant (not direct sub-block).
     */
    public boolean isSubblockOf(Block b) {
        // test whether this is a subblock of b
        int[] yourId = b.getBlockId();
        for (int i = 0 ; i < yourId.length ; i++) {
            if (yourId[i] != this.id[i])
                return false;
        }
        return true;
    }

    /**
     * Sets this block's internal block identifier to the input internal block identifier.
     * @param id The input block identifier.
     */
    public void setIdArray(int[] id) {
        this.id = id;
    }

    /**
     * Returns the depth at which this block exists. Equivalent to the number of ancestors, plus one.
     * @return The number of ancestor blocks plus one.
     */
    public int getBlockDepth(){
        return id.length;
    }

    /**
     * Returns the internal block identifier.
     * @return The internal block identifier.
     */
    public int[] getBlockId(){
        return id;
    }

    /**
     * Sorts the input block list. This must be used instead of Collections.getSorted, which will throw an error, due to
     * inappropriate behavior in compareTo.
     * @param blockList The block list to be sorted
     * @return A sorted block list.
     */
    public static List<Block> getSorted(List<Block> blockList){
        List<Block> retval = new ArrayList<>();
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

    /**
     * Counts the number of questions directly in this block and in all sub-blocks.
     * @return The total number of questions in this block and all of its descendants.
     */
    public int blockSize(){
        //re-implement this is non-recursive later
        int size = this.branchParadigm.equals(BranchParadigm.ALL) ? 1 : questions.size();
        if (subBlocks!=null)
            for (Block b : subBlocks)
                size += b.blockSize();
        return size;
    }

    /**
     * Two blocks are equal if their internal identifiers are equal.
     * @param o The object to compare.
     * @return The usual.
     */
    public boolean equals(Object o) {
        assert(o instanceof Block);
        Block b = (Block) o;
        return Arrays.equals(this.id, b.id);
    }

    /**
     * Hashed on the internal block identifier.
     * @return The usual.
     */
    public int hashCode() {
        return Arrays.hashCode(id);
    }

    /**
     * Returns all of the questions for this block and all of its sub-blocks. The instance version of that other one.
     * @return A list of all the questions this block and its descendants contain.
     */
    public List<Question> getAllQuestions() {
        List<Question> qs = this.questions==null ? new ArrayList<Question>() : new ArrayList<>(this.questions);
        if (subBlocks==null)
            return qs;
        for (Block b : subBlocks) {
            qs.addAll(b.getAllQuestions());
        }
        return qs;
    }

    public boolean containsQuestion(Question question) {
        return this.getAllQuestions().contains(question);
    }

    /**
     * Shuffles the input block list, respecting static vs floating blocks.
     * @param blockList The block list to be shuffled.
     * @return A shuffled Block array.
     */
    public static Block[] shuffle(List<Block> blockList) {

        Block[] retval = new Block[blockList.size()];
        List<Block> floating = new ArrayList<>();
        List<Block> normal = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (Block b : blockList)
            if (b.randomize)
                floating.add(b);
            else normal.add(b);
        for (int i = 0 ; i < retval.length ; i++)
            indices.add(i);

        QCMetrics.rng.shuffle(floating);
        Collections.sort(normal);
        QCMetrics.rng.shuffle(indices);

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

    public String jsonize()
            throws SurveyException
    {
        return String.format("{ \"id\" : \"%s\", \"questions\" : %s %s %s}"
                , this.getId()
                , Question.jsonize(this.questions)
                , this.isRandomized() ? String.format(", \"randomize\" : %s", this.isRandomized()) : ""
                , this.subBlocks.size() > 0 ? String.format(", \"subblocks\" : %s", Block.jsonize(this.subBlocks)) : ""
        );
    }

    public static String jsonize(List<Block> blockList) throws SurveyException {
        Iterator<Block> bs = blockList.iterator();
        StringBuilder s = new StringBuilder(bs.next().jsonize());
        while (bs.hasNext()) {
            Block b = bs.next();
            s.append(String.format(", %s", b.jsonize()));
        }
        return String.format("[ %s ]", s.toString());
    }

    public boolean hasBranchQuestion() {
        return this.branchQ != null
                && this.branchQ.branchMap != null
                && this.branchQ.branchMap.size() != 0;
    }

    public Set<Block> getBranchDestinations() {
        return this.branchQ.getBranchDestinations();
    }

    private void setDefaults(Question q) {
        q.freetext = q.freetext == null ? false : q.freetext;
        q.exclusive = q.exclusive == null ? true : q.exclusive;
        q.ordered = q.ordered == null ? false : q.ordered;
    }

    public void addBranchQuestion(Question q) {
        setDefaults(q);
        if (this.questions.contains(q)) return;
        if (this.branchParadigm.equals(BranchParadigm.NONE)) {
            this.branchParadigm = BranchParadigm.ONE;
        } else if (this.branchParadigm.equals(BranchParadigm.ONE)) {
            this.branchParadigm = BranchParadigm.ALL;
        }
        this.branchQ = q;
        this.questions.add(q);
        q.block = this;
    }

    public void addQuestion(Question q) throws SurveyException {
        setDefaults(q);
        if (q.isBranchQuestion()) {
            throw new BranchException("Trying to add a branch question using the wrong method.");
        } else if (!this.questions.contains(q)){
            this.questions.add(q);
            q.block = this;
        }
    }

    public void addQuestions(Question... questions) throws SurveyException {
        for (Question q : questions)
            addQuestion(q);
    }

    /**
     * Method to add a subblock to this block programmatically.
     * @param b The block we want to add as a subblock.
     * @throws SurveyException
     */
    public void addBlock(Block b)
            throws SurveyException
    {
        if (this.branchParadigm.equals(BranchParadigm.ALL))
            throw new BlockException("Cannot add a subblock to a branch-all block.");
        else {
            this.subBlocks.add(b);
            b.parentBlock = this;
        }
        edu.umass.cs.surveyman.analyses.rules.BranchParadigm.ensureBranchParadigms(this);
    }

    /**
     * Returns the topmost block of this block; if this block is top-level, it returns itself.
     * @return Farthest containing block.
     */
    public Block getFarthestContainingBlock()
    {
        Block retval = this;
        while (retval.parentBlock!=null) {
            retval = retval.parentBlock;
        }
        assert retval.isTopLevel() : String.format("Farthest containing block must be top level; Block %s is not.",
                retval.getId());
        return retval;
    }

    /**
     * Composed of the block identifier and the string representation of its containing questions and sub-blocks.
     * @return A string representation of this block.
     */
    @Override
    public String toString() {
        String[] tabs = new String[id.length];
        Arrays.fill(tabs, "\t");
        String indent = StringUtils.join(tabs, "");
        StringBuilder str = new StringBuilder(strId + ":\n" + indent);
        for (Question q : questions)
            str.append("\n").append(indent).append(q.toString());
        if (!subBlocks.isEmpty()) {
            for (Block b : subBlocks) {
                str.append(b.toString());
            }
        }
        return str.toString();
    }

    /**
     * DO NOT CALL COLLECTIONS.SORT IF YOU HAVE FLOATING BLOCKS -- compareTo is transitive and you may get out-of-order
     * blocks. Call Block.getSorted instead.
     * @param o The object to compare.
     * @return int if you're lucky, RuntimeException if you're not.
     */
    @Override
    public int compareTo(Object o)
    {
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


}

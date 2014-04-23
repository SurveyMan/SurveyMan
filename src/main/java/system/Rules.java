package system;

import csv.CSVParser;
import org.apache.log4j.Logger;
import survey.Block;
import survey.Question;
import survey.Survey;
import survey.SurveyException;

import java.lang.reflect.Method;
import java.util.*;


public class Rules {

    public static class DuplicateQuestions extends SurveyException implements Bug {
        Object caller;
        Method lastAction;

        public DuplicateQuestions(Question q1, Question q2, Survey survey) {
            super(String.format("Question (%s) is a duplicate of Question 2 (%s)", q1, q2));
            this.caller = survey;
            this.lastAction = Rules.class.getEnclosingMethod();
            Debugger.addBug(this);
        }

        @Override
        public Method getLastAction() {
            return lastAction;
        }

        @Override
        public Object getCaller() {
            return caller;
        }
    }
    public static class BranchConsistencyException extends SurveyException implements Bug {
        Object caller;
        Method lastAction;
        public BranchConsistencyException(String msg, CSVParser parser, Method lastAction) {
            super(msg);
            this.caller = parser;
            this.lastAction = lastAction;
            Debugger.addBug(this);
        }

        @Override
        public Method getLastAction() {
            return lastAction;
        }

        @Override
        public Object getCaller() {
            return caller;
        }
    }
    public static class BlockException extends SurveyException {
        public BlockException(String msg){
            super(msg);
        }
    }
    final private static Logger LOGGER = Logger.getLogger(Rules.class);

    private static void ensureBranchForward(int[] toBlock, Question q, CSVParser parser) throws SurveyException {
        int[] fromBlock = q.block.getBlockId();
        String toBlockStr = String.valueOf(toBlock[0]);
        for (int i=1; i<toBlock.length; i++)
            if (fromBlock[i]>toBlock[i]) {
                SurveyException e = new CSVParser.BranchException(q.block.getStrId(), Block.idToString(toBlock), parser, parser.getClass().getEnclosingMethod());
                LOGGER.warn(e);
                throw e;
            }
    }

    public static void ensureBranchForward(Survey survey, CSVParser parser) throws SurveyException {
        for (Question q : survey.questions) {
            if (q.branchMap.isEmpty())
                continue;
            for (Block b : q.branchMap.values()) {
                if (b!=null) // if we aren't sampling
                    ensureBranchForward(b.getBlockId(), q, parser);
            }
        }
    }

    public static void ensureBranchTop(Survey survey, CSVParser parser) throws SurveyException {
        for (Question q : survey.questions) {
            if (q.branchMap.isEmpty())
                continue;
            for (Block b : q.branchMap.values())
                if (b!=null && !b.isTopLevel())
                    throw new CSVParser.BranchException(String.format("Branch %s is not top level", Arrays.asList(b.getBlockId())), parser, parser.getClass().getEnclosingMethod());
        }
    }

    public static void ensureCompactness(Survey survey) throws SurveyException {
        //first check the top level
        List<Block> topLevelBlocks = survey.topLevelBlocks;
        Map<String, Block> allBlockLookUp = survey.blocks;
        Block[] temp = new Block[topLevelBlocks.size()];
        for (Block b : topLevelBlocks) {
            int[] id = b.getBlockId();
            if (temp[id[0]-1]==null)
                temp[id[0]-1]=b;
            else {
                SurveyException e = new CSVParser.SyntaxException(String.format("Block %s is noncontiguous.", b.getStrId()), null, null);
                LOGGER.warn(e);
                throw e;
            }
        }
        if (allBlockLookUp==null)
          return;
        for (Block b : allBlockLookUp.values())
            if (b.subBlocks!=null)
                for (Block bb : b.subBlocks)
                    if (bb==null) {
                        SurveyException e = new CSVParser.SyntaxException(String.format("Detected noncontiguous subblock in parent block %s", b.getStrId()), null, null);
                        LOGGER.warn(e);
                        throw e;
                    }
    }

    private static boolean onSamePath(Question q1, Question q2, Survey survey) throws SurveyException {
        Question[] allQs = survey.getQuestionsByIndex();
        Question start, end;
        if (q1.before(q2)) {
            start = q1;
            end = q2;
        } else if (q2.before(q2)) {
            start = q2;
            end = q1;
        } else return false;
        // see if we can reach end from start
        LinkedList<Question> path = new LinkedList<Question>();
        path.addFirst(start);
        while (! path.isEmpty()) {
            Question q = path.removeFirst();
            if (q.equals(end))
                return true;
            if (q.index+1==allQs.length)
                return false;
            if (q.branchMap.isEmpty())
                path.addFirst(allQs[q.index+1]);
            else {
                for (Block branchTo : q.branchMap.values()) {
                    branchTo.sort();
                    Question qq = branchTo.questions.get(0);
                    if (path.contains(qq))
                        continue;
                    else path.addFirst(qq);
                }
            }
        }
        return false;
    }

    public static void ensureNoDupes(Survey survey) throws SurveyException {
        Question q1, q2;
        for (Question outerQ : survey.questions) {
            q1 = outerQ;
            for (Question innerQ : survey.questions) {
                if (outerQ!=innerQ && q1.equals(innerQ)) {
                    q2=innerQ;
                    if (onSamePath(q1, q2, survey)) {
                        SurveyException e = new DuplicateQuestions(q1, q2, survey);
                        LOGGER.warn(e);
                        throw e;
                    }
                }
            }
        }

    }

    private static int ensureBranchParadigms(Block b, Survey survey, CSVParser parser) throws SurveyException {
        switch (b.branchParadigm) {
            case NONE:
                // all of its children have the branch paradigm NONE or SAMPLE
                for (Block sb : b.subBlocks) {
                    if (sb.branchParadigm.equals(Block.BranchParadigm.ONE))
                        throw new BranchConsistencyException(String.format("Parent block %s has paradigm %s. Ancestor block %s has paradigm %s."
                                , b.getStrId(), b.branchParadigm.name(), sb.getStrId(), sb.branchParadigm.name()), parser, null);
                    ensureBranchParadigms(sb, survey, parser);
                }
                break;
            case SAMPLE:
                if (!b.subBlocks.isEmpty())
                    throw new BlockException(String.format("Blocks with the branch-all paradigm cannot have subblocks. " +
                            "(This is semantically at odds with what branch-all does.)"));
                break;
            case ONE:
                int ones = 0;
                for (Block sb : b.subBlocks) {
                    if (sb.branchParadigm.equals(Block.BranchParadigm.NONE))
                        ensureBranchParadigms(sb, survey, parser);
                    else {
                        ones++;
                        int kidsOnes = ensureBranchParadigms(sb, survey, parser);
                        if (ones > 1 || kidsOnes > 1)
                            throw new BlockException(String.format("Blocks can only have one branching subblock. " +
                                    "Block %s has %d immediate branching blocks and at least %d branching blocks in one of its children"
                            , b.getStrId(), ones, kidsOnes));
                    }
                    return ones;
                }
        }
        return 0;
    }

    public static void ensureBranchParadigms(Survey survey, CSVParser parser) throws SurveyException {
        for (Block b : survey.topLevelBlocks) {
            ensureBranchParadigms(b, survey, parser);
        }
    }


    public static void ensureNoTopLevelRandBranching(Survey survey) throws SurveyException {
        for (Block b : survey.topLevelBlocks) {
            if (b.isRandomized())
                // no branching from this block
                assert(!b.branchParadigm.equals(Block.BranchParadigm.ONE));
            else {
                // no branching to randomizable blocks
                if (b.branchParadigm.equals(Block.BranchParadigm.ONE)){
                    assert b.branchQ!=null : String.format("Branch ONE from block %s does not have branchQ set", b.getStrId());
                    Question branchQ = b.branchQ;
                    assert branchQ.branchMap.values().size() > 0 : String.format("Branch map for question %s is empty", branchQ.quid);
                    for (Block dest : branchQ.branchMap.values())
                        if (dest!=null)
                            assert(!dest.isRandomized());
                }
            }
        }
    }

    private static void ensureSampleHomogenousMaps(Block block) throws SurveyException{
        if (block.branchParadigm.equals(Block.BranchParadigm.SAMPLE)){
            assert(block.subBlocks.size()==0);
            Collection<Block> dests = block.branchQ.branchMap.values();
            for (Question q : block.questions){
                Collection<Block> qDests = q.branchMap.values();
                if (!qDests.containsAll(dests) || !dests.containsAll(qDests))
                    throw new CSVParser.BranchException(String.format("Question %s has branch map %s; was expecting %s", q, qDests, dests), null, null);
            }
        } else {
            for (Block b : block.subBlocks)
                ensureSampleHomogenousMaps(b);
        }
    }

    public static void ensureSampleHomogenousMaps(Survey survey) throws SurveyException{
        for (Block b : survey.topLevelBlocks)
            ensureSampleHomogenousMaps(b);
    }

    public static void ensureExclusiveBranching(Survey survey) throws SurveyException{
        for (Question q : survey.questions)
            if (!q.branchMap.isEmpty() && !q.exclusive)
                throw new CSVParser.BranchException(String.format("Question %s is nonexclusive and branches.", q), null, null);
    }

    public static void ensureBranchConsistency(Survey survey, CSVParser parser)  throws SurveyException {
        for (Block b : parser.getAllBlockLookUp().values()) {
            switch (b.branchParadigm) {
                case NONE:
                    if (b.branchQ!=null)
                        throw new BranchConsistencyException(String.format("Block (%s) is set to have no branching but has its branch question set to (%s)", b, b.branchQ), parser, parser.getClass().getEnclosingMethod());
                    break;
                case SAMPLE:
                    for (Question q : b.questions)
                        if (q.branchMap.isEmpty())
                            throw new BranchConsistencyException(String.format("Block (%s) is set to have all branching but question (%s) does not have its branch map set.", b, q), parser, parser.getClass().getEnclosingMethod());
                    break;
                case ONE:
                    Question branchQ = null;
                    for (Question q : b.questions)
                        if (q.branchMap.isEmpty())
                            continue;
                        else {
                            if (branchQ==null)
                                branchQ = q;
                            else throw new BranchConsistencyException(String.format("Block (%s) expected to have exactly one branch question, but both questions (%s) and (%s) are set to  branch.", b, q, branchQ), parser, parser.getClass().getEnclosingMethod());
                        }
                    if (!branchQ.equals(b.branchQ))
                        throw new BranchConsistencyException(String.format("Block (%s) expected (%s) to be the branch question, but found question (%s) instead.", b, b.branchQ, branchQ), parser, parser.getClass().getEnclosingMethod());
                    break;
            }
        }
    }

}

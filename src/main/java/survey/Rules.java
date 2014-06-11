package survey;

import input.exceptions.BranchException;
import input.exceptions.SyntaxException;
import org.apache.log4j.Logger;
import qc.IQCMetrics;
import qc.Interpreter;
import survey.exceptions.BlockException;
import survey.exceptions.BranchConsistencyException;
import survey.exceptions.SurveyException;
import survey.exceptions.UnreachableBlockException;

import java.util.*;


public class Rules {

    final private static Logger LOGGER = Logger.getLogger(Rules.class);

    private static void ensureBranchForward(int[] toBlock, Question q) throws SurveyException {
        int[] fromBlock = q.block.getBlockId();
        for (int i=0; i<toBlock.length; i++)
            if (fromBlock[i]>toBlock[i]) {
                SurveyException e = new BranchException(q.block.getStrId(), Block.idToString(toBlock));
                LOGGER.warn(e);
                throw e;
            }
    }

    public static void ensureBranchForward(Survey survey) throws SurveyException {
        for (Question q : survey.questions) {
            if (q.branchMap.isEmpty())
                continue;
            for (Block b : q.branchMap.values()) {
                if (b!=null) // if we aren't sampling
                    ensureBranchForward(b.getBlockId(), q);
            }
        }
    }

    public static void ensureBranchTop(Survey survey) throws SurveyException {
        for (Question q : survey.questions) {
            if (q.branchMap.isEmpty())
                continue;
            for (Block b : q.branchMap.values())
                if (b!=null && !b.isTopLevel())
                    throw new BranchException(String.format("Branch %s is not top level", Arrays.asList(b.getBlockId())));
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
                SurveyException e = new SyntaxException(String.format("Block %s is noncontiguous.", b.getStrId()));
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
                        SurveyException e = new SyntaxException(String.format("Detected noncontiguous subblock in parent block %s", b.getStrId()));
                        LOGGER.warn(e);
                        throw e;
                    }
    }

    private static int ensureBranchParadigms(Block b) throws SurveyException {
        switch (b.branchParadigm) {
            case NONE:
                // all of its children have the branch paradigm NONE or ALL
                for (Block sb : b.subBlocks) {
                    if (sb.branchParadigm.equals(Block.BranchParadigm.ONE))
                        throw new BranchConsistencyException(String.format("Parent block %s has paradigm %s. Ancestor block %s has paradigm %s."
                                , b.getStrId(), b.branchParadigm.name(), sb.getStrId(), sb.branchParadigm.name()));
                    ensureBranchParadigms(sb);
                }
                break;
            case ALL:
                if (!b.subBlocks.isEmpty())
                    throw new BlockException(String.format("Blocks with the branch-all paradigm cannot have subblocks. " +
                            "(This is semantically at odds with what branch-all does.)"));
                break;
            case ONE:
                int ones = 0;
                for (Block sb : b.subBlocks) {
                    if (sb.branchParadigm.equals(Block.BranchParadigm.NONE))
                        ensureBranchParadigms(sb);
                    else {
                        ones++;
                        int kidsOnes = ensureBranchParadigms(sb);
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

    public static void ensureBranchParadigms(Survey survey) throws SurveyException {
        for (Block b : survey.topLevelBlocks) {
            ensureBranchParadigms(b);
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
        if (block.branchParadigm.equals(Block.BranchParadigm.ALL)){
            assert(block.subBlocks.size()==0);
            Collection<Block> dests = block.branchQ.branchMap.values();
            for (Question q : block.questions){
                Collection<Block> qDests = q.branchMap.values();
                if (!qDests.containsAll(dests) || !dests.containsAll(qDests))
                    throw new BranchException(String.format("Question %s has branch map %s; was expecting %s", q, qDests, dests));
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
                throw new BranchException(String.format("Question %s is nonexclusive and branches.", q));
    }

    public static void ensureBranchConsistency(Survey survey)  throws SurveyException {
        for (Block b : survey.blocks.values()) {
            switch (b.branchParadigm) {
                case NONE:
                    if (b.branchQ!=null)
                        throw new BranchConsistencyException(String.format("Block (%s) is set to have no branching but has its branch question set to (%s)", b, b.branchQ));
                    break;
                case ALL:
                    for (Question q : b.questions)
                        if (q.branchMap.isEmpty())
                            throw new BranchConsistencyException(String.format("Block (%s) is set to have all branching but question (%s) does not have its branch map set.", b, q));
                    break;
                case ONE:
                    Question branchQ = null;
                    for (Question q : b.questions)
                        if (!q.branchMap.isEmpty())
                            if (branchQ==null)
                                branchQ = q;
                            else throw new BranchConsistencyException(String.format("Block (%s) expected to have exactly one branch question, but both questions (%s) and (%s) are set to  branch.", b, q, branchQ));
                    if (branchQ!=null && !branchQ.equals(b.branchQ))
                        throw new BranchConsistencyException(String.format("Block (%s) expected (%s) to be the branch question, but found question (%s) instead.", b, b.branchQ, branchQ));
                    break;
            }
        }
    }

    public static void ensureReachability(Survey survey) throws SurveyException {
        // only need to check stationary top-level blocks
        try {
            IQCMetrics qc = (IQCMetrics) Class.forName("qc.Metrics").newInstance();
            List<Block> stationaryBlocks = Interpreter.partitionBlocks(survey).get(false);
            List<List<Block>> dag = qc.getDag(stationaryBlocks);
            Set<Block> allVisited = new HashSet<Block>();
            for (List<Block> path : dag)
                for (Block b : path)
                    allVisited.add(b);
            for (Block b : stationaryBlocks)
                if (!allVisited.contains(b))
                    throw new UnreachableBlockException(b);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}

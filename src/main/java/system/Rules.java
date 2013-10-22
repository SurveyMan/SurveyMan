package system;

import csv.CSVParser;
import org.apache.log4j.Logger;
import survey.Block;
import survey.Question;
import survey.Survey;
import survey.SurveyException;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class Rules {

    public static class DuplicateQuestions extends SurveyException implements Bug {
        Object caller;
        Method lastAction;

        public DuplicateQuestions(Question q1, Question q2, Survey survey) {
            super(String.format("Question (%s) is a duplicate of Question 2 (%s)"));
            this.caller = survey;
            this.lastAction = (new Rules()).getClass().getEnclosingMethod();
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


    final private static Logger LOGGER = Logger.getLogger(Rules.class);

    private static void ensureBranchForward(int[] toBlock, Question q, CSVParser parser) throws SurveyException {
        int[] fromBlock = q.block.id;
        String toBlockStr = String.valueOf(toBlock[0]);
        for (int i=1; i<toBlock.length; i++)
            toBlockStr = toBlockStr + "." + toBlock[i];
        if (fromBlock[0]>=toBlock[0]) {
            SurveyException e = new CSVParser.BranchException(q.block.strId, toBlockStr, parser, parser.getClass().getEnclosingMethod());
            LOGGER.warn(e);
            throw e;
        }
    }

    public static void ensureBranchForward(Survey survey, CSVParser parser) throws SurveyException {
        for (Question q : survey.questions) {
            if (q.branchMap.isEmpty())
                continue;
            for (Block b : q.branchMap.values()) {
                ensureBranchForward(b.id, q, parser);
            }
        }
    }

    public static void ensureCompactness(CSVParser parser) throws SurveyException {
        //first check the top level
        List<Block> topLevelBlocks = parser.getTopLevelBlocks();
        Map<String, Block> allBlockLookUp = parser.getAllBlockLookUp();
        Block[] temp = new Block[topLevelBlocks.size()];
        for (Block b : topLevelBlocks) {
            if (temp[b.id[0]-1]==null)
                temp[b.id[0]-1]=b;
            else {
                SurveyException e = new CSVParser.SyntaxException(String.format("Block %s is noncontiguous.", b.strId)
                        , parser
                        , parser.getClass().getEnclosingMethod());
                LOGGER.warn(e);
                throw e;
            }
        }
        for (Block b : allBlockLookUp.values())
            if (b.subBlocks!=null)
                for (Block bb : b.subBlocks)
                    if (bb==null) {
                        SurveyException e = new CSVParser.SyntaxException(String.format("Detected noncontiguous subblock in parent block %s", b.strId)
                                , parser
                                , parser.getClass().getEnclosingMethod());
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

    public static void ensureRandomizedBlockConsistency(Survey survey, CSVParser parser) {
        Iterator<Block> blockIterator = parser.getAllBlockLookUp().values().iterator();
        while (blockIterator.hasNext()) {
            Block b = blockIterator.next();
            if (b.isRandomized()) {
                // do something
            }
        }
    }


    public void ensureOneBranch(Survey survey, CSVParser parser)  throws SurveyException {
        for (Block b : parser.getAllBlockLookUp().values()) {
            boolean branch = false;
            for (Question q : b.questions) {
                if (branch && q.branchMap.size() > 0)
                    throw new Block.MultBranchPerBlockException(b);
                else if (!branch && q.branchMap.size() > 0)
                    branch = true;
            }
        }
    }

}

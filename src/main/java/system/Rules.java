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
            super(String.format("Question (%s) is a duplicate of Question 2 (%s)", q1, q2));
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
    final private static Logger LOGGER = Logger.getLogger(Rules.class);

    private static void ensureBranchForward(int[] toBlock, Question q, CSVParser parser) throws SurveyException {
        int[] fromBlock = q.block.getBlockId();
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
                ensureBranchForward(b.getBlockId(), q, parser);
            }
        }
    }

    public static void ensureCompactness(CSVParser parser) throws SurveyException {
        //first check the top level
        List<Block> topLevelBlocks = parser.getTopLevelBlocks();
        Map<String, Block> allBlockLookUp = parser.getAllBlockLookUp();
        Block[] temp = new Block[topLevelBlocks.size()];
        for (Block b : topLevelBlocks) {
            int[] id = b.getBlockId();
            if (temp[id[0]-1]==null)
                temp[id[0]-1]=b;
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


    public void ensureBranchConsistency(Survey survey, CSVParser parser)  throws SurveyException {
        for (Block b : parser.getAllBlockLookUp().values()) {
            switch (b.branchParadigm) {
                case NONE:
                    if (b.branchQ!=null)
                        throw new BranchConsistencyException(String.format("Block (%s) is set to have no branching but has its branch question set to (%s)", b, b.branchQ), parser, parser.getClass().getEnclosingMethod());
                    break;
                case ALL:
                    for (Question q : b.questions)
                        if (q.branchMap.isEmpty())
                            throw new BranchConsistencyException(String.format("Block (%s) is set to have all branching but question (%q) does not have its branch map set.", b, q), parser, parser.getClass().getEnclosingMethod());
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

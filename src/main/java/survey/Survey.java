package survey;

import org.apache.log4j.Logger;
import qc.IQCMetrics;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import survey.exceptions.SurveyException;

import java.util.*;

public class Survey {


    public static class QuestionNotFoundException extends SurveyException {
        public QuestionNotFoundException(String quid, String sid) {
            super(String.format("Question with id %s not found in survey with id %s", quid, sid));
        }
        public QuestionNotFoundException(int i) {
            super(String.format("No question found at line %d", i));
        }
    }

    public static class MalformedQuestionException extends SurveyException {
        public MalformedQuestionException(String msg) {
            super(msg);
        }
    }

    public static class BlockNotFoundException extends SurveyException {
        public BlockNotFoundException(int[] id, Survey s) {
            super(String.format("Block with id %s not found in survey %s", Arrays.toString(id), s.source));
        }
    }

    private static final Gensym gensym = new Gensym("survey");
    private static final Logger LOGGER = Logger.getLogger(Survey.class);
    public static final String QUESTION = "QUESTION";
    public static final String BLOCK = "BLOCK";
    public static final String OPTIONS = "OPTIONS";
    public static final String RESOURCE = "RESOURCE";
    public static final String EXCLUSIVE = "EXCLUSIVE";
    public static final String ORDERED = "ORDERED";
    public static final String RANDOMIZE = "RANDOMIZE";
    public static final String BRANCH = "BRANCH";
    public static final String FREETEXT = "FREETEXT";
    public static final String CORRELATION = "CORRELATION";
    public static final String CUSTOM_ID = "q_-1_-1";
    public static final String[] knownHeaders = {QUESTION, BLOCK, OPTIONS, RESOURCE, EXCLUSIVE, ORDERED, RANDOMIZE, BRANCH, FREETEXT, CORRELATION};

    public String sid = gensym.next();
    public List<Question> questions; //top level list of questions
    public IQCMetrics qc;
    public Map<String, Block> blocks;
    public List<Block> topLevelBlocks;
    public String encoding;
    public String[] otherHeaders;
    public String sourceName;
    public String source;
    public Map<String, List<Question>> correlationMap;

    public synchronized void randomize() throws SurveyException{
        // randomizes the question list according to the block structure
        if (!(blocks == null || blocks.isEmpty())) {
            for (Block b : blocks.values())
                b.randomize();
        } else {
            // this is lazy on my part
            Collections.shuffle(questions, Question.rng);
            int i = 0;
            for (Question q : questions) {
                q.randomize();
                q.index = i;
                i++;
            }
        }
    }


    public boolean removeQuestion(String quid) throws SurveyException{
        boolean found = false;
        for (Question q : questions)
            if (q.quid.equals(quid)) {
                found = true;
                questions.remove(q);
                break;
            }
        for (Block b : blocks.values()) {
            b.removeQuestion(quid);
        }
        int i = 0;
        for (Question q : questions){
            q.index = i;
            i++;
        }
        return found;
    }

    public Question getQuestionById(String quid) throws SurveyException {
        if (quid.equals("assignmentId") || quid.startsWith("start") || quid.equals(CUSTOM_ID))
            return new Question(-1, -1);
        for (Question q : questions)
            if (q.quid.equals(quid))
                return q;
        throw new QuestionNotFoundException(quid, sid);
    }

    public Question getQuestionByLineNo(int lineno) throws SurveyException{
        for (Question q : questions)
            for (int ln : q.sourceLineNos)
                if (ln==lineno)
                    return q;
        throw new QuestionNotFoundException(lineno);
    }
    
    public Question[] getQuestionsByIndex() throws SurveyException {
        Question[] qs = new Question[questions.size()];
        for (Question q: questions) {
            if (q.index > qs.length)
                throw new MalformedQuestionException(String.format("Question\r\n\"%s\"\r\n has an index that exceeds max index %d"
                        , q.toString()
                        , qs.length - 1));
            else if (qs[q.index] != null)
                throw new Question.MalformedOptionException(String.format("Question \r\n\"%s\"\r\n and \r\n\"%s\"\r\n have the same index."
                        , qs[q.index]
                        , q.toString()));
            qs[q.index] = q;
        }
        return qs;
    }
    
    public void resetQuestionIndices() {
        int startingIndex = 0;
        if (this.blocks.isEmpty()) {
            for (int i = 0 ; i < this.questions.size() ; i++)
                 this.questions.get(i).index = i;
        } else {
            for (Block b : this.blocks.values())
              startingIndex += resetQuestionIndices(b, startingIndex);
        }
    }
    
    private int resetQuestionIndices(Block b, int startingIndex) {
        LOGGER.info("resetQuestionIndices: " + b.getStrId()+ " " + startingIndex);
        int index = startingIndex;
        for (Question q : b.questions){
            q.index = index;
            index++;
        }
        for (Block bb : b.subBlocks) {
            LOGGER.info(String.format("block %s's subblock %s starting at %d", b.getStrId(), bb.getStrId(), index));
            index += resetQuestionIndices(bb, index);
        }
        LOGGER.info(String.format("%s's block size : %d", b.getStrId(), b.blockSize()));
        return b.blockSize();
    }

    public boolean permitsBreakoff () {
        for (Question q : this.questions) {
            if (q.permitBreakoff)
                return true;
        }
        return false;
    }

    public Block getBlockById(int[] id) throws BlockNotFoundException {
        String idStr = Block.idToString(id);
        if (blocks.containsKey(idStr))
            return blocks.get(idStr);
        throw new BlockNotFoundException(id, this);
    }

    public Set<Question> getVariantSet(Question thisQ){
        if (thisQ.block.branchParadigm.equals(Block.BranchParadigm.ALL))
            return new HashSet<Question>(thisQ.block.questions);
        return null;
    }

    public CellProcessor[] makeCellProcessors() {

        List<CellProcessor> cells = new ArrayList<CellProcessor>(Arrays.asList(new CellProcessor[]{
                new StrRegEx("sr[0-9]+") //srid
                , null // workerid
                , null  //surveyid
                , new StrRegEx("(assignmentId)|(start_)?q_-?[0-9]+_-?[0-9]+") // quid
                , null //qtext
                , new ParseInt() //qloc
                , new StrRegEx("comp_-?[0-9]+_-?[0-9]+") //optid
                , null //opttext
                , new ParseInt() // oloc
                //, new ParseDate(dateFormat)
                //, new ParseDate(dateFormat)
        }));


        for (int i = 0 ; i < this.otherHeaders.length ; i++) {
            cells.add(null);
        }

        if (!this.correlationMap.isEmpty())
            cells.add(null);

        return cells.toArray(new CellProcessor[cells.size()]);

    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Survey id ").append(sid).append("\n");
        if (blocks.size() > 0) {
            for (Block b : blocks.values())
                str.append("\n").append(b.toString());
        } else {
            for (Question q : questions)
                str.append("\n").append(q.toString());
        }
        return str.toString();
    }
}

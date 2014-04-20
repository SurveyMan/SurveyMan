package survey;

import org.apache.log4j.Logger;
import qc.QCMetrics;
import system.Gensym;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public static final String[] knownHeaders = {QUESTION, BLOCK, OPTIONS, RESOURCE, EXCLUSIVE, ORDERED, RANDOMIZE, BRANCH, FREETEXT, CORRELATION};

    public String sid = gensym.next();
    public List<Question> questions; //top level list of questions
    public QCMetrics qc;
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
            System.out.println("Block randomization (in Survey.randomize)");
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
        LOGGER.info("resetQuestionIndices: " + b.strId + " " + startingIndex);
        int index = startingIndex;
        for (Question q : b.questions){
            q.index = index;
            index++;
        }
        for (Block bb : b.subBlocks) {
            LOGGER.info(String.format("block %s's subblock %s starting at %d", b.strId, bb.strId, index));
            index += resetQuestionIndices(bb, index);
        }
        LOGGER.info(String.format("%s's block size : %d", b.strId, b.blockSize()));
        return b.blockSize();
    }

    private String dataString(Component c) {
        if (c instanceof StringComponent)
            return ((StringComponent) c).data;
        else return String.format("<p>%s</p>", ((URLComponent) c).data.toExternalForm());
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

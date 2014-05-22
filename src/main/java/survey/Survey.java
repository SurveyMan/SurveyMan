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

    public boolean permitsBreakoff () {
        for (Question q : this.questions) {
            if (q.permitBreakoff)
                return true;
        }
        return false;
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

    public void staticAnalysis() throws SurveyException{
        // make sure survey is well formed
        Rules.ensureBranchForward(this);
        Rules.ensureBranchTop(this);
        Rules.ensureCompactness(this);
        Rules.ensureNoDupes(this);
        Rules.ensureBranchParadigms(this);
        Rules.ensureNoTopLevelRandBranching(this);
        Rules.ensureSampleHomogenousMaps(this);
        Rules.ensureExclusiveBranching(this);
        Rules.ensureBranchConsistency(this);
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

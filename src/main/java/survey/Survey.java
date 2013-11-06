package survey;

import java.util.*;

import qc.QCMetrics;
import system.Gensym;

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
    public static final String[] knownHeaders = {QUESTION, BLOCK, OPTIONS, RESOURCE, EXCLUSIVE, ORDERED, RANDOMIZE, BRANCH, FREETEXT};

    public String sid = gensym.next();
    public List<Question> questions; //top level list of questions
    public QCMetrics qc;
    public ArrayList<Block> blocks;
    public String encoding;
    public String[] otherHeaders;
    public String sourceName;
    public String source;
    public Map<String, List<Question>> correlationMap;

    public synchronized void randomize() throws SurveyException{
        // randomizes the question list according to the block structure
        if (!(blocks == null || blocks.isEmpty())) {
            System.out.println("Block randomization (in Survey.randomize)");
            for (Block b : blocks)
                b.randomize();
        } else {
            // this is lazy on my part
            System.out.println("Shuffling? (in randomize of Survey.java)");
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
        for (Block b : blocks) {
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

    @Override
    public String toString() {
        String str = "Survey id " + sid + "\n";
        if (blocks.size() > 0) {
            for (int i = 0 ; i < blocks.size(); i ++)
                str = str + "\n" + blocks.get(i).toString();
        } else {
            for (Question q : questions)
                str = str +"\n" + q.toString();
        }
        return str;
    }

    private String dataString(Component c) {
        if (c instanceof StringComponent)
            return ((StringComponent) c).data;
        else return String.format("<p>%s</p>", ((URLComponent) c).data.toExternalForm());
    }

    public String toFileString() throws SurveyException{

        String newline = System.getProperty("line.separator");
        List<String> headers = Arrays.asList(otherHeaders);
        Collections.addAll(headers, knownHeaders);
        StringBuilder s = new StringBuilder(headers.get(0));

        // write headers
        for (int i = 1 ; i < headers.size() ; i++)
            s.append(String.format(",%s", headers.get(i)));
        s.append(newline);

        // write contents
        for (Question q : getQuestionsByIndex()) {
            boolean qWritten = false;
            for (Component opt : q.getOptListByIndex()) {
                boolean first = true;
                for (String header : headers) {
                    if (!first) s.append(",");
                    if (header==QUESTION && !qWritten) {
                        s.append("\"");
                        for (Component c : q.data)
                            if (c instanceof StringComponent)
                                s.append(dataString(c));
                        s.append("\"");
                    } else if (header==OPTIONS) {
                        s.append(String.format("\"%s\"", dataString(opt)));
                    } else if (header==RESOURCE) {
                        for (Component c : q.data)
                            if (c instanceof URLComponent)
                                s.append(String.format("\"%s\""));
                    } else if (header==Survey.BLOCK) {
                        s.append(String.format("\"%s\"", Block.idToString(q.block.id)));
                    } else if (header==Survey.BRANCH) {
                        s.append(String.format("\"%s\"", Block.idToString(q.branchMap.get(opt).id)));
                    } else if (header==Survey.CORRELATION) {
                        for (Map.Entry<String, List<Question>> entry : correlationMap.entrySet()) {
                            String id = entry.getKey();
                            boolean matched = false;
                            for (Question possibleMatch : entry.getValue())
                                if (q==possibleMatch) {
                                    s.append(String.format("\"%s\"", id));
                                    matched = true;
                                    break;
                                }
                            if (matched) break;
                        }
                    } else if (header==Survey.EXCLUSIVE) {
                        s.append(String.format("\"%b\"", q.exclusive));
                    } else if (header==Survey.FREETEXT) {
                        s.append(String.format("\"%b\"", q.freetext));
                    } else if (header==Survey.ORDERED) {
                        s.append(String.format("\"%b\"", q.ordered));
                    } else if (header==Survey.RANDOMIZE) {
                        s.append(String.format("\"%b\"", q.randomize));
                    }
                }
            }
            s.append(newline);
        }

        return s.toString();
    }
}

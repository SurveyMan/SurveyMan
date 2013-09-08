package survey;

import java.util.ArrayList;
import java.util.Collections;

import csv.CSVParser;
import qc.QCMetric;
import java.util.List;

import system.mturk.MturkLibrary;
import utils.Gensym;

public class Survey {

    private static final Gensym gensym = new Gensym("survey");

    public String sid = gensym.next();
    public List<Question> questions; //top level list of questions
    public QCMetric qc;
    public ArrayList<Block> blocks;
    public String encoding;
    public String[] otherHeaders;
    public String sourceName;
    public String source;

    public void randomize() throws SurveyException{
        // randomizes the question list according to the block structure
        if (blocks != null)
            for (Block b : blocks)
                b.randomize();
        else {
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
        for (Question q : questions)
            if (q.quid.equals(quid))
                return q;
        throw new QuestionNotFoundException(quid, sid);
    }
    
    public Question[] getQuestionsByIndex() throws SurveyException {
        Question[] qs = new Question[questions.size()];
        for (Question q: questions) {
            if (q.index > qs.length)
                throw new MalformedQuestionException(String.format("Question\r\n\"%s\"\r\n has an index that exceeds max index %d"
                        , q.toString()
                        , qs.length - 1));
            else if (qs[q.index] != null)
                throw new MalformedOptionException(String.format("Question \r\n\"%s\"\r\n and \r\n\"%s\"\r\n have the same index."
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
}

class QuestionNotFoundException extends SurveyException {
    public QuestionNotFoundException(String quid, String sid) {
        super(String.format("Question with id %s not found in survey with id %s", quid, sid));
    }
}

class MalformedQuestionException extends SurveyException {
    public MalformedQuestionException(String msg) {
        super(msg);
    }
}
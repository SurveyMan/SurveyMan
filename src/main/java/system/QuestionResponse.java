package system;

import com.google.gson.JsonObject;
import interstitial.IQuestionResponse;
import interstitial.OptTuple;
import interstitial.ResponseWriter;
import survey.Component;
import survey.Question;
import survey.StringComponent;
import survey.Survey;
import survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestionResponse implements IQuestionResponse {

    public static final String newline = ResponseWriter.newline;

    private Question q;
    private List<OptTuple> opts = new ArrayList<OptTuple>();
    private int indexSeen;

    @Override
    public boolean equals(Object that){
        if (that instanceof QuestionResponse) {
            return this.q.equals(((QuestionResponse) that).q)
                    && this.opts.equals(((QuestionResponse) that).opts);
        } else return false;
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder();
        for (OptTuple o : opts){
            s.append(o.c.toString());
        }
        return String.format(" (%s) %s : [ %s ]", q.quid, q.toString(), s.toString());
    }

    public QuestionResponse(){

    }

    public QuestionResponse(Survey s, String quid, int qpos) throws SurveyException {
        this.q = s.getQuestionById(quid);
        this.indexSeen = qpos;
    }

    public QuestionResponse(Question q) {
        this.q = q;
    }

    public String quid() {
        return q.quid;
    }

    /** otherValues is a map of the key value pairs that are not necessary for quality control,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    public Map<String, String> otherValues;

    public void add(String quid, OptTuple tupe, Map<String, String> otherValues) {
        this.otherValues = otherValues;
        if (this.q == null) {
            this.q = new Question(-1,-1);
            this.q.quid = quid;
        }
        this.opts.add(tupe);
        this.indexSeen = -1;
    }

    public void add(JsonObject response, Survey s, Map<String,String> otherValues) throws SurveyException {

        boolean custom = Question.customQuestion(response.get("quid").getAsString());

        if (this.otherValues == null)
            this.otherValues = otherValues;
        else
            assert(this.otherValues.equals(otherValues));

        if (custom){
            this.q = new Question(-1,-1);
            this.q.data = new StringComponent("CUSTOM", -1, -1);
            this.indexSeen = response.get("qpos").getAsInt();
            this.opts.add(new OptTuple(new StringComponent(response.get("oid").getAsString(), -1, -1), -1));
        } else {
            this.q = s.getQuestionById(response.get("quid").getAsString());
            this.indexSeen = response.get("qpos").getAsInt();
            if (q.freetext){
            } else {
                Component c = s.getQuestionById(q.quid).getOptById(response.get("oid").getAsString());
                int optloc = response.get("opos").getAsInt();
                this.opts.add(new OptTuple(c, optloc));
            }
        }
    }

    @Override
    public Question getQuestion() {
        return q;
    }

    @Override
    public List<OptTuple> getOpts() {
        return opts;
    }

    @Override
    public int getIndexSeen() {
        return indexSeen;
    }
}

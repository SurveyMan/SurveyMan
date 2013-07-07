package survey;

import com.amazonaws.mturk.requester.Assignment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.Gensym;
import scalautils.AnswerParse;
import scalautils.Response;

public class SurveyResponse {
    public static final Gensym gensym = new Gensym("sr");
    public final String srid = gensym.next();
    public String workerId = "";
    public List<QuestionResponse> responses = new ArrayList<QuestionResponse>();
    /** otherValues is a map of the key value pairs that are not necessary for QC,
     *  but are returned by the service. They should be pushed through the system
     *  and spit into an output file, unaltered.
     */
    Map<String, String> otherValues = new HashMap<String, String>();
    
    @Override
    public String toString() {
        String retval = "\nResponse for worker " + workerId + ":\n";
        for (QuestionResponse qr : responses)
            retval = retval + "\t" + qr.toString();
        return retval;
    }
        
    public SurveyResponse (Survey s, Assignment a) {
        this.workerId = a.getWorkerId();
        otherValues.put("acceptTime", a.getAcceptTime().toString());
        otherValues.put("approvalTime", a.getApprovalTime().toString());
        otherValues.put("rejectionTime", a.getRejectionTime().toString());
        otherValues.put("requesterFeedback", a.getRequesterFeedback().toString());
        otherValues.put("submitTime", a.getSubmitTime().toString());
        ArrayList<Response> rawResponses = AnswerParse.parse(s, a);
        for (Response r : rawResponses) {
            Question q = s.getQuestionById(r.quid());
            List<Component> opts = new ArrayList<Component>();
            for (String oid : r.opts())
                opts.add(q.getOptById(oid));
            this.responses.add(new QuestionResponse(q, opts, r.indexSeen()));
        }
    }
    
    public class QuestionResponse {

        public Question q;
        public List<Component> opts = new ArrayList<Component>();
        public int indexSeen; // the index at which this question was seen.

        /** otherValues is a map of the key value pairs that are not necessary for QC,
         *  but are returned by the service. They should be pushed through the system
         *  and spit into an output file, unaltered.
         */
        Map<String, String> otherValues = new HashMap<String, String>();
        
        public QuestionResponse(Question q, List<Component> opts, int indexSeen){
            this.q = q;
            this.opts = opts;
            this.indexSeen = indexSeen;
        }
        
        public String toString() {
            String retval = q.data.toString();
            for (Component c : opts) 
                retval = retval + "\n\t\t" + c.toString();
            return retval;
        }
    }
}
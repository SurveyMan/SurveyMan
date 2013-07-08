package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import survey.Survey;
import system.Library;
import java.util.HashMap;
import qc.QC;
import survey.SurveyException;
import survey.SurveyResponse;
import system.Runner;


public class ResponseManager {

    public static final String RESULTS = Library.OUTDIR + Library.fileSep + "results.csv";
    public static final String SUCCESS = Library.OUTDIR + Library.fileSep + "success.csv";
    private static final RequesterService service = new RequesterService(new PropertiesClientConfig(Library.CONFIG));

    public static SurveyResponse parseResponse(Assignment assignment, Survey survey) throws SurveyException {
        return new SurveyResponse(survey, assignment);
    }
    
    public static void addResponses(HashMap<String, SurveyResponse> responses, Survey survey, String hitid) throws SurveyException {
        Assignment[] assignments = service.getAllAssignmentsForHIT(hitid);
        for (Assignment a : assignments) {
            SurveyResponse sr = parseResponse(a, survey);
            if (QC.isBot(sr)) {
                service.rejectAssignment(a.getAssignmentId(), QC.BOT);
                service.blockWorker(a.getAssignmentId(), QC.BOT);
            } else {
                //service.assignQualification("survey", a.getWorkerId(), 1, false);
                responses.put(sr.srid, sr);
                service.approveAssignment(a.getAssignmentId(), "Thanks");
                service.forceExpireHIT(hitid);
            }
        }
    }
    
    public static boolean hasResponse(String hittypeid, String hitid){
        for (HIT hit : service.getAllReviewableHITs(hittypeid))
            if (hit.getHITId().equals(hitid))
                return true;
        return false;
    }
    
    public static boolean hasJobs() {
        boolean checked = false;
        boolean retval = true;
        while (! checked) {
            try {
                retval = service.searchAllHITs().length!=0;
                checked = true;
            } catch (Exception e) {
                System.err.println("WARNING: "+e.getMessage());
                try {
                    Thread.sleep(Runner.waitTime);
                } catch (InterruptedException ie) {}
            }
        }
        return retval;
    }
            
    public static void main(String[] args) {
        
    }

}

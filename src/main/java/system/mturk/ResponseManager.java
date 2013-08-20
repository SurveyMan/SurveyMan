package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import org.apache.log4j.Logger;
import survey.Survey;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import qc.QC;
import survey.SurveyException;
import survey.SurveyResponse;
import system.Runner;


public class ResponseManager {

    public static class Record {
        public Survey survey;
        public List<SurveyResponse> responses;
        public Properties parameters;

        public Record(Survey survey, List<SurveyResponse> responses, Properties parameters) {
            this.survey = survey;
            this.responses = responses;
            this.parameters = parameters;
        }
    }

    private static final Logger LOGGER = Logger.getLogger("system.mturk");

    public static final String RESULTS = MturkLibrary.OUTDIR + MturkLibrary.fileSep + "results.csv";
    public static final String SUCCESS = MturkLibrary.OUTDIR + MturkLibrary.fileSep + "success.csv";
    private static final RequesterService service = SurveyPoster.service;
    public static HashMap<Survey, Record> manager = new HashMap<Survey, Record>();

    public static SurveyResponse parseResponse(Assignment assignment, Survey survey) throws SurveyException {
        return new SurveyResponse(survey, assignment);
    }
    
    public static void addResponses(List<SurveyResponse> responses, Survey survey, String hitid) throws SurveyException {
        Assignment[] assignments = service.getAllAssignmentsForHIT(hitid);
        for (Assignment a : assignments) {
            SurveyResponse sr = parseResponse(a, survey);
            if (QC.isBot(sr)) {
                service.rejectAssignment(a.getAssignmentId(), QC.BOT);
                service.blockWorker(a.getAssignmentId(), QC.BOT);
            } else {
                //service.assignQualification("survey", a.getWorkerId(), 1, false);
                responses.add(sr);
                service.approveAssignment(a.getAssignmentId(), "Thanks");
                //service.forceExpireHIT(hitid);
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
                LOGGER.warn("WARNING: "+e.getMessage());
                try {
                    Thread.sleep(Runner.waitTime);
                } catch (InterruptedException ie) {}
            }
        }
        return retval;
    }
}

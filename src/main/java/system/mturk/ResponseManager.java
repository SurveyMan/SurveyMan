package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.service.axis.RequesterService;
import org.apache.log4j.Logger;
import survey.Survey;

import java.io.File;
import java.io.IOException;
import java.util.*;

import qc.QC;
import survey.SurveyException;
import survey.SurveyResponse;
import system.Library;
import system.Runner;


public class ResponseManager {

    public static class Record {

        public Survey survey;
        public String outputFileName;
        public List<SurveyResponse> responses;
        public Properties parameters;
        private Deque<HIT> hits;

        public Record(Survey survey, Properties parameters) {
            File outfile = new File(String.format("%s%s%s_%s_%s.csv"
                    , MturkLibrary.OUTDIR
                    , MturkLibrary.fileSep
                    , survey.sourceName
                    , survey.sid
                    , Library.TIME));
            try {
                outfile.createNewFile();
                this.outputFileName = outfile.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            this.survey = survey;
            this.responses = new ArrayList<SurveyResponse>();
            this.parameters = parameters;
            this.hits = new ArrayDeque<HIT>();
        }

        public void addNewHIT(HIT hit) {
            hits.push(hit);
        }

        public HIT getLastHIT(){
            return hits.peekLast();
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

    public static void renewIfExpired(Survey survey) {
        HIT hit = manager.get(survey).getLastHIT();
        if (hit.getExpiration().before(Calendar.getInstance())) {
            Calendar exprTime = Calendar.getInstance();
            long oldExprTime = hit.getExpiration().getTimeInMillis();
            long oldCreationTime = hit.getCreationTime().getTimeInMillis();
            exprTime.setTimeInMillis(exprTime.getTimeInMillis()+(oldExprTime-oldCreationTime));
            hit.setExpiration(exprTime);
        }
    }
    
    public static boolean hasResponse(String hittypeid, String hitid){
        for (HIT hit : service.getAllReviewableHITs(hittypeid))
            if (hit.getHITId().equals(hitid))
                return true;
        return false;
    }
    
    public static boolean hasJobs(Survey survey) {
        boolean checked = false;
        boolean retval = true;
        while (! checked) {
            try {
                retval = Arrays.asList(service.searchAllHITs()).contains(ResponseManager.manager.get(survey).getLastHIT());
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

    public static boolean hasEnoughFund() {
        double balance = SurveyPoster.service.getAccountBalance();
        LOGGER.info("Got account balance: " + RequesterService.formatCurrency(balance));
        return balance > 0;
    }

    private static List<HIT> hitTask(HITStatus inputStatus) {
        List<HIT> hits = new ArrayList<HIT>();
        String msg = "";
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            if (status.equals(inputStatus))
                hits.add(hit);
        }
        return hits;
    }

    public static List<HIT> unassignableHITs() {
        return hitTask(HITStatus.Unassignable);
    }

    public static List<HIT> deleteExpiredHITs() {
        List<HIT> hits = new ArrayList<HIT>();
        List<HIT> tasks = hitTask(HITStatus.Reviewable);
        tasks.addAll(hitTask(HITStatus.Reviewing));
        for (HIT hit : tasks)
            if (hit.getExpiration().getTimeInMillis() < Calendar.getInstance().getTimeInMillis()){
                hits.add(hit);
                service.disposeHIT(hit.getHITId());
            }
        return hits;
    }

    public static List<HIT> assignableHITs() {
        return hitTask(HITStatus.Assignable);
    }

    public static List<HIT> expireOldHITs() {
        List<HIT> expiredHITs = new ArrayList<HIT>();
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            if (! (status.equals(HITStatus.Reviewable) || status.equals(HITStatus.Reviewing))) {
                service.forceExpireHIT(hit.getHITId());
                expiredHITs.add(hit);
                LOGGER.info("Expired HIT:"+hit.getHITId());
            }
        }
        return expiredHITs;
    }
}

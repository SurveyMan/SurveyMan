package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.InternalServiceException;
import com.amazonaws.mturk.service.exception.ServiceException;
import org.apache.log4j.Logger;
import survey.Survey;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.*;

import qc.QC;
import survey.SurveyException;
import survey.SurveyResponse;
import system.Library;
import system.Runner;


public class ResponseManager {

    public static class Record {

        final public Survey survey;
        final public Properties parameters;
        final public String outputFileName;
        public List<SurveyResponse> responses;
        private Deque<HIT> hits;

        public Record(final Survey survey, final Properties parameters) throws IOException{
            File outfile = new File(String.format("%s%s%s_%s_%s.csv"
                    , MturkLibrary.OUTDIR
                    , MturkLibrary.fileSep
                    , survey.sourceName
                    , survey.sid
                    , Library.TIME));
            outfile.createNewFile();
            this.outputFileName = outfile.getCanonicalPath();
            this.survey = survey;
            this.responses = new ArrayList<SurveyResponse>();
            this.parameters = parameters;
            this.hits = new ArrayDeque<HIT>();
        }

        public void addNewHIT(HIT hit) {
            hits.push(hit);
        }

        public HIT getLastHIT(){
            return hits.peekFirst();
        }

        public HIT[] getAllHITs() {
            return this.hits.toArray(new HIT[hits.size()]);
        }

        public synchronized Record copy() throws IOException {
            Record r = new Record(this.survey, this.parameters);
            // don't expect responses to be removed or otherwise modified, so it's okay to just copy them over
            for (SurveyResponse sr : responses)
                r.responses.add(sr);
            // don't expect HITs to be removed either
            // double check to make sure this is being added in the proper direction
            r.hits.addAll(this.hits);
            return r;
        }
    }

    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    public static final String SUCCESS = MturkLibrary.OUTDIR + MturkLibrary.fileSep + "success.csv";
    private static final RequesterService service = SurveyPoster.service;
    public static HashMap<Survey, Record> manager = new HashMap<Survey, Record>();

    public static Record getRecord(Survey survey) throws IOException {
        synchronized (manager) {
            Record r = manager.get(survey);
            if (r!=null)
                return manager.get(survey).copy();
            else return null;
        }
    }

    public static List<HIT> listAvailableHITsForRecord (Record r) {
        List<HIT> hits = Arrays.asList(r.getAllHITs());
        ArrayList<HIT> retval = new ArrayList<HIT>();
        for (HIT hit : hits)
            if (! service.getHIT(hit.getHITId()).getHITStatus().equals(HITStatus.Assignable))
                retval.add(hit);
       return retval;
    }

    public static SurveyResponse parseResponse(Assignment assignment, Survey survey) throws SurveyException {
        return new SurveyResponse(survey, assignment);
    }
    
    public static void addResponses(List<SurveyResponse> responses, Survey survey, String hitid) throws SurveyException {
        boolean success = false;
        ArrayList<SurveyResponse> responsesToAdd = new ArrayList<SurveyResponse>();
        while (!success) {
            try{
                Assignment[] assignments = service.getAllAssignmentsForHIT(hitid);
                System.out.println("numassignments: "+assignments.length);
                for (Assignment a : assignments) {
                    SurveyResponse sr = parseResponse(a, survey);
                    if (QC.isBot(sr)) {
                        service.rejectAssignment(a.getAssignmentId(), QC.BOT);
                        service.blockWorker(a.getAssignmentId(), QC.BOT);
                    } else {
                        //service.assignQualification("survey", a.getWorkerId(), 1, false);
                        responsesToAdd.add(sr);
                        if (a.getAssignmentStatus().equals(AssignmentStatus.Submitted))
                            service.approveAssignment(a.getAssignmentId(), "Thanks");
                        //service.forceExpireHIT(hitid);
                    }
                    responses.addAll(responsesToAdd);
                }
                success=true;
            } catch (ServiceException se) {
                LOGGER.warn("addResponse"+se);
            }
        }
    }

    public static void renewIfExpired(String hitId, Properties params) {
        HIT hit = service.getHIT(hitId);
        if (hit.getExpiration().before(Calendar.getInstance()))
            service.extendHIT(hitId, 1, Long.valueOf(params.getProperty("hitlifetime")));
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
    
    public static boolean hasResponse(String hittypeid, String hitid) {
        try{
            for (HIT hit : service.getAllReviewableHITs(hittypeid))
                if (hit.getHITId().equals(hitid))
                    return true;
        } catch (InternalServiceException se) {
            LOGGER.info(se);
        }
        return false;
    }
    
    public static boolean hasJobs(Survey survey) {
        boolean checked = false;
        boolean retval = true;
        while (! checked) {
            try {
                retval = Arrays.asList(service.searchAllHITs()).contains(ResponseManager.getRecord(survey).getLastHIT());
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

    public static void expireHIT(HIT hit) {
        service.forceExpireHIT(hit.getHITId());
    }

    public static List<HIT> expireOldHITs() {
        List<HIT> expiredHITs = new ArrayList<HIT>();
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            if (! (status.equals(HITStatus.Reviewable) || status.equals(HITStatus.Reviewing))) {
                expireHIT(hit);
                expiredHITs.add(hit);
                LOGGER.info("Expired HIT:"+hit.getHITId());
            }
        }
        return expiredHITs;
    }

}

package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.InternalServiceException;
import com.amazonaws.mturk.service.exception.InvalidStateException;
import com.amazonaws.mturk.service.exception.ServiceException;
import csv.CSVParser;
import org.apache.log4j.Logger;
import survey.Survey;

import java.io.IOException;
import java.util.*;
import qc.QC;
import survey.SurveyException;
import survey.SurveyResponse;

/**
 * ResponseManager communicates with Mechanical Turk. This class contains methods to query the status of various HITs,
 * update current HITs, and pull results into a local database of responses for use inside another program.
 *
 */

public class ResponseManager {

    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    protected static final RequesterService service = SurveyPoster.service;
    /**
    * A map of the surveys launched during this session to their results.
    */
    public static HashMap<Survey, Record> manager = new HashMap<Survey, Record>();

    /**
     * Returns a copy of the Record {@link Record} of results for argument survey. This method synchronizes on manager,
     * so the current contents of the Record for this survey may be stale. If there is no Record recorded yet, the
     * method returns null.
     *
     * @param survey
     * @return a copy of the Record {@link Record} associated with this Survey {@link Survey}.
     * @throws IOException
     */
    public static Record getRecord(Survey survey) throws IOException {
        synchronized (manager) {
            Record r = manager.get(survey);
            if (r!=null)
                return manager.get(survey).copy();
            else return null;
        }
    }

    /**
     * Given a Record {@link Record}, this method loops through the HITs {@link HIT} registered for the Record {@link Record}
     * and returns a list of HITs {@link HIT}. Note that if the argument is generated using getRecord, the resulting
     * list of HITs may be stale. This is generally fine for most operations in SurveyMan, but if the list must be as
     * fresh as possible, synchronize on manager and get the record that way.
     *
     * @param r
     * @return a list of HITs associated with this Record (i.e. the value associated with a given Survey {@link Survey}
     *  in manager.
     */
    public static List<HIT> listAvailableHITsForRecord (Record r) {
        List<HIT> hits = Arrays.asList(r.getAllHITs());
        ArrayList<HIT> retval = new ArrayList<HIT>();
        for (HIT hit : hits)
            if (! service.getHIT(hit.getHITId()).getHITStatus().equals(HITStatus.Assignable))
                retval.add(hit);
       return retval;
    }

    private static SurveyResponse parseResponse(Assignment assignment, Survey survey)
            throws SurveyException {
        return new SurveyResponse(survey, assignment);
    }
    
    protected static void addResponses(Survey survey, String hitid)
            throws SurveyException, IOException {
        List<SurveyResponse> responses = getRecord(survey).responses;
        boolean success = false;
        ArrayList<SurveyResponse> responsesToAdd = new ArrayList<SurveyResponse>();
        while (!success) {
            try{
                Assignment[] assignments = service.getAllAssignmentsForHIT(hitid);
                for (Assignment a : assignments) {
                    SurveyResponse sr = parseResponse(a, survey);
                    if (QCAction.addAsValidResponse(QC.assess(sr), a))
                        responsesToAdd.add(sr);
                    responses.addAll(responsesToAdd);
                }
                success=true;
            } catch (ServiceException se) {
                LOGGER.warn("addResponse "+se);
            }
        }
    }

    /**
     * Tries to parse all of the Approved assignments into a SurveyResponse {@link SurveyResponse} list according to
     * some date window.
     *
     * @param survey
     * @return a list of survey responses
     */
    public static List<SurveyResponse> getOldResponsesByDate(Survey survey, Calendar from, Calendar to) throws SurveyException {
        List<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        for (HIT hit : service.searchAllHITs())
            if (hit.getCreationTime().after(from) && hit.getCreationTime().before(to))
                for (Assignment assignment : service.getAllAssignmentsForHIT(hit.getHITId()))
                    if (assignment.getAssignmentStatus().equals(AssignmentStatus.Approved))
                        responses.add(parseResponse(assignment, survey));
        return responses;
    }

    /**
     * For a specific HIT {@link  HIT} Id, this function will extent the HIT's lifetime by the same length
     * as the original setting. The original setting is provited in the params argument.
     * Both arguments are typically extracted from a Record {@link Record} object associated with a
     * particular Survey {@link Survey} object.
     *
     * @param hitId
     * @param params
     */
    public static void renewIfExpired(String hitId, Properties params) {
        boolean renewed = false;
        while (!renewed){
            try {                  
                HIT hit = service.getHIT(hitId);
                if (hit.getExpiration().before(Calendar.getInstance()))
                    service.extendHIT(hitId, 1, Long.valueOf(params.getProperty("hitlifetime")));
                renewed = true;
            }catch (InternalServiceException ise) {
                LOGGER.info(ise);
            }
        }
    }

    /**
     * Renews the expiration on all HITs {@link HIT} associated with a particular survey by the length of
     * time originally provided. This information is extracted from ResponseManager.manager
     *
     * @param survey
     */
    public static void renewAllIfExpired(Survey survey) {
        for (HIT hit : manager.get(survey).getAllHITs())
            if (hit.getExpiration().before(Calendar.getInstance())) {
                Calendar exprTime = Calendar.getInstance();
                long oldExprTime = hit.getExpiration().getTimeInMillis();
                long oldCreationTime = hit.getCreationTime().getTimeInMillis();
                exprTime.setTimeInMillis(exprTime.getTimeInMillis()+(oldExprTime-oldCreationTime));
                hit.setExpiration(exprTime);
            }
    }

    /**
     * Checks whether there is a completed survey available. If a Service Exception is thrown, this method
     * returns false.
     *
     * @param hitid
     * @return whether there is a response available for review
     */
    public static boolean hasResponse(String hitid) {
        try{
            return service.getHIT(hitid).getNumberOfAssignmentsPending() > 0;
        } catch (InternalServiceException se) {
            LOGGER.info(se);
        }
        return false;
    }

    /**
     * Checks whether there are any assignable {@link HITStatus} HITs. This blocks on the manager and should
     * be used sparingly.
     *
     * @param survey
     * @return whether there are any assignable HITs.
     */
    public static boolean hasJobs(Survey survey) {
        while (true) {
            try {
                synchronized (manager) {
                    Record record = manager.get(survey);
                    for (String hitid : record.getAllHITIds())
                        if (service.getHIT(hitid).getHITStatus().equals(HITStatus.Assignable))
                            return true;
                    return false;
                }
           } catch (Exception e) {
                LOGGER.warn("WARNING: "+e.getMessage());
                try {
                    Thread.sleep(Runner.waitTime);
                } catch (InterruptedException ie) {}
            }
        }
    }

    /**
     * Wrapper for returning the number of cents currently in the user's account.
     * @return
     */
    public static double getAccountBalance(){
        while (true) {
            try {
                return service.getAccountBalance();
            } catch (ServiceException se) {
                LOGGER.info(se);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
            }
        }
    }

    private static List<HIT> hitTask(HITStatus inputStatus) {
        List<HIT> hits = new ArrayList<HIT>();
        String msg = "";
        boolean found = false;
        while (!found) {
            try {
                for (HIT hit : service.searchAllHITs()){
                    found = true;
                    HITStatus status = hit.getHITStatus();
                    if (status.equals(inputStatus))
                        hits.add(hit);
                }
            } catch (InternalServiceException ise) {
                LOGGER.info(ise);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    LOGGER.info(ex);
                }
            }
        }
        return hits;
    }

    /**
     * Returns a list of all currently unassignable HITs {@link HIT}. Note that this may include previously
     * unassigned HITs from old sessions (that is, the HITs may not be linked to the Surveys currently held
     * in manager)
     * @return
     */
    public static List<HIT> unassignableHITs() {
        return hitTask(HITStatus.Unassignable);
    }

    /**
     * Deletes all expired HITs {@link HIT}.
     * @return  A list of the expired HITs.
     */
    public static List<HIT> deleteExpiredHITs() {
        List<HIT> hits = new ArrayList<HIT>();
        List<HIT> tasks = hitTask(HITStatus.Reviewable);
        tasks.addAll(hitTask(HITStatus.Reviewing));
        for (HIT hit : tasks)
            if (hit.getExpiration().getTimeInMillis() < Calendar.getInstance().getTimeInMillis()){
                hits.add(hit);
                boolean deleted = false;
                while (!deleted) {
                    try {
                        service.disposeHIT(hit.getHITId());
                        System.out.println("deleted : "+hit.getHITId());
                        deleted = true;
                    } catch (InternalServiceException ise) {
                        LOGGER.info(ise);
                    } catch (InvalidStateException ise) {
                        LOGGER.info(ise);
                        deleted = true;
                    }
                }
            }
        return hits;
    }

    /**
     * Gets a list of all the assignable HITs currently listed on Mechanical Turk. Note that this may
     * include previously assigned HITs from old sessions (that is, the HITs may not be linked to the
     * Surveys {@link Survey} currently held in manager)
     * @return
     */
    public static List<HIT> assignableHITs() {
        return hitTask(HITStatus.Assignable);
    }

    /**
     * Expires a specific HIT.
     * @param hit
     */
    public static void expireHIT(HIT hit) {
        service.forceExpireHIT(hit.getHITId());
    }

    /**
     * Expires all HITs that are not listed as Reviewable or Reviewing. Reviewable assignmenst are those for
     * which a worker has submitted a response. Reviewing is a special state that corresponds to a staging
     * area where jobs wait for review.
     * @return
     */
    public static List<HIT> expireOldHITs() {
        List<HIT> expiredHITs = new ArrayList<HIT>();
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            boolean expired = false;
            if (! (status.equals(HITStatus.Reviewable) || status.equals(HITStatus.Reviewing))) {
                while (!expired) {
                    try{
                        expireHIT(hit);
                        expiredHITs.add(hit);
                        LOGGER.info("Expired HIT:"+hit.getHITId());
                        expired = true;
                    }catch(InternalServiceException ise) {
                        LOGGER.warn(ise);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            LOGGER.info(ex);
                        }
                    }
                }
            }
        }
        return expiredHITs;
    }

    public static void main(String[] args) throws IOException, SurveyException {
        Calendar from = Calendar.getInstance();
        from.set(Calendar.YEAR, Calendar.SEPTEMBER, 8);
        Calendar to = Calendar.getInstance();
        to.set(Calendar.YEAR, Calendar.SEPTEMBER, 13);
        Survey survey = CSVParser.parse("/Users/etosch/Downloads/SurveymanLicious2", "\\t");
        List<SurveyResponse> responses = getOldResponsesByDate(survey, from, to);
        Record record = new Record(survey, MturkLibrary.props);
        record.responses = responses;
        Runner.writeResponses(survey, record);
    }

}

package system.mturk;

import com.amazonaws.mturk.addon.BatchItemCallback;
import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.AssignmentStatus;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.InternalServiceException;
import com.amazonaws.mturk.service.exception.ServiceException;
import csv.CSVParser;
import org.apache.log4j.Logger;
import survey.Survey;

import java.io.IOException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
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

    private static final Logger LOGGER = Logger.getLogger(Runner.class);
    protected static RequesterService service = SurveyPoster.service;


    /**
    * A map of the surveys launched during this session to their results.
    */
    public static HashMap<Survey, Record> manager = new HashMap<Survey, Record>();


    protected static void chill(int seconds){
        try {
            Thread.sleep(seconds*1000);
        } catch (InterruptedException e) {}
    }

    //************** Wrapped Calls to MTurk ******************//
    public static HIT getHIT(String hitid){
        while (true) {
            synchronized (service) {
                try {
                    HIT hit = service.getHIT(hitid);
                    return hit;
                } catch (InternalServiceException ise) { chill(2); }
            }
        }
    }

    private static Assignment[] getAllAssignmentsForHIT(String hitid) {
        while (true) {
            synchronized (service) {
                try {
                    Assignment[] assignments = service.getAllAssignmentsForHIT(hitid);
                    return assignments;
                } catch (InternalServiceException ise) { chill(2); }
            }
        }
    }

    private static HIT[] searchAllHITs () {
        int waittime = 1;
        while (true) {
            synchronized (service) {
                try{
                    HIT[] hits = service.searchAllHITs();
                    System.out.println(String.format("Found %d HITs", hits.length));
                    LOGGER.info(String.format("Found %d HITs", hits.length));
                    return hits;
                } catch (InternalServiceException ise) {
                    LOGGER.warn(ise);
                    chill(waittime);
                    waittime = waittime*2;
                }
            }
        }
    }

    private static void extendHIT(String hitd, Integer maxAssignmentsIncrement, Long expirationIncrementInSeconds) {
        while (true){
            try {
                service.extendHIT(hitd, maxAssignmentsIncrement, expirationIncrementInSeconds);
                return;
            } catch (InternalServiceException ise) { chill(2); }
        }
    }

    private static void extendHITs (List<String> hitidlist, Integer maxAssignmentsIncrement, Long expirationIncrementInSeconds, final Survey survey) {
        String[] hitids = (String[]) hitidlist.toArray();
        while(true) {
            synchronized (service) {
                try {
                    service.extendHITs(hitids
                            , maxAssignmentsIncrement
                            , expirationIncrementInSeconds
                            , new BatchItemCallback() {
                                @Override
                                public void processItemResult(Object itemId, boolean succeeded, Object result, Exception itemException) {
                                    // update local copies of hits to indicate that the HIT was extended
                                    Record record = ResponseManager.manager.get(survey);
                                    for (HIT hit : record.getAllHITs())
                                        if (hit.getHITId().equals((String) itemId)) {
                                            hit.setExpiration(((HIT) result).getExpiration());
                                            String msg = "updated expiration date of hit "+(String) itemId;
                                            ResponseManager.LOGGER.info(msg);
                                            System.out.println(msg);
                                        }
                                }
                            });
                    return;
                } catch (InternalServiceException ise) { chill(2); }
            }
        }
    }

    private static void deleteHIT(String hitid) {
        while (true) {
            synchronized (service) {
                try {
                    service.disposeHIT(hitid);
                    return;
                } catch (InternalServiceException ise) { chill(1); }
            }
        }
    }
    private static void deleteHITs(List<String> hitids) {
        synchronized (service) {
            for (String hitid : hitids) {
                while (true) {
                    try {
                        service.disposeHIT(hitid);
                        break;
                    } catch (InternalServiceException ise) { chill(1); }
                }
            }
        }
    }

    private static void approveAssignments(List<String> assignmentids) {
        String msg = String.format("Attempting to approve %d assignments", assignmentids.size());
        System.out.println(msg);
        LOGGER.info(msg);
        // call to the batch assignment approval method never terminated.
        // synch outside the foor loop so new things arent posted in the interim.
        synchronized (service) {
            for (String assignmentid : assignmentids) {
                while (true) {
                    try {
                        service.approveAssignment(assignmentid, "Thanks.");
                        System.out.println("Approved "+assignmentid);
                        LOGGER.info("Approved "+assignmentid);
                        break;
                    } catch (InternalServiceException ise) { chill(1); }
                }
            }
        }
    }

    /**
     * Expires a specific HIT.
     * @param hit
     */
    public static void expireHIT(HIT hit) {
        while (true){
            synchronized (service) {
                try{
                    service.forceExpireHIT(hit.getHITId());
                    return;
                }catch(InternalServiceException ise){ chill(1); }
            }
        }
    }

    public static void expireHITs(List<String> hitids) {
        synchronized (service) {
            for (String hitid : hitids) {
                while(true) {
                    try {
                        service.forceExpireHIT(hitid);
                        String msg = String.format("Expired hit %s", hitid);
                        LOGGER.info(msg);
                        System.out.println(msg);
                        break;
                    } catch (InternalServiceException ise) { chill(1); }
                }
            }
        }
    }

    public static String getWebsiteURL() {
        synchronized (service) {
            while(true) {
                try {
                    String websiteURL = service.getWebsiteURL();
                    return websiteURL;
                } catch (InternalServiceException ise) { chill(1); }
            }
        }
    }

    public static String createHIT(String title, String description, String keywords, String xml, double reward, long assignmentDuration, long maxAutoApproveDelay, long lifetime) {
        int waittime = 1;
        synchronized (service) {
            while(true) {
                try {
                    HIT hitid = service.createHIT(null, title, description, keywords, xml, reward, assignmentDuration, maxAutoApproveDelay, lifetime, 1, "", null, null);
                    return hitid.getHITId();
                } catch (InternalServiceException ise) {
                    LOGGER.info(ise);
                    chill(waittime);
                    waittime = waittime*2;
                }
            }
        }
    }
    //***********************************************************//

    /**
     * Returns a copy of the Record {@link Record} of results for argument survey. This method synchronizes on manager,
     * so the current contents of the Record for this survey may be stale. If there is no Record recorded yet, the
     * method returns null.
     *
     * @param survey
     * @return a copy of the Record {@link Record} associated with this Survey {@link Survey}.
     * @throws IOException
     */
    public static Record getRecord(Survey survey) 
            throws IOException, SurveyException {
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
        for (HIT hit : hits) {
            HIT thishit = getHIT(hit.getHITId());
            if (thishit.getHITStatus().equals(HITStatus.Assignable))
                retval.add(hit);
        }
       return retval;
    }

    private static SurveyResponse parseResponse(Assignment assignment, Survey survey)
            throws SurveyException, IOException {
        Record record = ResponseManager.getRecord(survey);
        return new SurveyResponse(survey, assignment, record);
    }

    protected static void addResponses(Survey survey, String hitid)
            throws SurveyException, IOException {
        boolean success = false;
        Record r = manager.get(survey);
        // references to things in the record
        List<SurveyResponse> responses = r.responses;
        List<SurveyResponse> botResponses = r.botResponses;
        QC qc = r.qc;
        // local vars
        List<SurveyResponse> validResponsesToAdd = new ArrayList<SurveyResponse>();
        List<SurveyResponse> randomResponsesToAdd = new ArrayList<SurveyResponse>();

        while (!success) {
            try{
                Assignment[] assignments = getAllAssignmentsForHIT(hitid);
                for (Assignment a : assignments) {
                    SurveyResponse sr = parseResponse(a, survey);
                    if (QCAction.addAsValidResponse(qc.assess(sr), a, sr))
                        validResponsesToAdd.add(sr);
                    else randomResponsesToAdd.add(sr);
                }
                responses.addAll(validResponsesToAdd);
                botResponses.addAll(randomResponsesToAdd);
                success=true;
            } catch (ServiceException se) {
                LOGGER.warn("ServiceException in addResponses "+se);
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
    public static List<SurveyResponse> getOldResponsesByDate(Survey survey, Calendar from, Calendar to) 
            throws SurveyException, IOException {
        List<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        for (HIT hit : searchAllHITs())
            if (hit.getCreationTime().after(from) && hit.getCreationTime().before(to))
                for (Assignment assignment : getAllAssignmentsForHIT(hit.getHITId()))
                    if (assignment.getAssignmentStatus().equals(AssignmentStatus.Approved))
                        responses.add(parseResponse(assignment, survey));
        return responses;
    }

    /**
     * For a specific HIT {@link  HIT} Id, this function will extent the HIT's lifetime by the same length
     * as the original setting. The original setting is provided in the params argument.
     * Both arguments are typically extracted from a Record {@link Record} object associated with a
     * particular Survey {@link Survey} object.
     *
     * @param hitId
     * @param params
     */
    public static boolean renewIfExpired(String hitId, Properties params) {
        HIT hit = getHIT(hitId);
        if (hit.getExpiration().before(Calendar.getInstance())) {
            long extension = Long.valueOf(params.getProperty("hitlifetime"));
            extendHIT(hitId, 1, extension>=60?extension:60);
            return true;
        } else return false;
    }

    /**
     * Renews the expiration on all HITs {@link HIT} associated with a particular survey by the length of
     * time originally provided. This information is extracted from ResponseManager.manager
     *
     * @param survey
     */
    public static void renewAllIfExpired(Survey survey) throws IOException {
        Record record = ResponseManager.manager.get(survey);
        List<String> toExtend = new LinkedList<String>();
        long extendBy = Long.parseLong(record.parameters.getProperty("assignmentduration"));
        synchronized (record) {
            for (HIT hit : record.getAllHITs())
                if (hit.getExpiration().before(Calendar.getInstance()))
                    toExtend.add(hit.getHITId());
            extendHITs(toExtend, 1, extendBy, survey);
        }
    }

    /**
     * Checks whether there are any assignable {@link HITStatus} HITs. This blocks on the manager and should
     * be used sparingly.
     *
     * @param survey
     * @return whether there are any assignable HITs.
     */
    public static boolean hasJobs(Survey survey) {
        Record record = manager.get(survey);
        synchronized (record) {
            for (String hitid : record.getAllHITIds())
                if (getHIT(hitid).getHITStatus().equals(HITStatus.Assignable))
                    return true;
            return false;
        }
    }

    /**
     * Wrapper for returning the number of cents currently in the user's account.
     * @return
     */
    public static double getAccountBalance(){
        while (true) {
            try {
                double balance = service.getAccountBalance();
                return balance;
            } catch (ServiceException se) {
                LOGGER.info(se);
                chill(1);
            }
        }
    }

    private static List<HIT> hitTask(HITStatus inputStatus) {
        List<HIT> hits = new ArrayList<HIT>();
        HIT[] hitarray = searchAllHITs();
        for (HIT hit : hitarray){
            HITStatus status = hit.getHITStatus();
            if (status.equals(inputStatus))
                hits.add(hit);
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
     * Deletes all expired HITs {@link HIT}. Also approves any pending assignments.
     * @return  A list of the expired HITs.
     */
    public static List<HIT> deleteExpiredHITs() {
        List<HIT> hits = new ArrayList<HIT>();
        List<String> assignments = new ArrayList<String>();
        List<String> hitids = new ArrayList<String>();
        List<HIT> tasks = hitTask(HITStatus.Reviewable);
        tasks.addAll(hitTask(HITStatus.Reviewing));
        for (HIT hit : tasks)
            if (hit.getExpiration().getTimeInMillis() < Calendar.getInstance().getTimeInMillis()){
                hits.add(hit);
                hitids.add(hit.getHITId());
            }
        for (HIT hit : hits) {
            Assignment[] assignmentsForHIT = getAllAssignmentsForHIT(hit.getHITId());
            for (Assignment a : assignmentsForHIT)
                if (a.getAssignmentStatus().equals(AssignmentStatus.Submitted))
                    assignments.add(a.getAssignmentId());
        }
        approveAssignments(assignments);
        deleteHITs(hitids);
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
     * Expires all HITs that are not listed as Reviewable or Reviewing. Reviewable assignmenst are those for
     * which a worker has submitted a response. Reviewing is a special state that corresponds to a staging
     * area where jobs wait for review.
     * @return
     */
    public static List<HIT> expireOldHITs() {
        List<String> expiredHITIds = new LinkedList<String>();
        List<HIT> expiredHITs = new LinkedList<HIT>();
        HIT[] hitarray = searchAllHITs();
        for (HIT hit : hitarray){
            HITStatus status = hit.getHITStatus();
            if (! (status.equals(HITStatus.Reviewable) || status.equals(HITStatus.Reviewing))) {
                expiredHITs.add(hit);
                expiredHITIds.add(hit.getHITId());
            }
        }
        expireHITs(expiredHITIds);
        String msg = String.format("Expired %d HITs", expiredHITs.size());
        LOGGER.info(msg);
        System.out.println(msg);
        return expiredHITs;
    }

    public static void approveAllHITs(){
        HIT[] hits = searchAllHITs();
        List<String> assignmentidlist = new LinkedList<String>();
        for (HIT hit : hits){
            Assignment[] assignments = getAllAssignmentsForHIT(hit.getHITId());
            for (Assignment assignment : assignments)
                if (assignment.getAssignmentStatus().equals(AssignmentStatus.Submitted))
                    assignmentidlist.add(assignment.getAssignmentId());
        }
        String msg1 = String.format("Attempting to approve %d assignments", assignmentidlist.size());
        System.out.println(msg1);
        approveAssignments(assignmentidlist);
        String msg2 = String.format("Approved %d assignments.", assignmentidlist.size());
        System.out.println(msg2);
        LOGGER.info(msg1 + "\n" + msg2);
    }

    public static void main(String[] args) throws IOException, SurveyException {
        if (args.length < 4) {
            System.err.println("Usage :\n" +
                    "\tjava -cp path/to/surveyman.jar system.mturk.ResponseManager <fromDate> <toDate> <filename> <sep>\n" +
                    "\twhere\n" +
                    "\t<fromDate>, <toDate>\tare dates formatted as YYYYMMDD (e.g. Jan 1, 2013 would be 20130101)\n" +
                    "\t<filename>\t\t\t\tis the (relative or absolute) path to the file of interest\n" +
                    "\t<sep>\t\t\t\t\tis the field separator\n");
        } else {
            SurveyPoster.init();
            Calendar from = Calendar.getInstance();
            from.set(Integer.parseInt(args[0].substring(0,4)), Integer.parseInt(args[0].substring(4,6)), Integer.parseInt(args[0].substring(6,8)));
            System.out.println("From Date:"+new SimpleDateFormat().format(from.getTime(), new StringBuffer(), new FieldPosition(DateFormat.DATE_FIELD)));
            Calendar to = Calendar.getInstance();
            to.set(Integer.parseInt(args[1].substring(0,4)), Integer.parseInt(args[1].substring(4,6)), Integer.parseInt(args[1].substring(6,8)));
            System.out.println("To Date:"+new SimpleDateFormat().format(to.getTime(), new StringBuffer(), new FieldPosition(DateFormat.DATE_FIELD)));
            Survey survey = CSVParser.parse(args[2], args[3]);
            List<SurveyResponse> responses = getOldResponsesByDate(survey, from, to);
            Record record = new Record(survey, MturkLibrary.props);
            record.responses = responses;
            Runner.writeResponses(survey, record);
            System.out.println(String.format("Response can be found in %s", record.outputFileName));
        }
    }
}

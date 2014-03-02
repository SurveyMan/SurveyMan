package system.mturk;

import com.amazonaws.mturk.requester.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.InternalServiceException;
import com.amazonaws.mturk.service.exception.ObjectAlreadyExistsException;
import com.amazonaws.mturk.service.exception.ObjectDoesNotExistException;
import com.amazonaws.mturk.service.exception.ServiceException;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;
import survey.Survey;
import java.io.IOException;
import java.text.*;
import java.util.*;
import qc.QC;
import survey.SurveyException;
import survey.SurveyResponse;
import system.BackendType;
import system.Gensym;
import system.Record;
import system.Runner;
import system.interfaces.ResponseManager;
import system.interfaces.Task;
import javax.xml.parsers.ParserConfigurationException;
import static java.text.MessageFormat.*;

public class MturkResponseManager extends ResponseManager {

    protected static class CreateHITException extends SurveyException {
        public CreateHITException(String title) {
            super(String.format("Unable to create HIT for survey \"%s\"", title));
        }
    }

    private static final Logger LOGGER = Logger.getLogger(MturkResponseManager.class);
    public static RequesterService service;
    final protected static long maxAutoApproveDelay = 2592000l;
    final private static Gensym gensym = new Gensym("qual");


    private static boolean overTime(String name, int waittime){
        if (waittime > MturkResponseManager.maxwaittime){
          LOGGER.warn(String.format("Wait time in %s has exceeded max wait time. Cancelling request.", name));
          return true;
        } else return false;
    }


    //************** Wrapped Calls to MTurk ******************//


    public Task getTask(String taskId){
        String name = "getTask";
        int waittime = 1;
        while (true) {
            synchronized (MturkSurveyPoster.service) {
                try {
                    HIT hit = MturkSurveyPoster.service.getHIT(taskId);
                    LOGGER.info(String.format("Retrieved HIT %s", hit.getHITId()));
                    return new MturkTask(hit);
                } catch (InternalServiceException ise) {
                    if (overTime(name, waittime)) {
                        LOGGER.error(String.format("%s ran over time", name));
                        return null;
                    }
                    LOGGER.warn(format("{0} {1}", name, ise));
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }

    public static Assignment[] getAllAssignmentsForHIT(String hitid, AssignmentStatus[] statuses){
        String name = "getAllAssignmentsForHIT";
        int waittime = 1;
        while (true) {
            synchronized (service) {
                try {
                    Assignment[] hit = service.getAllAssignmentsForHIT(hitid, statuses);
                    LOGGER.info(String.format("Retrieved %d assignments for %s", hit.length, hitid));
                    return hit;
                } catch (InternalServiceException ise) {
                    if (overTime(name, waittime)) {
                        LOGGER.error(String.format("%s ran over time", name));
                        return null;
                    }
                    LOGGER.warn(format("{0} {1}", name, ise));
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }

    public static Assignment[] getAllAssignmentsForHIT(String hitID) {
        return getAllAssignmentsForHIT(hitID, new AssignmentStatus[]{AssignmentStatus.Rejected, AssignmentStatus.Approved, AssignmentStatus.Submitted});
    }

    public static void approveRejectedAssignment(String assignmentId) {
        String name = "approveRejectedAssignment";
        int waittime = 1;
        while (true) {
            synchronized (service) {
                try {
                    service.approveRejectedAssignment(assignmentId, "This assignment was incorrectly rejected.");
                    LOGGER.info(String.format("Approved assignment %s", assignmentId));
                    return;
                } catch (InternalServiceException ise) {
                    if (overTime(name, waittime)) {
                        LOGGER.error(String.format("%s ran over time", name));
                        return;
                    }
                    LOGGER.warn(format("{0} {1}", name, ise));
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }

    public static void grantBonus(String workerId, double amount, String assignmentId, String message) {
        String name = "grantBonus";
        int waittime = 1;
        while (true) {
            synchronized (service) {
                try {
                    service.grantBonus(workerId, amount, assignmentId, message);
                    LOGGER.info(String.format("Granted bonus of %f to %s", amount, workerId));
                    return;
                } catch (InternalServiceException ise) {
                    if (overTime(name, waittime)) {
                        LOGGER.error(String.format("%s ran over time", name));
                        return;
                    }
                    LOGGER.warn(format("{0} {1}", name, ise));
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }

    public static HIT[] getAllReviewableHITs(String hitTypeId){
        String name = "getAllReviewableHITs";
        int waittime = 1;
        while (true) {
            synchronized (service) {
                try {
                    HIT[] hits = service.getAllReviewableHITs(hitTypeId);
                    LOGGER.info(String.format("Retrieved %d HIT in %s", hits.length, name));
                    return hits;
                } catch (InternalServiceException ise) {
                    if (overTime(name, waittime)) {
                        LOGGER.error(String.format("%s ran over time", name));
                        return null;
                    }
                    System.out.println(ise.getMessage());
                    LOGGER.warn(format("{0} {1}", name, ise));
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }

    private static List<Assignment> getAllAssignmentsForHIT(HIT hit) {
        String name = "getAllAssignmentsForHIT";
        int waittime = 1;
        while (true) {
            synchronized (MturkSurveyPoster.service) {
                try {
                    Assignment[] hitAssignments = MturkSurveyPoster.service.getAllAssignmentsForHIT(hit.getHITId());
                    List<Assignment> assignments = new LinkedList<Assignment>();
                    boolean addAll = assignments.addAll(Arrays.asList(hitAssignments));
                    if (addAll)
                        LOGGER.info(String.format("Retrieved %d assignments for HIT %s", hitAssignments.length, hit.getHITId()));
                    return assignments;
                } catch (InternalServiceException ise) { 
                  LOGGER.warn(format("{0} {1}", name, ise));
                  chill(waittime); 
                  waittime *= 2;
                }
            }
        }
    }

    public static HIT[] searchAllHITs () {
        String name = "searchAllHITs";
        int waittime = 1;
        while (true) {
            synchronized (MturkSurveyPoster.service) {
                try{
                    System.out.println(MturkSurveyPoster.service.getWebsiteURL());
                    HIT[] hits = MturkSurveyPoster.service.searchAllHITs();
                    System.out.println(String.format("Found %d HITs", hits.length));
                    LOGGER.info(String.format("Found %d HITs", hits.length));
                    return hits;
                } catch (InternalServiceException ise) {
                    LOGGER.warn(format("{0} {1}", name, ise));
                    if (overTime(name, waittime))
                      return null;
                    chill(waittime);
                    waittime = waittime*2;
                }
            }
        }
    }

    public boolean makeTaskAvailable(String taskId, Record record) {
        String name = "makeTaskAvailable";
        int waitTime = 1;
        while (true){
            try {
                MturkTask task = (MturkTask) getTask(taskId);
                int currentMaxAssignments = MturkSurveyPoster.service.getAllAssignmentsForHIT(task.getTaskId()).length;
                int maxAssignmentsIncrement = Integer.parseInt(record.library.props.getProperty("numparticipants")) - currentMaxAssignments;
                long expirationIncrementMillis = task.hit.getExpiration().getTimeInMillis() - System.currentTimeMillis();
                MturkSurveyPoster.service.extendHIT(taskId, maxAssignmentsIncrement, expirationIncrementMillis / 1000);
                return true;
            } catch (InternalServiceException ise) {
                LOGGER.warn(format("{0} {1}", name, ise));
                if (overTime(name, waitTime))
                    return false;
                chill(waitTime);
                waitTime = 2 * waitTime;
            }
        }
    }

    private static void deleteHITs(List<String> hitids) {
        String name = "deleteHITs";
        synchronized (service) {
            for (String hitid : hitids) {
                int wait = 1;
                while (true) {
                    try {
                        service.disposeHIT(hitid);
                        break;
                    } catch (InternalServiceException ise) {
                      LOGGER.warn(format("{0} {1}", name, ise));
                      if (overTime(name, wait))
                        return;
                      chill(wait);
                      wait *= 2;
                    }
                }
            }
        }
    }

    private static void approveAssignments(List<String> assignmentids) {
        String msg = String.format("Attempting to approve %d assignments", assignmentids.size());
        String name = "approveAssignments";
        System.out.println(msg);
        LOGGER.info(msg);
        int waittime = 1;
        // call to the batch assignment approval method never terminated.
        // synch outside the foor loop so new things arent posted in the interim.
        synchronized (service) {
            for (String assignmentid : assignmentids) {
                while (true) {
                    try {
                        service.approveAssignment(assignmentid, "Thanks.");
                        System.out.println("Approved " + assignmentid);
                        LOGGER.info("Approved " + assignmentid);
                        break;
                    } catch (InternalServiceException ise) { 
                      LOGGER.warn(format("{0} {1}", name, ise));
                      if (overTime(name, waittime))
                        return;
                      chill(1);
                      waittime *= 2;
                    }
                }
            }
        }
    }

    public boolean makeTaskUnavailable(Task task) {
        String name = "expireHIT";
        while (true){
            synchronized (MturkSurveyPoster.service) {
                try{
                    MturkSurveyPoster.service.forceExpireHIT(task.getTaskId());
                    return true;
                }catch(InternalServiceException ise){
                  LOGGER.warn(MessageFormat.format("{0} {1}", name, ise));
                  chill(1);
                }catch(ObjectDoesNotExistException odne) {
                  LOGGER.warn(MessageFormat.format("{0} {1}", name, odne));
                  return false;
                }
            }
        }
    }

    @Override
    public void addTaskToRecordByTaskId(Record r, String tid) {
        HIT hit = MturkSurveyPoster.service.getHIT(tid);
        MturkTask task = new MturkTask(hit);
        r.addNewTask(task);
    }

    public static void expireHITs(List<String> hitids) {
        String name = "expireHITs";
        synchronized (service) {
            for (String hitid : hitids) {
                while(true) {
                    try {
                        MturkSurveyPoster.service.forceExpireHIT(hitid);
                        String msg = String.format("Expired hit %s", hitid);
                        LOGGER.info(msg);
                        System.out.println(msg);
                        break;
                    } catch (InternalServiceException ise) { 
                      LOGGER.warn(MessageFormat.format("{0} {1}", name, ise));
                      chill(1); 
                    }
                }
            }
        }
    }

    public static String getWebsiteURL() {
        String name = "getWebsiteURL";
        synchronized (MturkSurveyPoster.service) {
            while(true) {
                try {
                    String websiteURL = MturkSurveyPoster.service.getWebsiteURL();
                    return websiteURL;
                } catch (InternalServiceException ise) {
                    LOGGER.warn(MessageFormat.format("{0} {1}", name, ise));
                  chill(3); 
                }
            }
        }
    }

    /*
    protected static QualificationRequirement answerOnce(Record record){
        assert(record!=null);
        assert(record.qualificationType!=null);
        return new QualificationRequirement(
                  record.qualificationType.getQualificationTypeId()
                , Comparator.NotEqualTo
                , 1
                , null
                , false
        );
    }

    public static QualificationRequirement assignOneWorker(Record record, String workerId) {
        String name = "assignQualification";
        int waittime = 1;
        synchronized (service) {
            while(true) {
                try {
                    System.out.println(workerId);
                    System.out.println(record.qualificationType.getQualificationTypeId());
                    System.out.println("all qualification types" + service.getAllQualificationTypes().length);
                    service.assignQualification(record.qualificationType.getQualificationTypeId(), workerId, 1, false);
                } catch (InternalServiceException ise) {
                    LOGGER.warn(MessageFormat.format("{0}{1}", name, ise));
                    if(overTime(name, waittime))
                        LOGGER.warn("Could not assign qualification to "+workerId);
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }

    public static QualificationRequirement minHITsApproved(int minNum) {
        String name = "minHITsApproved";
        String qualId = "00000000000000000040";
        int waittime = 1;
        synchronized (service) {
            while(true) {
                try {
                    QualificationType qualificationType = service.getQualificationType(qualId);

                    return new QualificationRequirement(
                              qualificationType.getQualificationTypeId()
                            , Comparator.GreaterThan
                            , minNum
                            , null
                            , false
                    );
                } catch (InternalServiceException ise) {
                    LOGGER.warn(MessageFormat.format("{0}{1}", name, ise));
                    if (overTime(name, waittime))
                        LOGGER.warn(String.format("Could not fetch qualification for Worker_NumberHITsApproved (%s)", qualId));
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }

    public static QualificationRequirement minPercentApproval(int quantile) {
        String name = "minPercentApproval";
        String qualId = "000000000000000000L0";
        int waittime = 1;
        synchronized (service) {
            while (true) {
                try {
                    QualificationType qualificationType = service.getQualificationType(qualId);
                    return new QualificationRequirement(
                                qualificationType.getQualificationTypeId()
                            , Comparator.GreaterThanOrEqualTo
                            , quantile
                            , null
                            , false
                        );
                } catch (InternalServiceException ise) {
                    LOGGER.warn(MessageFormat.format("{0}{1}", name, ise));
                    if (overTime(name, waittime))
                        LOGGER.warn(String.format("Could not fetch qualification for Worker_​PercentAssignmentsApproved (%s)", qualId));
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }
*/
    public static String registerNewHitType(Record record) throws CreateHITException {
        String name = "registerNewHitType";
        String hittypeid = record.survey.sid+gensym.next()+MturkLibrary.TIME;
        int waittime = 1;

        synchronized (MturkSurveyPoster.service) {
            while(true) {
                try {
                    Properties props = record.library.props;
                    String keywords = props.getProperty("keywords");
                    String description = "Repeat customer";
                    QualificationType qualificationType = MturkSurveyPoster.service.createQualificationType(
                              hittypeid
                            , keywords
                            , description
                            , QualificationTypeStatus.Active
                            , new Long(Integer.MAX_VALUE)
                            , null //test
                            , null //answer key
                            , null //test duration
                            , true //autogranted
                            , 0 //integer autogranted (count of 0)
                        );
                    assert(qualificationType != null);
                    //record.qualificationType = qualificationType;
                    //QualificationRequirement qr = answerOnce(record);
                    //QualificationRequirement fiftyPercent = minPercentApproval(80);
                    //QualificationRequirement atLeastOne = minHITsApproved(1);
                    String hitTypeId = MturkSurveyPoster.service.registerHITType(
                              maxAutoApproveDelay
                            , Long.parseLong(props.getProperty("assignmentduration"))
                            , Double.parseDouble((String) props.get("reward"))
                            , props.getProperty("title")
                            , props.getProperty("keywords")
                            , props.getProperty("description")
                            , null //new QualificationRequirement[]{ fiftyPercent, atLeastOne }
                        );
                    record.hitTypeId = hitTypeId;
                    LOGGER.info(String.format("Qualification id: (%s)", qualificationType.getQualificationTypeId()));
                    return hitTypeId;
                } catch (InternalServiceException ise) {
                    LOGGER.warn(MessageFormat.format("{0} {1}", name, ise));
                    if (overTime(name, waittime)) {
                      throw new CreateHITException(record.library.props.getProperty("title"));
                    }
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }

    public static String createHIT(String title, String description, String keywords, String xml, double reward
            , long assignmentDuration, long maxAutoApproveDelay, long lifetime, int assignments, String hitTypeId)
            throws ParseException, SurveyException {
        System.out.println(getWebsiteURL());
        String name = "createHIT";
        int waittime = 1;
        synchronized (MturkSurveyPoster.service) {
            while(true) {
                try {
                    HIT hitid = MturkSurveyPoster.service.createHIT(hitTypeId
                            , title
                            , description
                            , keywords
                            , xml
                            , reward
                            , assignmentDuration
                            , maxAutoApproveDelay
                            , lifetime
                            , assignments
                            , ""
                            , null // Qualification requirements
                            , null
                        );
                    return hitid.getHITId();
                } catch (InternalServiceException ise) {
                    LOGGER.info(MessageFormat.format("{0} {1}", name, ise));
                    if (overTime(name, waittime)) {
                      throw new CreateHITException(title);
                    }
                    chill(waittime);
                    waittime *= 2;
                } catch (ObjectAlreadyExistsException e) {
                    LOGGER.info(MessageFormat.format("{0} {1}", name, e));
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }
/*
    public static void removeQualification(Record record) {
        String name = "removeQualification";
        int waittime = 1;
        String qualid = record.qualificationType.getQualificationTypeId();
        LOGGER.info(String.format("Retiring qualification type : (%s)", qualid));
        synchronized (service) {
            while(true) {
                try {
                    service.updateQualificationType(qualid, "retiring", QualificationTypeStatus.Inactive);
                    record.qualificationType.setQualificationTypeStatus(QualificationTypeStatus.Inactive);
                    break;
                } catch (ObjectDoesNotExistException q) {
                    LOGGER.info(String.format("Qualification %s already removed", qualid));
                } catch (InternalServiceException ise) {
                    LOGGER.info(MessageFormat.format("{0} {1}", name, ise));
                    if (overTime(name, waittime)) {
                      LOGGER.warn(String.format("Cannot update qualification %s to inactive. Aborting.", qualid));
                      break;
                    }
                    chill(waittime);
                    waittime *= 2;
                }
            }
            while(true) {
                try {
                    service.disposeQualificationType(qualid);
                    break;
                } catch (InternalServiceException ise) {
                    LOGGER.info(MessageFormat.format("{0} {1}", name, ise));
                    if (overTime(name, waittime)) {
                        LOGGER.warn(String.format("Cannot dispose qualification %s. Aborting.", qualid));
                        break;
                    }
                    chill(waittime);
                    waittime *= 2;
                }
            }
        }
    }
*/
    //***********************************************************//

    public List<Task> listAvailableTasksForRecord(Record r) {
        if (r==null)
            return new ArrayList<Task>();
        List<Task> hits = Arrays.asList(r.getAllTasks());
        ArrayList<Task> retval = new ArrayList<Task>();
        for (Task hit : hits) {
            MturkTask thishit = (MturkTask) getTask(hit.getTaskId());
            if (thishit.hit.getHITStatus().equals(HITStatus.Assignable))
                retval.add(hit);
        }
       return retval;
    }

    /*
    private static SurveyResponse parseResponse(Assignment assignment, Survey survey)
            throws SurveyException, IOException, DocumentException, ParserConfigurationException, SAXException {
        Record record = MturkResponseManager.getRecord(survey);
        return new SurveyResponse(survey, assignment, record);
    }
*/

    public boolean renewIfExpired(String hitId, Survey survey) throws IOException, SurveyException {
        HIT hit = ((MturkTask) getTask(hitId)).hit;
        Record record = getRecord(survey);
        if (hit.getExpiration().before(Calendar.getInstance())) {
            makeTaskAvailable(hitId, record);
            return true;
        } else return false;
    }

    private static List<HIT> getHITsForStatus(HITStatus inputStatus) {
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
     * @return A list of unassignable HITs.
     */
    public static List<HIT> unassignableHITs() {
        return getHITsForStatus(HITStatus.Unassignable);
    }

    /**
     * Deletes all expired HITs {@link HIT}. Also approves any pending assignments.
     * @return  A list of the expired HITs.
     */
    public static List<HIT> deleteExpiredHITs() {
        List<HIT> hits = new ArrayList<HIT>();
        List<String> assignments = new ArrayList<String>();
        List<String> hitids = new ArrayList<String>();
        List<HIT> tasks = getHITsForStatus(HITStatus.Reviewable);
        tasks.addAll(getHITsForStatus(HITStatus.Reviewing));
        for (HIT hit : tasks)
            if (hit.getExpiration().getTimeInMillis() < Calendar.getInstance().getTimeInMillis()){
                hits.add(hit);
                hitids.add(hit.getHITId());
            }
        for (HIT hit : hits) {
            List<Assignment> assignmentsForHIT = getAllAssignmentsForHIT(hit);
            for (Assignment a : assignmentsForHIT)
                if (a.getAssignmentStatus().equals(AssignmentStatus.Submitted))
                    assignments.add(a.getAssignmentId());
        }
        approveAssignments(assignments);
        deleteHITs(hitids);
        LOGGER.info(String.format("Deleted %d HITs", hitids.size()));
        return hits;
    }

    /**
     * Gets a list of all the assignable HITs currently listed on Mechanical Turk. Note that this may
     * include previously assigned HITs from old sessions (that is, the HITs may not be linked to the
     * Surveys {@link Survey} currently held in manager)
     * @return A list of assignable HITs.
     */
    public static List<HIT> assignableHITs() {
        return getHITsForStatus(HITStatus.Assignable);
    }

    public static SurveyResponse parseResponse (Assignment a, Survey survey, Record r)
            throws SurveyException, ParserConfigurationException, SAXException, DocumentException, IOException {
        SimpleDateFormat format = new SimpleDateFormat(SurveyResponse.dateFormat);
        Map<String, String> otherValues = new HashMap<String, String>();
        otherValues.put("acceptTime", String.format("\"%s\"", format.format(a.getAcceptTime().getTime())));
        otherValues.put("submitTime", String.format("\"%s\"", format.format(a.getSubmitTime().getTime())));
        return new SurveyResponse(survey
                , a.getWorkerId()
                , a.getAnswer()
                , r
                , otherValues
        );
    }

    public int addResponses(Survey survey, Task task) throws SurveyException {
        boolean success = false;
        Record r = null;
        try {
            r = ResponseManager.getRecord(survey);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (r == null) return -1;
        // references to things in the record
        List<SurveyResponse> responses = r.responses;
        System.out.println(String.format("%d responses total", responses.size()));
        List<SurveyResponse> botResponses = r.botResponses;
        QC qc = r.qc;
        // local vars
        List<SurveyResponse> validResponsesToAdd = new ArrayList<SurveyResponse>();
        List<SurveyResponse> randomResponsesToAdd = new ArrayList<SurveyResponse>();

        while (!success) {
            try{
                HIT hit = ((MturkTask) task).hit;
                List<Assignment> assignments = getAllAssignmentsForHIT(hit);
                for (Assignment a : assignments) {
                    if (a.getAssignmentStatus().equals(AssignmentStatus.Submitted)) {
                        SurveyResponse sr = parseResponse(a,survey,r);
                        if (QCAction.addAsValidResponse(qc.assess(sr), a, r, sr))
                            validResponsesToAdd.add(sr);
                        else randomResponsesToAdd.add(sr);
                    }
                }
                responses.addAll(validResponsesToAdd);
                botResponses.addAll(randomResponsesToAdd);
                success=true;
            } catch (ServiceException se) {
                LOGGER.warn("ServiceException in addResponses "+se);
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return validResponsesToAdd.size();

    }

    /**
     * Expires all HITs that are not listed as Reviewable or Reviewing. Reviewable assignmenst are those for
     * which a worker has submitted a response. Reviewing is a special state that corresponds to a staging
     * area where jobs wait for review.
     * @return A list of HITs that have been expired.
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
        return expiredHITs;
    }

}

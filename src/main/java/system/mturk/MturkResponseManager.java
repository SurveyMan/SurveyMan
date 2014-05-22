package system.mturk;

import com.amazonaws.mturk.requester.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.InternalServiceException;
import com.amazonaws.mturk.service.exception.ObjectAlreadyExistsException;
import com.amazonaws.mturk.service.exception.ObjectDoesNotExistException;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import interstitial.*;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;
import survey.Survey;
import java.io.IOException;
import java.text.*;
import java.util.*;
import qc.QC;
import survey.exceptions.SurveyException;
import system.SurveyResponse;
import survey.Gensym;

import javax.xml.parsers.ParserConfigurationException;
import static java.text.MessageFormat.*;

public class MturkResponseManager extends AbstractResponseManager {

    protected static class CreateHITException extends SurveyException {
        public CreateHITException(String title) {
            super(String.format("Unable to create HIT for survey \"%s\"", title));
        }
    }

    private static final Logger LOGGER = Logger.getLogger(MturkResponseManager.class);
    protected static PropertiesClientConfig config;
    protected static RequesterService service;
    final protected static long maxAutoApproveDelay = 2592000l;
    final private static Gensym gensym = new Gensym("qual");

    public MturkResponseManager(){
        MturkLibrary lib = new MturkLibrary();
        lib.init();
        config = new PropertiesClientConfig(lib.CONFIG);
        service = new RequesterService(config);
    }

    private static boolean overTime(String name, int waittime){
        if (waittime > MturkResponseManager.maxwaittime){
          LOGGER.warn(String.format("Wait time in %s has exceeded max wait time. Cancelling request.", name));
          return true;
        } else return false;
    }

    public ITask getTask(String taskId){
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
                } catch (ObjectDoesNotExistException odnee) {
                    LOGGER.warn(format("{0} {1}", name, odnee));
                }
            }
        }
    }

    public ITask getTask(String taskId, Record record) {
        ITask retval = getTask(taskId);
        if ( ! Arrays.asList(record.getAllTasks()).contains(retval) )
            record.addNewTask(retval);
        return retval;
    }

    private static Assignment[] getAllAssignmentsForHIT(String hitid, AssignmentStatus[] statuses){
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

    private static HIT[] searchAllHITs () {
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

    @Override
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

    @Override
    public void awardBonus(double amount, ISurveyResponse sr, Survey survey) {
        String name = "awardBonus";
        int waitTime = 1;
        while (true){
            try {
                Record r = getRecord(survey);
                System.out.println("all tasks for this record:" + r.getAllTasks().length);
                if (r.getAllTasks().length==0){
                    System.out.println("No tasks for record " + r.rid);
                    return;
                }
                for (ITask task : r.getAllTasks()) {
                    Assignment[] assignments = service.getAllAssignmentsForHIT(task.getTaskId());
                    System.out.println("all assignments for this record:" + assignments.length);
                    String assignmentId = "";
                    for (Assignment a : assignments) {
                        if (a.getWorkerId().equals(sr.workerId())) {
                            service.grantBonus(sr.workerId(), amount, assignmentId, "For partial work completed.");
                            System.out.println(String.format("Granted worker %s bonus %f for assignment %s in survey %s"
                                    , sr.workerId(), amount, assignmentId, survey.sourceName));
                        }
                    }
                }
            } catch (InternalServiceException ise) {
                LOGGER.warn(format("{0} {1}", name, ise));
                if (overTime(name, waitTime)){

                    return;
                }
                chill(waitTime);
                waitTime = 2 * waitTime;
            } catch (SurveyException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ITask makeTaskForId(Record record, String taskid) {
        HIT task = service.getHIT(taskid);
        return new MturkTask(task, record);
    }

    @Override
    public SurveyResponse parseResponse(String workerId, String ansXML, Survey survey, Record r, Map<String, String> otherValues) throws SurveyException {
        try {
            if (otherValues==null)
                return new SurveyResponse(survey, workerId, ansXML, r, new HashMap<String, String>());
        else return new SurveyResponse(survey, workerId, ansXML, r, otherValues);
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
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

    @Override
    public boolean makeTaskUnavailable(ITask task) {
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

    private static void expireHITs(List<String> hitids) {
        String name = "expireHITs";
        synchronized (MturkSurveyPoster.service) {
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

    protected static String getWebsiteURL() {
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

    protected static String registerNewHitType(Record record) throws CreateHITException {
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

    protected static String createHIT(String title, String description, String keywords, String xml, double reward
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

    @Override
    public List<ITask> listAvailableTasksForRecord(Record r) {
        if (r==null)
            return new ArrayList<ITask>();
        List<ITask> hits = Arrays.asList(r.getAllTasks());
        ArrayList<ITask> retval = new ArrayList<ITask>();
        for (ITask hit : hits) {
            MturkTask thishit = (MturkTask) getTask(hit.getTaskId());
            if (thishit.hit.getHITStatus().equals(HITStatus.Assignable))
                retval.add(hit);
        }
       return retval;
    }

    public boolean renewIfExpired(String hitId, Survey survey) throws SurveyException {
        HIT hit = ((MturkTask) getTask(hitId)).hit;
        Record record;
        try {
            record = getRecord(survey);
        } catch (IOException io) {
            io.printStackTrace();
            return false;
        }
        if (hit.getExpiration().before(Calendar.getInstance())) {
            makeTaskAvailable(hitId, record);
            return true;
        } else return false;
    }

    public int numAvailableAssignments(ITask task) {
        String name = "availableAssignments";
        while (true){
            synchronized (MturkSurveyPoster.service) {
                try{
                    HIT hit = MturkSurveyPoster.service.getHIT(task.getTaskId());
                    int numAvail = hit.getNumberOfAssignmentsAvailable();
                    return numAvail;
                }catch(InternalServiceException ise){
                    LOGGER.warn(MessageFormat.format("{0} {1}", name, ise));
                    chill(1);
                }catch(ObjectDoesNotExistException odne) {
                    LOGGER.warn(MessageFormat.format("{0} {1}", name, odne));
                    return 0;
                }
            }
        }
    }

    public ITask addAssignments(ITask task, int n) {
        String name = "addAssignments";
        while (true){
            synchronized (MturkSurveyPoster.service) {
                try{
                    String id = task.getTaskId();
                    Record r = task.getRecord();
                    service.extendHIT(id, n, 0l);
                    return task;
                }catch(InternalServiceException ise){
                    LOGGER.warn(MessageFormat.format("{0} {1}", name, ise));
                    chill(1);
                }catch(ObjectDoesNotExistException odne) {
                    LOGGER.warn(MessageFormat.format("{0} {1}", name, odne));
                    return null;
                }
            }
        }
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

    public int addResponses(Survey survey, ITask task) throws SurveyException {
        boolean success = false;
        Record r = null;
        try {
            r = AbstractResponseManager.getRecord(survey);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (r == null) return -1;
        // references to things in the record
        List<ISurveyResponse> responses = r.responses;
        List<ISurveyResponse> botResponses = r.botResponses;
        QC qc = r.qc;
        // local vars
        List<SurveyResponse> validResponsesToAdd = new ArrayList<SurveyResponse>();
        List<SurveyResponse> randomResponsesToAdd = new ArrayList<SurveyResponse>();
        SimpleDateFormat format = new SimpleDateFormat(SurveyResponse.dateFormat);

        while (!success) {
            HIT hit = ((MturkTask) task).hit;
            List<Assignment> assignments = getAllAssignmentsForHIT(hit);
            for (Assignment a : assignments) {
                if (a.getAssignmentStatus().equals(AssignmentStatus.Submitted)) {
                    Map<String, String> otherValues = new HashMap<String, String>();
                    otherValues.put("acceptTime", String.format("\"%s\"", format.format(a.getAcceptTime().getTime())));
                    otherValues.put("submitTime", String.format("\"%s\"", format.format(a.getSubmitTime().getTime())));
                    SurveyResponse sr = parseResponse(a.getWorkerId(), a.getAnswer(),survey,r, otherValues);
                    if (QCAction.addAsValidResponse(qc.assess(sr), a, r, sr))
                        validResponsesToAdd.add(sr);
                    else randomResponsesToAdd.add(sr);
                }
            }
            responses.addAll(validResponsesToAdd);
            botResponses.addAll(randomResponsesToAdd);
            success=true;
        }
        if (validResponsesToAdd.size()>0 || botResponses.size() > 0)
            System.out.println(String.format("%d responses total", responses.size()));
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

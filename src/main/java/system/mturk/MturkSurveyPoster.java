package system.mturk;

import com.amazonaws.mturk.requester.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.*;

import java.text.ParseException;
import java.util.*;

import interstitial.*;
import qc.IQCMetrics;
import survey.exceptions.SurveyException;
import org.apache.log4j.Logger;
import system.mturk.generators.MturkXML;

public class MturkSurveyPoster implements ISurveyPoster {

    final private static Logger LOGGER = Logger.getLogger(MturkSurveyPoster.class);
    protected static PropertiesClientConfig config = new PropertiesClientConfig(MturkLibrary.CONFIG);
    protected static RequesterService service = new RequesterService(config);
    private boolean firstPost = true;

    public void init(){
        MturkLibrary lib = new MturkLibrary();
        config = new PropertiesClientConfig(lib.CONFIG);
        service = new RequesterService(config);
    }

    @Override
    public boolean getFirstPost(){
        return firstPost;
    }

    @Override
    public void setFirstPost(boolean post) {
        this.firstPost = post;
    }

    @Override
    public String makeTaskURL(ITask mturkTask) {
        HIT hit = ((MturkTask) mturkTask).hit;
        return MturkResponseManager.getWebsiteURL()+"/mturk/preview?groupId="+hit.getHITTypeId();
    }

    @Override
    public void refresh(Record record) {
        if (!record.library.getClass().equals(MturkLibrary.class)){
            record.library = new MturkLibrary();
        }
        MturkLibrary lib = (MturkLibrary) record.library;
        config.setServiceURL(lib.MTURK_URL);
        service = new RequesterService(config);
    }

    private List<ITask> postNewSurvey(AbstractResponseManager rm, Record record) throws SurveyException {
        MturkResponseManager responseManager = (MturkResponseManager) rm;
        List<ITask> tasks = new ArrayList<ITask>();
        Properties props = record.library.props;
        int numToBatch = Integer.parseInt(record.library.props.getProperty("numparticipants"));
        long lifetime = Long.parseLong(props.getProperty("hitlifetime"));
        //String hitTypeId = MturkResponseManager.registerNewHitType(record);
        //record.hitTypeId = hitTypeId;
        String hitid = null;
        this.refresh(record);
        try {
            hitid = MturkResponseManager.createHIT(
                    props.getProperty("title")
                    , props.getProperty("description")
                    , props.getProperty("keywords")
                    , MturkXML.getXMLString(record.survey)
                    , Double.parseDouble(props.getProperty("reward"))
                    , Long.parseLong(props.getProperty("assignmentduration"))
                    , MturkResponseManager.maxAutoApproveDelay
                    , lifetime
                    , numToBatch
                    , null //hitTypeId
            );
        } catch (ParseException e) {
            e.printStackTrace();
        }
        MturkTask hit = (MturkTask) responseManager.getTask(hitid, record);
        System.out.println(makeTaskURL(hit));
        tasks.add((ITask) hit);
        return tasks;

    }

    private List<ITask> extendThisSurvey(AbstractResponseManager rm, Record record) throws SurveyException {
        List<ITask> tasks = new ArrayList<ITask>();
        int desiredResponses = Integer.parseInt(record.library.props.getProperty("numparticipants"));
        int okay = 0;
        for (ITask task : record.getAllTasks()) {
            ((MturkResponseManager) rm).renewIfExpired(task.getTaskId(), record.survey);
            okay += ((MturkResponseManager) rm).numAvailableAssignments(task);
            okay += record.qc.validResponses.size();
            if(desiredResponses - okay > 0)
                tasks.addAll(((MturkResponseManager) rm).addAssignments(task, desiredResponses - okay));
        }
        return tasks;
    }

    public List<ITask> postSurvey(AbstractResponseManager rm, Record record) throws SurveyException {
        if (firstPost)
            return postNewSurvey(rm, record);
        else {
            IQCMetrics metrics = null;
            try {
                metrics = (IQCMetrics) Class.forName("qc.Metrics").newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            for (ISurveyResponse sr : record.responses) {
                if (metrics.entropyClassification(sr, record.responses))
                    record.qc.botResponses.add(sr);
                else record.qc.validResponses.add(sr);
            }
            return extendThisSurvey(rm, record);
        }
    }

    @Override
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt) {
        synchronized (r) {
            for (ITask task : r.getAllTasks()) {
                responseManager.makeTaskUnavailable(task);
            }
            interrupt.setInterrupt(true);
            return true;
        }
    }

    public boolean postMore(AbstractResponseManager responseManager, Record r) {
        // post more if we have less than two posted at once

        MturkResponseManager mturkResponseManager = (MturkResponseManager) responseManager;

        if (firstPost) {
            firstPost = false;
            return true;
        }
        int availableHITs = mturkResponseManager.listAvailableTasksForRecord(r).size();
        if (availableHITs==0) {
            for (int i = 0 ; i <10 ; i++) {
                if (availableHITs == mturkResponseManager.listAvailableTasksForRecord(r).size())
                    MturkResponseManager.chill(10);
            }
            return availableHITs == 0 && ! r.qc.complete(r.responses, r.library.props);
        } else return false;
    }
}

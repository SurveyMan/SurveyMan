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

    private ITask postNewSurvey(AbstractResponseManager rm, Record record) throws SurveyException {
        MturkResponseManager responseManager = (MturkResponseManager) rm;
        MturkTask task = null;
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
            task = new MturkTask(service.getHIT(hitid), record);
            return task;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ITask extendThisSurvey(AbstractResponseManager rm, Record record) throws SurveyException {
        ITask rettask = null;
        int desiredResponses = Integer.parseInt(record.library.props.getProperty("numparticipants"));
        int okay = 0;
        for (ITask task : record.getAllTasks()) {
            ((MturkResponseManager) rm).renewIfExpired(task.getTaskId(), record.survey);
            okay += ((MturkResponseManager) rm).numAvailableAssignments(task);
            okay += record.qc.validResponses.size();
            if(desiredResponses - okay > 0)
                rettask = (((MturkResponseManager) rm).addAssignments(task, desiredResponses - okay));
        }
        return rettask;
    }

    public ITask postSurvey(AbstractResponseManager rm, Record record) throws SurveyException {
        if (firstPost) {
            firstPost = false;
            return postNewSurvey(rm, record);
        } else {
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
            // classify responses and check if we should extend
            for (ISurveyResponse sr : record.responses) {
                if (metrics.entropyClassification(record.survey, sr, record.responses))
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
            interrupt.setInterrupt(true, "Call to stop survey.", this.getClass().getEnclosingMethod());
            return true;
        }
    }
}

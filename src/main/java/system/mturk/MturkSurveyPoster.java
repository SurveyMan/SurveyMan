package system.mturk;

import com.amazonaws.mturk.requester.*;
import java.text.ParseException;
import java.util.*;
import interstitial.*;
import survey.exceptions.SurveyException;
import org.apache.log4j.Logger;
import system.Parameters;
import system.mturk.generators.MturkXML;

public class MturkSurveyPoster implements ISurveyPoster {

    final private static Logger LOGGER = Logger.getLogger("SurveyMan");
    private boolean firstPost = true;

    public MturkSurveyPoster(){
        super();
    }

    @Override
    public void init(String configURL){
    }

    @Override
    public String makeTaskURL(AbstractResponseManager responseManager, ITask mturkTask) {
        HIT hit = ((MturkTask) mturkTask).hit;
        String url = ((MturkResponseManager) responseManager).getWebsiteURL()+"/mturk/preview?groupId="+hit.getHITTypeId();
        LOGGER.info(url);
        return url;
    }

    private ITask postNewSurvey(MturkResponseManager responseManager, Record record) throws SurveyException {
        Properties props = record.library.props;
        int numToBatch = Integer.parseInt(record.library.props.getProperty(Parameters.NUM_PARTICIPANTS));
        long lifetime = Long.parseLong(props.getProperty(Parameters.HIT_LIFETIME));
        try {
            String hitid = responseManager.createHIT(
                    props.getProperty(Parameters.TITLE)
                    , props.getProperty(Parameters.DESCRIPTION)
                    , props.getProperty(Parameters.KEYWORDS)
                    , MturkXML.getXMLString(record.survey)
                    , Double.parseDouble(props.getProperty(Parameters.REWARD))
                    , Long.parseLong(props.getProperty(Parameters.ASSIGNMENT_DURATION))
                    , MturkResponseManager.maxAutoApproveDelay
                    , lifetime
                    , numToBatch
                    , null //hitTypeId
            );
            MturkTask task = (MturkTask) responseManager.getTask(hitid);
            return new MturkTask(task.hit, record);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ITask extendThisSurvey(AbstractResponseManager rm, Record record) throws SurveyException {
        ITask rettask = null;
        int desiredResponses = Integer.parseInt(record.library.props.getProperty(Parameters.NUM_PARTICIPANTS));
        int okay = 0;
        for (ITask task : record.getAllTasks()) {
            ((MturkResponseManager) rm).renewIfExpired(task.getTaskId(), record.survey);
            okay += ((MturkResponseManager) rm).numAvailableAssignments(task);
            okay += record.validResponses.size();
            if(desiredResponses - okay > 0)
                rettask = (((MturkResponseManager) rm).addAssignments(task, desiredResponses - okay));
        }
        return rettask;
    }

    public ITask postSurvey(AbstractResponseManager rm, Record record) throws SurveyException {
        if (firstPost) {
            firstPost = false;
            return postNewSurvey((MturkResponseManager) rm, record);
        } else return extendThisSurvey(rm, record);
    }

    @Override
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt) {
            for (ITask task : r.getAllTasks()) {
                responseManager.makeTaskUnavailable(task);
            }
            if (!interrupt.getInterrupt())
                interrupt.setInterrupt(true, "Call to stop survey.", new Exception(){}.getStackTrace()[1]);
            return true;
    }
}

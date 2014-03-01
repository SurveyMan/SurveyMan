package system.mturk;

import com.amazonaws.mturk.requester.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.*;
import java.text.ParseException;
import java.util.*;
import survey.Survey;
import survey.SurveyException;
import org.apache.log4j.Logger;
import system.Record;
import system.interfaces.ResponseManager;
import system.interfaces.SurveyPoster;
import system.interfaces.Task;
import system.mturk.generators.XML;

public class MturkSurveyPoster implements SurveyPoster{

    final private static Logger LOGGER = Logger.getLogger(MturkSurveyPoster.class);
    protected static PropertiesClientConfig config = new PropertiesClientConfig(MturkLibrary.CONFIG);
    protected static RequesterService service = new RequesterService(config);
    private boolean firstPost = true;
    public static boolean assigned = false;

    public void init(){
        MturkLibrary lib = new MturkLibrary();
        config = new PropertiesClientConfig(MturkLibrary.CONFIG);
        service = new RequesterService(config);
    }

    public boolean getFirstPost(){
        return firstPost;
    }

    public void setFirstPost(boolean post) {
        this.firstPost = post;
    }

    public static String makeHITURL(MturkTask mturkTask) {
        HIT hit = mturkTask.hit;
        return MturkResponseManager.getWebsiteURL()+"/mturk/preview?groupId="+hit.getHITTypeId();
    }

    public void refresh(Record record) {
        MturkLibrary lib = (MturkLibrary) record.library;
        config.setServiceURL(lib.MTURK_URL);
        service = new RequesterService(config);
    }

    public List<Task> postSurvey(ResponseManager rm, Record record) throws SurveyException {
        MturkResponseManager responseManager = (MturkResponseManager) rm;
        List<Task> tasks = new ArrayList<Task>();
        Properties props = record.library.props;
        int numToBatch = Integer.parseInt(record.library.props.getProperty("numparticipants"));
        long lifetime = Long.parseLong(props.getProperty("hitlifetime"));
        String hitTypeId = MturkResponseManager.registerNewHitType(record);
        record.hitTypeId = hitTypeId;
        String hitid = null;
        try {
            hitid = MturkResponseManager.createHIT(
                    props.getProperty("title")
                    , props.getProperty("description")
                    , props.getProperty("keywords")
                    , XML.getXMLString(record.survey)
                    , Double.parseDouble(props.getProperty("reward"))
                    , Long.parseLong(props.getProperty("assignmentduration"))
                    , MturkResponseManager.maxAutoApproveDelay
                    , lifetime
                    , numToBatch
                    , hitTypeId
            );
        } catch (ParseException e) {
            e.printStackTrace();
        }
        MturkTask hit = (MturkTask) responseManager.getTask(hitid);
        System.out.println(MturkSurveyPoster.makeHITURL(hit));
        synchronized (MturkResponseManager.manager) {
            record.addNewTask((Task) hit);
            MturkResponseManager.manager.notifyAll();
        }
        tasks.add((Task) hit);
        return tasks;
    }

    public boolean postMore(ResponseManager responseManager, Survey survey) {
        // post more if we have less than two posted at once

        MturkResponseManager mturkResponseManager = (MturkResponseManager) responseManager;

        if (firstPost) {
            firstPost = false;
            return true;
        }

        synchronized (MturkResponseManager.manager) {
            MturkResponseManager.chill(5);
            Record r = MturkResponseManager.manager.get(survey.sid);
            if (r==null) return true;
            int availableHITs = mturkResponseManager.listAvailableTasksForRecord(r).size();
            if (availableHITs==0) {
                for (int i = 0 ; i <10 ; i++) {
                    if (availableHITs == mturkResponseManager.listAvailableTasksForRecord(r).size())
                        MturkResponseManager.chill(10);
                }
            }
            return availableHITs == 0 && ! r.qc.complete(r.responses, r.library.props);
        }
    }
}

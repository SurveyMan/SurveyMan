package system.mturk;

import com.amazonaws.mturk.requester.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import survey.Survey;
import survey.SurveyException;
import org.apache.log4j.Logger;
import system.Library;
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
    public String makeTaskURL(Task mturkTask) {
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

    public List<Task> postSurvey(ResponseManager rm, Record record) throws SurveyException {
        MturkResponseManager responseManager = (MturkResponseManager) rm;
        List<Task> tasks = new ArrayList<Task>();
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
                    , XML.getXMLString(record.survey)
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

        MturkResponseManager.chill(5);
        Record r = null;
        try {
            r = MturkResponseManager.getRecord(survey);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SurveyException e) {
            e.printStackTrace();
        }
        if (r==null) return true;
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

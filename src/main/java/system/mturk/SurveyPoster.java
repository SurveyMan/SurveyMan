package system.mturk;

import com.amazonaws.mturk.requester.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.*;
import java.io.*;
import java.text.ParseException;
import java.util.*;
import survey.Survey;
import survey.SurveyException;
import org.apache.log4j.Logger;
import system.Record;
import system.mturk.generators.XML;

public class SurveyPoster {

    final private static Logger LOGGER = Logger.getLogger(SurveyPoster.class);
    protected static PropertiesClientConfig config = new PropertiesClientConfig(MturkLibrary.CONFIG);
    protected static RequesterService service = new RequesterService(config);

    /**
     * Returns the String URL for a particular HIT {@link HIT}. Lists of HITs for a particular survey can be found inside
     * the Survey {@link Survey} instance's Record {@link system.Record}.
     *
     * @param hit
     * @return
     */
    public static String makeHITURL(HIT hit) {
        return ResponseManager.getWebsiteURL()+"/mturk/preview?groupId="+hit.getHITTypeId();
    }

    /**
     * Posts a survey on Mechanical Turk using the current settings in MturkLibrary. Note that randomization is not
     * performed here and so must be called before calling postSurvey. This also allows the user to not use
     * randomization if she wishes.
     *
     * @param record {@link system.Record}
     * @return
     * @throws SurveyException
     * @throws ServiceException
     * @throws IOException
     */

    public static boolean assigned = false;

    public void refresh(Record record) {
        MturkLibrary lib = (MturkLibrary) record.library;
        config.setServiceURL(lib.MTURK_URL);
        service = new RequesterService(config);
    }

    public static List<HIT> postSurvey(Record record)
            throws SurveyException, ServiceException, IOException, ParseException {
        List<HIT> hits = new ArrayList<HIT>();
        Properties props = record.library.props;
        int numToBatch = Integer.parseInt(record.library.props.getProperty("numparticipants"));
        long lifetime = Long.parseLong(props.getProperty("hitlifetime"));
        String hitTypeId = ResponseManager.registerNewHitType(record);
        record.hitTypeId = hitTypeId;
        String hitid = ResponseManager.createHIT(
                    props.getProperty("title")
                    , props.getProperty("description")
                    , props.getProperty("keywords")
                    , XML.getXMLString(record.survey)
                    , Double.parseDouble(props.getProperty("reward"))
                    , Long.parseLong(props.getProperty("assignmentduration"))
                    , ResponseManager.maxAutoApproveDelay
                    , lifetime
                    , numToBatch
                    , hitTypeId
                );
        HIT hit = ResponseManager.getHIT(hitid);
        System.out.println(SurveyPoster.makeHITURL(hit));
        synchronized (ResponseManager.manager) {
            record.addNewHIT(hit);
            ResponseManager.manager.notifyAll();
        }
        hits.add(hit);
        return hits;
    }
    public static boolean firstPost = true;
    /**
     * Checks whether the survey is complete and whether there are fewer than 2 HITs currently posted.
     *
     * @param survey
     * @return
     * @throws IOException
     */
    public static boolean postMore(Survey survey) throws IOException {
            // post more if we have less than two posted at once
        if (firstPost) {
            firstPost = false;
            return true;
        }
        synchronized (ResponseManager.manager) {
            ResponseManager.chill(5);
            Record r = ResponseManager.manager.get(survey.sid);
            if (r==null) return true;
            int availableHITs = ResponseManager.listAvailableHITsForRecord(r).size();
            if (availableHITs==0) {
                for (int i = 0 ; i <10 ; i++) {
                    if (availableHITs == ResponseManager.listAvailableHITsForRecord(r).size())
                        ResponseManager.chill(10);
                }
            }
            return availableHITs == 0 && ! r.qc.complete(r.responses, r.library.props);
        }
    }
}

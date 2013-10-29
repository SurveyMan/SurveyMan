package system.mturk;

import com.amazonaws.mturk.addon.*;
import com.amazonaws.mturk.requester.*;
import com.amazonaws.mturk.requester.Comparator;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.*;
import org.apache.commons.io.output.NullOutputStream;

import java.io.*;

import java.security.MessageDigest;
import java.text.ParseException;
import java.util.*;

import org.hsqldb.lib.MD5;
import survey.Survey;
import survey.SurveyException;
import org.apache.log4j.Logger;
import system.mturk.generators.XML;

public class SurveyPoster {

    final private static Logger LOGGER = Logger.getLogger(SurveyPoster.class);
    private static PropertiesClientConfig config;
    private static int numToBatch = 2;
    protected static RequesterService service;

    /**
     * Contains information Mechanical Turk needs to post the HIT, including most of the contents of
     * ~/surveyman/params.properties. Some fields in ~/surveyman/params.properties are used elsewhere.
     */
    public static HITProperties parameters;

    /**
     * Initializes the MTurkLibrary and sets all properties according to ~/surveyman/params.properties
     */
    public static void init() {
        MturkLibrary.init();
        try {
            config = new PropertiesClientConfig(MturkLibrary.CONFIG);
        } catch (IllegalStateException ise) {
            LOGGER.fatal(ise);
            (new File(MturkLibrary.CONFIG)).delete();
            System.exit(-1);
        }
        updateProperties();
    }

    /**
     * Used to update the parameters when settings in MturkLibrary have changed. It is important to call this method
     * when the user is interacting with the system.
     */
    public static void updateProperties() {
        SurveyPoster.parameters = new HITProperties(MturkLibrary.props);
        MturkLibrary.updateURL();
        SurveyPoster.config.setServiceURL(MturkLibrary.MTURK_URL);
        SurveyPoster.service = new RequesterService(config);
        ResponseManager.service = SurveyPoster.service;
    }

    /**
     * Returns the String URL for a particular HIT {@link HIT}. Lists of HITs for a particular survey can be found inside
     * the Survey {@link Survey} instance's Record {@link Record}.
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
     * @param survey
     * @return
     * @throws SurveyException
     * @throws ServiceException
     * @throws IOException
     */
    public static List<HIT> postSurvey(Survey survey, Map<String, Integer> orderSeen)
            throws SurveyException, ServiceException, IOException, ParseException {
        Record record;
        synchronized (ResponseManager.manager) {
            if (!ResponseManager.manager.containsKey(survey.sid)) {
                record = new Record(survey, (Properties) MturkLibrary.props.clone());
                ResponseManager.manager.put(survey.sid, record);
                ResponseManager.registerNewHitType(record);
            } else {
                record = ResponseManager.manager.get(survey.sid);
            }
        }
        List<HIT> hits = new ArrayList<HIT>();
        for (int i = numToBatch ; i > 0 ; i--) {
            long lifetime = Long.parseLong(MturkLibrary.props.getProperty("hitlifetime"));
            String hitid = ResponseManager.createHIT(
                    MturkLibrary.props.getProperty("title")
                    , MturkLibrary.props.getProperty("description")
                    , MturkLibrary.props.getProperty("keywords")
                    , XML.getXMLString(survey)
                    , Double.parseDouble(MturkLibrary.props.getProperty("reward"))
                    , Long.parseLong(MturkLibrary.props.getProperty("assignmentduration"))
                    , ResponseManager.maxAutoApproveDelay
                    , lifetime
                    , ResponseManager.answerOnce(record)
                    , record.hitTypeId
                );
            HIT hit = ResponseManager.getHIT(hitid);
            System.out.println(SurveyPoster.makeHITURL(hit));
            synchronized (ResponseManager.manager) {

                record.addNewHIT(hit);
                ResponseManager.manager.notifyAll();
            }
            hits.add(hit);
        }

        return hits;

    }

    /**
     * Checks whether the survey is complete and whether there are fewer than 2 HITs currently posted.
     *
     * @param survey
     * @return
     * @throws IOException
     */
    public static boolean postMore(Survey survey) throws IOException {
            // post more if we have less than two posted at once
        synchronized (ResponseManager.manager) {
            Record r = ResponseManager.manager.get(survey.sid);
            //System.out.println("Record: "+r);
            if (r==null) return true;
            int availableHITs = ResponseManager.listAvailableHITsForRecord(r).size();
            System.out.println("available HITs: "+availableHITs);
            return availableHITs < 2 && ! r.qc.complete(r.responses, r.parameters);
        }
    }
}

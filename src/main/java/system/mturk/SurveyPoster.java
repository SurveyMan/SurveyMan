package system.mturk;

import com.amazonaws.mturk.addon.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.*;
import com.amazonaws.mturk.requester.HIT;

import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import qc.QC;
import survey.Survey;
import survey.SurveyException;
import org.apache.log4j.Logger;
import system.mturk.generators.XML;

public class SurveyPoster {

    final private static Logger LOGGER = Logger.getLogger("system.mturk");
    final private static long maxAutoApproveDelay = 2592000;
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
        return service.getWebsiteURL()+"/mturk/preview?groupId="+hit.getHITTypeId();
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
    public static List<HIT> postSurvey(Survey survey) throws SurveyException, ServiceException, IOException {
        List<HIT> hits = new ArrayList<HIT>();
        for (int i = numToBatch ; i > 0 ; i--) {
            boolean notRecorded = true;
            while(notRecorded) {
                HIT hit = service.createHIT(null
                        , MturkLibrary.props.getProperty("title")
                        , MturkLibrary.props.getProperty("description")
                        , MturkLibrary.props.getProperty("keywords")
                        , XML.getXMLString(survey)
                        , Double.parseDouble(MturkLibrary.props.getProperty("reward"))
                        , Long.parseLong(MturkLibrary.props.getProperty("assignmentduration"))
                        , maxAutoApproveDelay
                        , Long.parseLong(MturkLibrary.props.getProperty("hitlifetime"))
                        , 1
                        , ""
                        , null
                        , null
                        );
                if (!ResponseManager.manager.containsKey(survey))
                    ResponseManager.manager.put(survey, new Record(survey, (Properties) MturkLibrary.props.clone()));
                ResponseManager.manager.get(survey).addNewHIT(hit);
                notRecorded = false;
                i--;
                hits.add(hit);
            }
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
        Record r = ResponseManager.getRecord(survey);
        int availableHITs = ResponseManager.listAvailableHITsForRecord(r).size();
        System.out.print("_"+availableHITs);
        return availableHITs < 2 && !QC.complete(r.responses, r.parameters);
    }
}

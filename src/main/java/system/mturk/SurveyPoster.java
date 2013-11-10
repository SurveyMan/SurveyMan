package system.mturk;

import com.amazonaws.mturk.addon.*;
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
import system.mturk.generators.XML;

public class SurveyPoster {

    final private static Logger LOGGER = Logger.getLogger(SurveyPoster.class);
    private static PropertiesClientConfig config = new PropertiesClientConfig(MturkLibrary.CONFIG);
    private static int numToBatch = 2;
    protected static RequesterService service = new RequesterService(config);

    /**
     * Contains information Mechanical Turk needs to post the HIT, including most of the contents of
     * ~/surveyman/params.properties. Some fields in ~/surveyman/params.properties are used elsewhere.
     */
    public static HITProperties parameters;

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
     * @param record {@link Record}
     * @return
     * @throws SurveyException
     * @throws ServiceException
     * @throws IOException
     */
    public static List<HIT> postSurvey(Record record)
            throws SurveyException, ServiceException, IOException, ParseException {
        List<HIT> hits = new ArrayList<HIT>();
        Properties props = record.library.props;
        for (int i = numToBatch ; i > 0 ; i--) {
            long lifetime = Long.parseLong(props.getProperty("hitlifetime"));
            assert(record.hitTypeId!=null);
            assert(record.qualificationType!=null);
            ResponseManager.freshenQualification(record);
            assert(record.qualificationType.getQualificationTypeStatus().equals(QualificationTypeStatus.Active));
            String hitid = ResponseManager.createHIT(
                    props.getProperty("title")
                    , props.getProperty("description")
                    , props.getProperty("keywords")
                    , XML.getXMLString(record.survey)
                    , Double.parseDouble(props.getProperty("reward"))
                    , Long.parseLong(props.getProperty("assignmentduration"))
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
            return availableHITs < 2 && ! r.qc.complete(r.responses, r.library.props);
        }
    }
}

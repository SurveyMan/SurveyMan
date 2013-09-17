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

    public static HITProperties parameters;
    public static String hitURL = "";
    //public static QualificationType alreadySeen = service.createQualificationType("survey", "survey", QC.QUAL);

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


    public static void updateProperties() {
        try {
            SurveyPoster.parameters = new HITProperties(MturkLibrary.PARAMS);
        } catch (IOException ex) {
            LOGGER.fatal(ex.getMessage());
            System.exit(-1);
        }
        SurveyPoster.config.setServiceURL(MturkLibrary.MTURK_URL);
        SurveyPoster.service = new RequesterService(config);
        ResponseManager.service = SurveyPoster.service;
        System.out.println("A"+service);
    }

    private static String makeHITURL(String hitTypeID) {
        return service.getWebsiteURL()+"/mturk/preview?groupId="+hitTypeID;
    }

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
                String hitid = hit.getHITId();
                String hittypeid = hit.getHITTypeId();
                hitURL = makeHITURL(hittypeid);
                LOGGER.info("Created HIT:"+hitid);
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

    public static boolean postMore(Survey survey) throws IOException {
            // post more if we have less than two posted at once
        Record r = ResponseManager.getRecord(survey);
        int availableHITs = ResponseManager.listAvailableHITsForRecord(r).size();
        System.out.print("_"+availableHITs);
        return availableHITs < 2;
    }
}

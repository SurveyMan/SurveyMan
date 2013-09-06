package system.mturk;

import com.amazonaws.mturk.addon.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.AccessKeyException;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.*;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.service.exception.InternalServiceException;
import java.io.*;
import csv.CSVParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import gui.display.Experiment;
import survey.Survey;
import survey.SurveyException;
import org.apache.log4j.Logger;

public class SurveyPoster {

    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    private static PropertiesClientConfig config;
    protected static RequesterService service;
    public static HITProperties parameters;
    static {
        try {
            config = new PropertiesClientConfig(MturkLibrary.CONFIG);
        } catch (IllegalStateException ise) {
            LOGGER.fatal(ise);
            (new File(MturkLibrary.CONFIG)).delete();
            System.exit(-1);
        }
        updateProperties();
    }
    private static int numToBatch = 2;
    public static String hitURL = "";
    //public static QualificationType alreadySeen = service.createQualificationType("survey", "survey", QC.QUAL);

    public static void updateProperties() {
        try {
            parameters = new HITProperties(MturkLibrary.PARAMS);
        } catch (IOException ex) {
            LOGGER.fatal(ex.getMessage());
            System.exit(-1);
        }
        config.setServiceURL(MturkLibrary.MTURK_URL);
        service = new RequesterService(config);
    }

    private static String makeHITURL(String hitTypeID) {
        return service.getWebsiteURL()+"/mturk/preview?groupId="+hitTypeID;
    }

    public static HIT postSurvey(Survey survey) throws SurveyException, ServiceException, IOException {
        LOGGER.info(MturkLibrary.props);
        boolean notRecorded = true;
        HIT hit = null;
        while (notRecorded) {
            hit = service.createHIT(null
                    , MturkLibrary.props.getProperty("title")
                    , MturkLibrary.props.getProperty("description")
                    , MturkLibrary.props.getProperty("keywords")
                    , XMLGenerator.getXMLString(survey)
                    , Double.parseDouble(MturkLibrary.props.getProperty("reward"))
                    , Long.parseLong(MturkLibrary.props.getProperty("assignmentduration"))
                    , Long.parseLong(MturkLibrary.props.getProperty("autoapprovaldelay"))
                    , Long.parseLong(MturkLibrary.props.getProperty("hitlifetime"))
                    , numToBatch
                    , ""
                    , null
                    , null
                    );
            String hitid = hit.getHITId();
            String hittypeid = hit.getHITTypeId();
            hitURL = makeHITURL(hittypeid);
            LOGGER.info("Created HIT:"+hitid);
            if (!ResponseManager.manager.containsKey(survey))
                ResponseManager.manager.put(survey, new ResponseManager.Record(survey, (Properties) MturkLibrary.props.clone()));
            ResponseManager.manager.get(survey).addNewHIT(hit);
            recordHit(hitid, hittypeid);
            notRecorded = false;
        }
        return hit;
    }     

    private static void recordHit(String hitid, String hittypeid) {
        try { 
            PrintWriter out = new PrintWriter(new FileWriter(ResponseManager.SUCCESS, true));
            out.println(hitid+","+hittypeid);
            out.close();
        } catch (IOException io) {
            LOGGER.warn(io);
        }
    }
}

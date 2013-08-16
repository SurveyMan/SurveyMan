package system.mturk;

import com.amazonaws.mturk.addon.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.*;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.service.exception.InternalServiceException;
import java.io.*;
import csv.CSVParser;
import java.util.Arrays;
import survey.Survey;
import survey.SurveyException;
import org.apache.log4j.Logger;

public class SurveyPoster {

    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    private static final String fileSep = System.getProperty("file.separator");
    private static PropertiesClientConfig config = new PropertiesClientConfig(MturkLibrary.CONFIG);
    protected static RequesterService service;
    public static HITProperties parameters;
    static {
        updateProperties();
    }
    private static int numToBatch = 1;
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
    
    public static boolean hasEnoughFund() {
        double balance = service.getAccountBalance();
        LOGGER.info("Got account balance: " + RequesterService.formatCurrency(balance));
        return balance > 0;
    }
    
    public static void expireOldHITs() {
        boolean expired = false;
        //while (! expired) {
        //    try {
                for (HIT hit : service.searchAllHITs()){
                    HITStatus status = hit.getHITStatus();
                    if (! (status.equals(HITStatus.Reviewable) || status.equals(HITStatus.Reviewing))) {
                        service.disableHIT(hit.getHITId());
                        LOGGER.info("Expired HIT:"+hit.getHITId());
                    }
                }
                //expired = true;
        //    } catch (Exception e) {
        //        LOGGER.warning(e.getMessage());
        //    }
        //}
        //LOGGER.info("Total HITs available before execution: " + service.getTotalNumHITsInAccount());
    }

    public static void deleteExpiredHITs(){
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            if (status.equals(HITStatus.Reviewable)) {
                service.disposeHIT(hit.getHITId());
                LOGGER.info("Disposed HIT:"+hit.getHITId());
            }
        }
    }

    private static String makeHITURL(String hitTypeID) {
        return service.getWebsiteURL()+"/mturk/preview?groupId="+hitTypeID;
    }

    public static HIT postSurvey(Survey survey) throws SurveyException, ServiceException {
        LOGGER.info(MturkLibrary.props);
        boolean notRecorded = true;
        HIT hit = null;
        while (notRecorded) {
            try {                
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
                recordHit(hitid, hittypeid);
                notRecorded = false;
            } catch (InternalServiceException e) {
                LOGGER.warn(e);
            }
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

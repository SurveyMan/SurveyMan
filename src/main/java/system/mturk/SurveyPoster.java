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
    
    public static List<HIT> expireOldHITs() {
        List<HIT> expiredHITs = new ArrayList<HIT>();
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            if (! (status.equals(HITStatus.Reviewable) || status.equals(HITStatus.Reviewing))) {
                service.disableHIT(hit.getHITId());
                expiredHITs.add(hit);
                LOGGER.info("Expired HIT:"+hit.getHITId());
            }
        }
        return expiredHITs;
    }

    public static List<HIT> deleteExpiredHITs(){
        List<HIT> deletedHITs = new ArrayList<HIT>();
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            if (status.equals(HITStatus.Reviewable)) {
                service.disposeHIT(hit.getHITId());
                deletedHITs.add(hit);
                LOGGER.info("Disposed HIT:"+hit.getHITId());
            }
        }
        return deletedHITs;
    }

    public static List<HIT> assignableHITs() {
        List<HIT> hits = new ArrayList<HIT>();
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            if (status.equals(HITStatus.Assignable)) {
                service.disposeHIT(hit.getHITId());
                hits.add(hit);
                LOGGER.info("Disposed HIT:"+hit.getHITId());
            }
        }
        return hits;
    }

    public static List<HIT> unassignableHITs() {
        List<HIT> hits = new ArrayList<HIT>();
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            if (status.equals(HITStatus.Unassignable)) {
                service.disposeHIT(hit.getHITId());
                hits.add(hit);
                LOGGER.info("Disposed HIT:"+hit.getHITId());
            }
        }
        return hits;
    }

    private static String makeHITURL(String hitTypeID) {
        return service.getWebsiteURL()+"/mturk/preview?groupId="+hitTypeID;
    }

    public static HIT postSurvey(Survey survey) throws SurveyException, ServiceException {
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
            Experiment.updateStatusLabel("Created HIT "+hitid+". To view, press 'View HIT'.");
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

package system.mturk;

import com.amazonaws.mturk.addon.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.*;
import com.amazonaws.mturk.requester.HIT;
import java.io.*;
import csv.CSVParser;
import java.util.logging.Level;
import java.util.logging.Logger;
import survey.Survey;

public class SurveyPoster {

    private static final String config = ".config";
    private static final RequesterService service = new RequesterService(new PropertiesClientConfig(config));
    public static final String outDir = "data/output";
    public static HITProperties parameters;
    static {
        try {
            parameters = new HITProperties("props");
        } catch (IOException ex) {
            Logger.getLogger(SurveyPoster.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
    
    public static boolean hasEnoughFund() {
        double balance = service.getAccountBalance();
        System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
        return balance > 0;
    }
    
    public static void postSurvey(Survey survey) throws FileNotFoundException, Exception {
        System.out.println(XMLGenerator.getXMLString(survey));
        HITQuestion question = new HITQuestion(XMLGenerator.getXMLString(survey));
        HIT[] hit = service.createHITs(null //surveys have no input
                , parameters
                , question
                , new HITDataCSVWriter(outDir + "/survey" + survey.sid, ',' , true, true)
                , new HITDataCSVWriter(outDir + "/survey" + survey.sid, ',' , true, true)
                );
        for (int i = 0 ; i < hit.length ; i++) {
            String hitid = hit[i].getHITId();
            String hittypeid = hit[i].getHITTypeId();
            System.out.println("Created HIT: " + hitid);
            System.out.println("You may see your HIT with HITTypeId '" + hittypeid + "' here: ");
            System.out.println(service.getWebsiteURL() + "/mturk/preview?groupId=" + hittypeid);
            recordHit(hitid, hittypeid);
        }
    }

    private static void recordHit(String hitid, String hittypeid) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(new File(outDir + "/survey.success"), true));
        out.println(hitid+","+hittypeid);
    }
    
    public static void main(String[] args) throws Exception {
        Survey survey = CSVParser.parse("/Users/jnewman/dev/new-SurveyMan/SurveyMan/data/linguistics/test2.csv", "\t");
        service.previewHIT(null,parameters, new HITQuestion(XMLGenerator.getXMLString(survey)));
        postSurvey(survey);
    }
}

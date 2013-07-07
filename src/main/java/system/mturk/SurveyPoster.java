package system.mturk;

import com.amazonaws.mturk.addon.*;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.*;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.HITStatus;
import com.amazonaws.mturk.requester.QualificationType;
import java.io.*;
import csv.CSVParser;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import qc.QC;
import survey.Survey;
import system.Library;

public class SurveyPoster {

    private static final String fileSep = System.getProperty("file.separator");
    protected static final RequesterService service = new RequesterService(new PropertiesClientConfig(Library.CONFIG));
    //public static QualificationRequirement[] qr = { new QualificationRequirement(QC.QUAL, Comparator.NotEqualTo, 1, null, true) };
    public static HITProperties parameters;
    public static QualificationType alreadySeen = service.createQualificationType("survey", "survey", QC.QUAL);
    static {
        try {
            parameters = new HITProperties(Library.PARAMS);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(-1);
        }
    }
    
    public static boolean hasEnoughFund() {
        double balance = service.getAccountBalance();
        System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
        return balance > 0;
    }
    
    public static void expireOldHITs() {
        for (HIT hit : service.searchAllHITs()){
            HITStatus status = hit.getHITStatus();
            if (! (status.equals(HITStatus.Reviewable) || status.equals(HITStatus.Reviewing)))
                service.disableHIT(hit.getHITId());
        }
        System.out.println("Total HITs available before execution: " + service.getTotalNumHITsInAccount());
    }
    
    public static String postSurvey(Survey survey) throws IOException {
        HIT hit = service.createHIT(parameters.getTitle()
                , parameters.getDescription()
                , parameters.getRewardAmount()
                , XMLGenerator.getXMLString(survey)
                , parameters.getMaxAssignments()
                , true
                );
        String hitid = hit.getHITId();
        String hittypeid = hit.getHITTypeId();
        System.out.println("Created HIT: " + hitid);
        System.out.println("You may see your HIT with HITTypeId '" + hittypeid + "' here: ");
        System.out.println(service.getWebsiteURL() + "/mturk/preview?groupId=" + hittypeid);
        recordHit(hitid, hittypeid);
        return hitid;
    }

    private static void recordHit(String hitid, String hittypeid) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(ResponseManager.SUCCESS, true));
        out.println(hitid+","+hittypeid);
        out.close();
    }
    
    public static void main(String[] args) throws Exception {
        expireOldHITs();
        Survey survey3 = CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", fileSep), ":");
        Survey survey2 = CSVParser.parse(String.format("data%1$slinguistics%1$stest2.csv", fileSep), "\\t");
        //Survey survey1 = CSVParser.parse(String.format("data%1$slinguistics%1$stest1.csv", fileSep), ",");
        Survey[] surveys = {survey2,survey3};
        for (Survey survey : Arrays.asList(surveys)) {
            HITQuestion hitq = new HITQuestion();
            hitq.setQuestion(XMLGenerator.getXMLString(survey));
            //service.previewHIT(null,parameters,hitq);
            postSurvey(survey);
        }
        if (service.getTotalNumHITsInAccount() != 0)
            Logger.getAnonymousLogger().log(Level.WARNING, "Total registered HITs is " + service.getTotalNumHITsInAccount());
    }
}

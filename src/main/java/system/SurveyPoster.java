/**
 * Created by IntelliJ IDEA.
 * User: jnewman
 * Date: 6/14/13
 * Time: 11:39 AM
 * To change this template use File | Settings | File Templates.
 */



import com.amazonaws.mturk.addon.*;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import com.amazonaws.mturk.requester.HIT;

import java.io.*;

import java.io.PrintWriter;
import java.util.*;

public class SurveyPoster {
    private RequesterService service;
    private String mturkPropertiesPath = "./java/mturk.properties";

    //Defining the attributes of the HIT. These things should be provided by the user somehow...
    private String title = "Take our experimental survey.";
    private String description = "How likely is this as a word of English?";
    private int numAssignments = 1;
    private double reward = 0.05;
    private String keywords = "survey";
    private long assignmentDurationInSeconds = 60 * 60; // 1 hour
    private long autoApprovalDelayInSeconds = 60 * 60 * 24 * 15; // 15 days
    private long lifetimeInSeconds = 60 * 60 * 24 * 3; // 3 days
    private String requesterAnnotation = "";
    QualificationRequirement[] qualReqs = null;
    
    public SurveyPoster() {
        service = new RequesterService(new PropertiesClientConfig(mturkPropertiesPath));
    }

    public boolean hasEnoughFund() {
        double balance = service.getAccountBalance();
        System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
        return balance > 0;
    }
    
    public void postSurvey(int numSurveys, String surveyDir, Map<Integer, String[]> qs, Map<Integer, Map> opts) {
        String questionFile = "";
        String successFilePath = "./java/system/surveys.success";
        String outputFilePath = "./java/system/surveys.results";
        try
        {
            PrintWriter out = new PrintWriter(new FileWriter(new File(successFilePath)));
            out.println("hitid\thittypeid");
            for (int i = 0; i < numSurveys; i++) {
                if (i < 10) {
                    questionFile = surveyDir + "survey0"+(i+1)+".question";
                }
                else {
                    questionFile = surveyDir + "survey"+(i+1)+".question";
                }
                HITQuestion question = new HITQuestion(questionFile);
                HIT hit = service.createHIT(null, // HITTypeId
                        title,
                        description, keywords,
                        question.getQuestion(),
                        reward, assignmentDurationInSeconds,
                        autoApprovalDelayInSeconds, lifetimeInSeconds,
                        numAssignments, requesterAnnotation,
                        qualReqs,
                        null // responseGroup
                );
                out.println(hit.getHITId()+"\t"+hit.getHITTypeId());
                System.out.println("Created HIT: " + hit.getHITId());

                System.out.println("You may see your HIT with HITTypeId '"
                        + hit.getHITTypeId() + "' here: ");

                System.out.println(service.getWebsiteURL()
                        + "/mturk/preview?groupId=" + hit.getHITTypeId());
            }
            out.flush();
            out.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        waitForResults(successFilePath, outputFilePath, qs, opts);
    }
    
    public void waitForResults(String successFilePath, String outputFilePath, Map<Integer, String[]> qs, Map<Integer, Map> opts) {
        boolean resultsNotIn = true;
        try
        {
            while (resultsNotIn)
            {
                Thread.sleep(15000);
                getResults(successFilePath, outputFilePath);
                if (surveyIsComplete(outputFilePath, qs, opts)) {
                    resultsNotIn = false;
                    System.out.println("Results written and recorded. Exiting.");
                }
                else
                {
                    System.out.println("Results not yet written.");
                }
            }
            //runScript("./reviewResults.sh", "~/dev/aws-mturk-clt-1.3.1/samples/external_hit/");
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void getResults(String successFilePath, String outputFilePath) {
        try
        {
            HITDataInput success = new HITDataCSVReader(successFilePath);
            HITTypeResults results = service.getHITTypeResults(success);
            File resultsFile = new File(outputFilePath);
            resultsFile.delete();
            results.setHITDataOutput(new HITDataCSVWriter(outputFilePath));
            results.writeResults();
        }
        catch (IOException e)
        {
            System.out.println("Error handling success file");
            e.printStackTrace();
        }
    }

    public boolean surveyIsComplete(String outputFilePath, Map<Integer, String[]> qs, Map<Integer, Map> opts) {
        return parseResults(outputFilePath, qs, opts);
    }

    public boolean parseResults(String resultsFile, Map<Integer, String[]> qs, Map<Integer, Map> opts) {
        boolean complete = true;
        try
        {
            Scanner scan = new Scanner(new File(resultsFile));
            String[] headers = scan.nextLine().split("\t");
            int assignCol = 0, completeCol = 0, pendCol = 0, answerCol = 0;
            Map<Integer, String> results = new HashMap<Integer, String>();
            for (int i = 0; i < headers.length; i++)
            {
                if (headers[i].equals("\"numavailable\""))
                {
                    assignCol = i;
                }
                if (headers[i].equals("\"numpending\""))
                {
                    pendCol = i;
                }
                if (headers[i].equals("\"numcomplete\""))
                {
                    completeCol = i;
                }
                if (headers[i].equals("\"answers[question_id answer_value]\""))
                {
                    answerCol = i;
                }
            }
            int numAvailable = 0;
            int numComplete = 0;
            int numPending = 0;
            Map<Integer, String> optionMap;
            int oid = 0;
            Map<Integer,Double> ratings = new HashMap<Integer,Double>();
            while(scan.hasNextLine())
            {
                String line = scan.nextLine();
                String [] hitArray = line.split("\t");
                if (hitArray.length == 0) {
                    break;
                }
                try
                {
                    numAvailable = Integer.parseInt(hitArray[assignCol].replace("\"",""));
                    numPending = Integer.parseInt(hitArray[pendCol].replace("\"",""));
                    numComplete = Integer.parseInt(hitArray[completeCol].replace("\"",""));
                    if (numAvailable > 0 || numPending > 0)
                    {
                        complete = false;
                    }
                }
                catch (java.lang.NumberFormatException e)
                {
                    e.printStackTrace();
                }
                ArrayList<String> answers = new ArrayList<String>();
                String res = "";
                int currentCol = answerCol;
                if (!hitArray[answerCol].equals("\"none\""))
                {
                    for (int x = 0; x < qs.keySet().size()*2+2; x++)
                    {
                        res = hitArray[currentCol].replace("\"","");
                        if (!res.equalsIgnoreCase("submit"))
                        {
                            answers.add(res);
                        }
                        currentCol++;
                    }
                }
                int qid = 0;
                int counter = 0;
                String questionString = "";
                for (String ans : answers) {
                    if (counter%2 == 0) {
                        qid = Integer.parseInt(ans.replace("q",""));
                    }
                    else {
                        String[] q = qs.get(qid);
                        if (!results.containsKey(qid))
                        {
                            if (!q[2].equals(""))
                            {
                                results.put(qid,q[1]+","+q[2].substring(q[2].lastIndexOf("/")+1)+",");
                            }
                            else
                            {
                                results.put(qid,q[1]+",");
                            }
                        }
                        oid = Integer.parseInt(ans.substring(ans.lastIndexOf("n")+1).replace("\"",""));
                        String questionLine = results.get(qid);
                        questionLine+=(opts.get(qid).get(oid)+",");
                        results.put(qid,questionLine);
                        if (!ratings.containsKey(qid))
                        {
                            ratings.put(qid,(double)oid);
                        }
                        else
                        {
                            ratings.put(qid,((double)oid+ratings.get(qid))/2.0);
                        }
                    }
                    counter++;
                }
            }
            for (int qid : results.keySet())
            {
                results.put(qid,results.get(qid)+ratings.get(qid));
            }
            System.out.println(results);
            PrintWriter out = new PrintWriter(new FileWriter("./java/system/results.txt"));
            out.println("Question,Options chosen,Rating");
            for (int qid : results.keySet())
            {
                out.println(results.get(qid));
            }
            out.flush();
            out.close();
        }
        catch (java.io.IOException e)
        {
            System.out.println("Could not parse results file");
            System.exit(0);
        }
        return complete;
    }
}

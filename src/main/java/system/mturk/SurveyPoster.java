package system.mturk;

import com.amazonaws.mturk.addon.*;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import com.amazonaws.mturk.requester.HIT;
import java.io.*;
import java.util.*;
import scalautils.ParamParse;
import survey.Survey;

public class SurveyPoster {

    private static final String config = ".config";
    private static final RequesterService service = new RequesterService(new PropertiesClientConfig(config));
    public static final Map<String, String> parameters = ParamParse.getMap();
    public static final String successFilePath = "data/output/surveys.success";
    public static final String outputFilePath = "data/output/surveys.results";


    public static boolean hasEnoughFund() {
        double balance = service.getAccountBalance();
        System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
        return balance > 0;
    }
    
    public static String postSurvey(Survey survey) throws FileNotFoundException, Exception {
        HITQuestion question = new HITQuestion(XMLGenerator.getXMLString(survey));
        HIT hit = service.createHIT(survey.sid // HITTypeId - does this have any semantics to them?
                , parameters.get("title")
                , parameters.get("description")
                , parameters.get("keywords")
                , question.getQuestion()
                , Double.parseDouble(parameters.get("reward"))
                , Long.parseLong(parameters.get("assignmentDurationInSeconds"))
                , Long.parseLong(parameters.get("autoApprovalDelayInSeconds"))
                , Long.parseLong(parameters.get("lifetimeInSeconds"))
                , Integer.parseInt(parameters.get("numAssignments"))
                , ""
                , null //qualification requirements
                , null // responseGroup
                );
        String hitid = hit.getHITId();
        String hittypeid = hit.getHITTypeId();
        System.out.println("Created HIT: " + hitid);
        System.out.println("You may see your HIT with HITTypeId '" + hittypeid + "' here: ");
        System.out.println(service.getWebsiteURL() + "/mturk/preview?groupId=" + hittypeid);
        return hitid + "," + hittypeid;
    }

    private static void recordHit(String hitid, String hittypeid) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(new File(successFilePath), true));
        out.println(hitid+","+hittypeid);
    }

    public static void waitForResults(Survey survey) throws IOException {
        boolean resultsNotIn = true;
        while (resultsNotIn) {
            try {
                Thread.sleep(2*60000);
                getResults();
                if (surveyIsComplete(survey)) {
                    resultsNotIn = false;
                    System.out.println("Results written and recorded. Exiting.");
                } System.out.print(".");
            } catch (InterruptedException e) {
                System.out.print("!");
            }
        }
    }

    public static void getResults() throws IOException {
        HITDataInput success = new HITDataCSVReader(successFilePath);
        HITTypeResults results = service.getHITTypeResults(success);
        File resultsFile = new File(outputFilePath);
        resultsFile.delete();
        results.setHITDataOutput(new HITDataCSVWriter(outputFilePath));
        results.writeResults();
    }

    public static boolean surveyIsComplete(Survey survey) {
        return parseResults(outputFilePath, survey);
    }

    public static boolean parseResults(String resultsFile, Survey survey) {
//        boolean complete = true;
//        try
//        {
//            Scanner scan = new Scanner(new File(resultsFile));
//            String[] headers = scan.nextLine().split("\t");
//            int assignCol = 0, completeCol = 0, pendCol = 0, answerCol = 0;
//            Map<Integer, String> results = new HashMap<Integer, String>();
//            for (int i = 0; i < headers.length; i++)
//            {
//                if (headers[i].equals("\"numavailable\""))
//                {
//                    assignCol = i;
//                }
//                if (headers[i].equals("\"numpending\""))
//                {
//                    pendCol = i;
//                }
//                if (headers[i].equals("\"numcomplete\""))
//                {
//                    completeCol = i;
//                }
//                if (headers[i].equals("\"answers[question_id answer_value]\""))
//                {
//                    answerCol = i;
//                }
//            }
//            int numAvailable = 0;
//            int numComplete = 0;
//            int numPending = 0;
//            Map<Integer, String> optionMap;
//            Map<Integer,Double> ratings = new HashMap<Integer,Double>();
//            while(scan.hasNextLine())
//            {
//                String line = scan.nextLine();
//                String [] hitArray = line.split("\t");
//                if (hitArray.length == 0) {
//                    break;
//                }
//                try
//                {
//                    numAvailable = Integer.parseInt(hitArray[assignCol].replace("\"",""));
//                    numPending = Integer.parseInt(hitArray[pendCol].replace("\"",""));
//                    numComplete = Integer.parseInt(hitArray[completeCol].replace("\"",""));
//                    if (numAvailable > 0 || numPending > 0)
//                    {
//                        complete = false;
//                    }
//                }
//                catch (java.lang.NumberFormatException e)
//                {
//                    e.printStackTrace();
//                }
//                ArrayList<String> answers = new ArrayList<String>();
//                String res = "";
//                int currentCol = answerCol;
//                if (!hitArray[answerCol].equals("\"none\""))
//                {
//                    for (int x = 0; x < survey.questions.size()*2+2; x++)
//                    {
//                        System.out.println(hitArray[currentCol]);
//                        res = hitArray[currentCol].replace("\"","");
//                        if (!res.equalsIgnoreCase("submit"))
//                        {
//                            answers.add(res);
//                        }
//                        currentCol++;
//                    }
//                }
//                int qid = 0;
//                int counter = 0;
//                String questionString = "";
//                for (String ans : answers) {
//                    if (counter%2 == 0) {
//                        qid = Integer.parseInt(ans.replace("q",""));
//                    }
//                    // don't know what any of this is.
////                    else {
////                        String[] q = qs.get(qid);
////                        if (!results.containsKey(qid))
////                        {
////                            if (!q[2].equals(""))
////                            {
////                                results.put(qid,q[1]+","+q[2].substring(q[2].lastIndexOf("/")+1)+",");
////                            }
////                            else
////                            {
////                                results.put(qid,q[1]+",");
////                            }
////                        }
//                        int oid = 0;
//                        String[] optionStrings = ans.split("\\|");
//                        String questionLine = results.get(qid);
//                        double rating = 0.0;
//                        for (int k=0; k<optionStrings.length; k++)
//                        {
//                            oid = Integer.parseInt(optionStrings[k].substring(optionStrings[k].lastIndexOf("n")+1).replace("\"",""));
//                            //questionLine+=(opts.get(qid).get(oid)+",");
//                            rating = ((double)oid+rating)/2;
//                        }
//                        results.put(qid,questionLine);
//                        if (!ratings.containsKey(qid))
//                        {
//                            ratings.put(qid,rating);
//                        }
//                        else
//                        {
//                            ratings.put(qid,(rating+ratings.get(qid))/2.0);
//                        }
//                    }
//                    counter++;
//                }
//            }
//            for (int qid : results.keySet())
//            {
//                results.put(qid,results.get(qid)+ratings.get(qid));
//            }
//            System.out.println(results);
//            PrintWriter out = new PrintWriter(new FileWriter("./output/results.txt"));
//            out.println("Question,Options chosen,Rating");
//            for (int qid : results.keySet())
//            {
//                out.println(results.get(qid));
//            }
//            out.flush();
//            out.close();
//        }
//        catch (java.io.IOException e)
//        {
//            System.out.println("Could not parse results file");
//            e.printStackTrace();
//            System.exit(0);
//        }
//        return complete;
    return true;
    }
}

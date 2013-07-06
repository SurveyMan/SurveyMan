package system.mturk;

import com.amazonaws.mturk.addon.HITDataCSVReader;
import com.amazonaws.mturk.addon.HITDataCSVWriter;
import com.amazonaws.mturk.addon.HITDataInput;
import com.amazonaws.mturk.addon.HITTypeResults;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import survey.Survey;
import system.Library;

import java.io.File;
import java.io.IOException;


public class ResponseParser {

    public static final String RESULTS = Library.OUTDIR + Library.fileSep + "results.csv";
    public static final String SUCCESS = Library.OUTDIR + Library.fileSep + "success.csv";
    public static boolean complete = false;
    private static final RequesterService service = new RequesterService(new PropertiesClientConfig(Library.CONFIG));

    public static void getResults() throws IOException {
        HITDataInput success = new HITDataCSVReader(SUCCESS, ',');
        HITTypeResults results = service.getHITTypeResults(success);
        results.setHITDataOutput(new HITDataCSVWriter(RESULTS));
        results.writeResults();
    }

//    public static boolean parseResults(String resultsFile, Survey survey) {
//        try{
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
//    return true;
//    }
    
    public static void main(String[] args) {
        
    }

}

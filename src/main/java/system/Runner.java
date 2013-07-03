package system;

import com.amazonaws.mturk.addon.HITDataCSVReader;
import com.amazonaws.mturk.addon.HITDataCSVWriter;
import com.amazonaws.mturk.addon.HITDataInput;
import com.amazonaws.mturk.addon.HITTypeResults;
import java.io.File;
import java.io.IOException;
import survey.Survey;

/**
 *
 * 
 */
public class Runner {

//    public static final String outputFilePath = "data/output/surveys.results";
//
//    
//    
//        public static void waitForResults(Survey survey) throws IOException {
//        boolean resultsNotIn = true;
//        while (resultsNotIn) {
//            try {
//                Thread.sleep(2*60000);
//                getResults();
//                if (surveyIsComplete(survey)) {
//                    resultsNotIn = false;
//                    System.out.println("Results written and recorded. Exiting.");
//                } System.out.print(".");
//            } catch (InterruptedException e) {
//                System.out.print("!");
//            }
//        }
//    }
//        
//            public static void getResults() throws IOException {
//        HITDataInput success = new HITDataCSVReader(successFilePath);
//        HITTypeResults results = service.getHITTypeResults(success);
//        File resultsFile = new File(outputFilePath);
//        resultsFile.delete();
//        results.setHITDataOutput(new HITDataCSVWriter(outputFilePath));
//        results.writeResults();
//    }
    
}

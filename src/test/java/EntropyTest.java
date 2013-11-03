import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import qc.QCMetrics;
import qc.RandomRespondent;
import scalautils.OptData;
import scalautils.Response;
import survey.*;
import csv.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import org.junit.Test;

@RunWith(JUnit4.class)
public class EntropyTest extends TestLog {

    @Test
    public void testEntropy() 
            throws InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException, IOException {
         //create survey
         String separator = System.getProperty("file.separator");
         String filename = "data"+separator+"food_survey.csv";
         CSVParser parser = new CSVParser(new CSVLexer(filename, separator));
         Survey s = parser.parse();
         for(Question q: s.questions)
            LOGGER.info(q);
         //read in responses and create SurveyResponse
         ArrayList<SurveyResponse> responses = new ArrayList<SurveyResponse>();
         Scanner scanner = null;
         try {
             scanner = new Scanner(new File("data"+separator+"food results.csv"));
         } catch (FileNotFoundException ex) {
            LOGGER.fatal(ex);
            System.exit(-1);
         }
         int count=0;
         while(scanner.hasNextLine()){
             count++;
             String responseLine = scanner.nextLine();
             //System.out.println(responseLine);
             SurveyResponse curResponse = new SurveyResponse(""+count);
             String[] answers = responseLine.split(",");
             for(int i = 0; i < answers.length; i++){
                 Question curQ = s.questions.get(i);
                 String optid = "";
                 for(Component c: curQ.options.values()){
                     if(((StringComponent) c).data.equals(answers[i])){
                         optid = c.getCid();
                         break;
                     }
                 }
                 OptData ans = new OptData(optid, curQ.getOptById(optid).index);
                 Response res = new Response(curQ.quid, i, Arrays.asList(new OptData[]{ans}));
                //System.out.println(answers);
                curResponse.responses.add(new SurveyResponse.QuestionResponse(res, s, null));
            }
            //System.out.println(curResponse);
            responses.add(curResponse);
        }

        //generate group of random respondents
        Random rand = new Random();
        for(int x=0; x<4; x++){
            RandomRespondent rr = new RandomRespondent(s, RandomRespondent.AdversaryType.UNIFORM);
            responses.add(rr.response);
        }

        QCMetrics qc = new QCMetrics(null);
        ArrayList<SurveyResponse> outliers = QCMetrics.entropyBootstrap(s, responses, qc);
        LOGGER.info("~~~~~~OUTLIERS~~~~~~");
        LOGGER.info("# outliers: "+outliers.size());
        for(SurveyResponse o: outliers){
            System.out.println(o.toString());
            System.out.println();
        }

    }
}

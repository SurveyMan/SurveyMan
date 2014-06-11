import input.csv.CSVLexer;
import input.csv.CSVParser;
import input.exceptions.SyntaxException;
import interstitial.BackendType;
import interstitial.Record;
import interstitial.ResponseWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import qc.RandomRespondent;
import survey.Survey;
import survey.exceptions.SurveyException;
import system.SurveyResponse;
import system.generators.HTML;
import system.mturk.MturkLibrary;
import system.mturk.generators.MturkHTML;
import system.mturk.generators.MturkXML;

import java.io.IOException;
import java.io.StringReader;

@RunWith(JUnit4.class)
public class SystemTest extends TestLog {

    public SystemTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    @Test
    public void testMturkHTMLGenerator() throws Exception {
        try{
            for ( int i = 0 ; i < testsFiles.length ; i++ ) {
                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
                Survey survey = csvParser.parse();
                Record record = new Record(survey, new MturkLibrary(), BackendType.MTURK);
                HTML.getHTMLString(record, new MturkHTML());
                LOGGER.info(testsFiles[i]+" generated IHTML successfully.");
            }
        } catch (SurveyException se) {
            LOGGER.warn(se);
        }
    }

    @Test
    public void testXMLGenerator() throws Exception {
        try{
            for (int i = 0 ; i < testsFiles.length ; i++) {
                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
                MturkXML.getXMLString(csvParser.parse());
                LOGGER.info(testsFiles[i]+" generated IHTML successfully.");
            }
        } catch (SurveyException se) {
            LOGGER.warn(se);
        }
    }

    @Test
    public void testColumnPipeline() throws Exception {
        for (int i = 0 ; i < testsFiles.length ; i++) {
            try {
                System.out.println("File:"+testsFiles[i]);
                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
                Survey survey = csvParser.parse();
                RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
                String headers = ResponseWriter.outputHeaders(survey);
                String output = ResponseWriter.outputSurveyResponse(survey, rr.response);
                new SurveyResponse("").readSurveyResponses(survey, new StringReader(headers + output));
            } catch (SurveyException se) {
                if (super.outcome[i])
                    throw se;
            } catch (NullPointerException npe) {
                System.out.println(String.format("Were we expecting survey %s to succeed? %b", super.testsFiles[i], super.outcome[i]));
                if (super.outcome[i])
                    throw npe;
                else System.out.println("THIS NEEDS TO FAIL GRACEFULLY");
            }
        }
    }

//    @Test
//    public void testCorrelatedPipeline() throws Exception {
//        for (int i = 0 ; i < testsFiles.length ; i++) {
//            try {
//                System.out.println("File:"+testsFiles[i]);
//                CSVParser csvParser = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i])));
//                Survey survey = csvParser.parse();
//                if (!survey.correlationMap.isEmpty()) {
//                    System.out.println("input specifies correlations "+survey.correlationMap.entrySet());
//                    RandomRespondent rr = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
//                    String headerString = ResponseWriter.outputHeaders(survey);
//                    assert(headerString.contains(Survey.CORRELATION));
//                    String[] headers = headerString.split(",");
//                    // write a function to actually parse in the correlations and check against correlationMap
//                }
//            } catch (SurveyException se) {
//                if (super.outcome[i])
//                    throw se;
//            } catch (NullPointerException npe) {
//            System.out.println(String.format("Were we expecting survey %s to succeed? %b", super.testsFiles[i], super.outcome[i]));
//            if (super.outcome[i])
//                throw npe;
//            else System.out.println("THIS NEEDS TO FAIL GRACEFULLY");
//        }
//
//    }
//        System.out.println("Success");
//    }

}
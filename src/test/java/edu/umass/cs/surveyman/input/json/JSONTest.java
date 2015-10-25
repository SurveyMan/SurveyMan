package edu.umass.cs.surveyman.input.json;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.AbstractRule;
import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Slurpie;
//import junit.framework.Assert;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;

/**
 * Tests functions of the classes in the CSV package.
 * @author etosch
 */

@RunWith(JUnit4.class)
public class JSONTest extends TestLog {

    private static Logger LOGGER = LogManager.getLogger(JSONTest.class);

    public JSONTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    @Test
    public void testParse() throws Exception
    {
        //grab filenames that start with ex and try to make surveys out of them
        testSingleJsonFile("ex0.json");
        testSingleJsonFile("ex1.json");
        testSingleJsonFile("ex2.json");
    }

    public void testSingleJsonFile(String filename)
            throws SurveyException, IOException
    {
        LOGGER.debug(String.format("Parsing example %s", filename));
        String json = Slurpie.slurp(filename);
        LOGGER.debug("JSON:\t" + json);
        JSONParser parser = new JSONParser(json);
        LOGGER.debug(String.format("Creating survey for %s", filename));
        Survey s = parser.parse();
        LOGGER.debug("Parsed survey: " + s.toString());
    }

    @Test
    public void testEmptySurvey() throws SurveyException {
        String json = "{\"survey\": [ { \"id\": \"b_10101\", \"randomize\": true, \"questions\": [], \"subblocks\": [] }]}";
        JSONParser parser = new JSONParser(json);
        Survey s = parser.parse();
        Assert.assertTrue("Has one block", s.getAllBlocks().size() == 1);
        Assert.assertTrue("Has no questions", s.questions.size() == 0);
        try {
            AbstractRule.getDefaultRules();
            StaticAnalysis.staticAnalysis(s, Classifier.STACKED, 20, 0.25, 0.05, RandomRespondent.AdversaryType.UNIFORM);
            LOGGER.warn("Should never get here");
            Assert.fail();
        } catch (SurveyException se) {

        }
    }

    @Test
    public void testComplexJson() throws SurveyException {
        String json = "{\"survey\": [{" +
            "\"id\": \"b_10101\"," +
                "\"randomize\": true, " +
                "\"questions\": [{" +
                "\"id\": \"q_5847\", " +
                    "\"qtext\": \"First question\"," +
                    "\"options\": [{" +
                    "\"id\": \"o_97512\"," +
                        "\"otext\": \"first\"}, {" +
                    "\"id\": \"o_57705\"," +
                        "\"otext\": \"second\"}, {" +
                    "\"id\": \"o_32878\"," +
                        "\"otext\": \"third\"}]," +
                "\"ordered\": false," +
                    "\"freetext\": false, " +
                    "\"exclusive\": true}]," +
            "\"subblocks\": []}]}";
        JSONParser parser = new JSONParser(json);
        Survey s = parser.parse();
        AbstractRule.getDefaultRules();
        StaticAnalysis.staticAnalysis(s, Classifier.STACKED, 20, 0.25, 0.05, RandomRespondent.AdversaryType.UNIFORM);
    }

}

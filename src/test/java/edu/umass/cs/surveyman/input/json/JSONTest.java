package edu.umass.cs.surveyman.input.json;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.input.json.JSONParser;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Slurpie;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

}

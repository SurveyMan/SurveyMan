package edu.umass.cs.surveyman.input.json;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.input.json.JSONParser;
import edu.umass.cs.surveyman.survey.Survey;
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
    public void testParse() throws Exception {
        //grab filenames that start with ex and try to make surveys out of them
        String[] jsonExamples = Slurpie.slurp("json_test_data").split("\n");
        LOGGER.info(String.format("JSON test examples:\n\t%s", Arrays.toString(jsonExamples)));
        for (String f : jsonExamples) {
            LOGGER.debug(String.format("Parsing example %s", f));
            String json = Slurpie.slurp(f);
            LOGGER.debug("JSON:\t" + json);
            JSONParser parser = new JSONParser(json);
            LOGGER.debug(String.format("Creating survey for %s", f));
            Survey s = parser.parse();
            LOGGER.debug("Parsed survey: " + s.toString());
        }
    }

}

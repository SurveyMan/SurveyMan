import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.input.json.JSONParser;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.util.Printer;
import edu.umass.cs.surveyman.util.Slurpie;
import org.apache.logging.log4j.Level;
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

    public JSONTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    @Test
    public void testParse() throws Exception {
        //grab filenames that start with ex and try to make surveys out of them
        String[] jsonExamples = Slurpie.slurp("json_test_data").split("\n");
        for (String f : jsonExamples) {
            JSONParser parser = JSONParser.makeParser(f);
            Survey s = parser.parse();
            Printer.println("parsed survey:"+s.toString());
            LOGGER.log(Level.DEBUG, "parsed survey: " + s.toString());
        }
    }

}

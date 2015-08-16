package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.utils.Slurpie;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.StringReader;

/**
 * Created by etosch on 8/15/15.
 */
@RunWith(JUnit4.class)
public class SampleTests extends TestLog {

    public SampleTests() throws Exception {

    }

    @Test
    public void Ipierotis() throws Exception {
        // Ipierotis.csv fails due to a branch exception. The stationary blocks are listed out of order. Block 1 has
        // a branching question. The system states that the branching question is listed twice, listing the what
        // appears to be the same question.
        CSVParser csvParser = new CSVParser(new CSVLexer(new StringReader(Slurpie.slurp("Ipierotis.csv"))));
        Survey s = csvParser.parse();
        Assert.assertEquals("Should have three blocks total.", 3, s.blocks.size());
        Assert.assertEquals("17 questions total", 17, s.questions.size());
        Assert.assertEquals("7 questions in block 3", 7, s.blocks.get("3").questions.size());
        Assert.assertEquals("9 questions in block 1", 9, s.blocks.get("1").questions.size());
        Assert.assertEquals("1 question in block 2", 1, s.blocks.get("2").questions.size());
    }
}

package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Slurpie;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by etosch on 4/2/15.
 */
@RunWith(JUnit4.class)
public class SampleHomogenousMapsTest extends TestLog {

    public SampleHomogenousMapsTest()
            throws IOException,
            SyntaxException
    {
        super.init(this.getClass());
    }

    @Test
    public void testIpierotis()
            throws IOException, InvocationTargetException, SurveyException, IllegalAccessException,
            NoSuchMethodException
    {
        CSVLexer lexer = new CSVLexer(new StringReader(Slurpie.slurp("Ipierotis.csv")));
        Survey survey = new CSVParser(lexer).parse();
        Assert.assertEquals(Block.BranchParadigm.NONE, survey.blocks.get("3").getBranchParadigm());
        Assert.assertEquals(Block.BranchParadigm.NONE, survey.blocks.get("2").getBranchParadigm());
        Assert.assertEquals(Block.BranchParadigm.ONE, survey.blocks.get("1").getBranchParadigm());
        Assert.assertEquals(survey.getQuestionByLineNo(131), survey.blocks.get("1").branchQ);
        AbstractRule.getDefaultRules();
    }
}

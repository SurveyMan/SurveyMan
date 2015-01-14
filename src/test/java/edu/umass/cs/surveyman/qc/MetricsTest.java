package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.ISurveyResponse;
import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class MetricsTest extends TestLog {

    public static Block block1 = new Block("1");
    public static Block block2 = new Block("2");
    public static Block block3 = new Block("3");
    public static Block block4 = new Block("4");
    public static Question branchQuestion1 = new Question("asdf", 1, 1);
    public static Component a = new StringComponent("a", 1, 2);
    public static Component b = new StringComponent("b", 2, 2);
    public static Question branchQuestion2 = new Question("fdsa", 3, 1);
    public static Component c = new StringComponent("c", 3, 1);
    public static Component d = new StringComponent("d", 4, 1);
    public static Question noBranchQuestion1 = new Question("foo", 5, 1);
    public static Question noBranchQuestion2 = new Question("bar", 6, 1);
    public static Survey survey = new Survey();

    static {
        try {
            branchQuestion1.addOption(a, block2);
            branchQuestion1.addOption(b, block4);
            block1.addBranchQuestion(branchQuestion1);
            branchQuestion2.addOption(c, block3);
            branchQuestion2.addOption(d, block4);
            block2.addBranchQuestion(branchQuestion2);
            block3.addQuestion(noBranchQuestion1);
            block4.addQuestion(noBranchQuestion2);
            survey.addBlock(block1);
            survey.addBlock(block2);
            survey.addBlock(block3);
            survey.addBlock(block4);
            StaticAnalysis.wellFormednessChecks(survey);
        } catch (SurveyException e) {
            e.printStackTrace();
        }
    }

    public MetricsTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    @Test
    public void testGetDag() {

        List<List<Block>> answerDag = new ArrayList<List<Block>>();

        List<Block> path1 = new ArrayList<Block>();
        path1.add(block1);
        path1.add(block2);
        path1.add(block4);
        answerDag.add(path1);

        List<Block> path2 = new ArrayList<Block>();
        path2.add(block1);
        path2.add(block2);
        path2.add(block3);
        path2.add(block4);

        List<Block> path3 = new ArrayList<Block>();
        path3.add(block1);
        path3.add(block4);

        List<Block> blockList = new ArrayList<Block>();
        blockList.add(block1);
        blockList.add(block2);
        blockList.add(block3);
        blockList.add(block4);

        List<List<Block>> computedDag1 = QCMetrics.getDag(blockList);
        List<List<Block>> computedDag2 = QCMetrics.getDag(survey.topLevelBlocks);

        assert computedDag1.size() == 3 : "Expected path length of 3; got " + computedDag1.size();
        assert computedDag2.size() == 3 : "Expected path length of 3; got " + computedDag2.size();
        // TODO(etosch): show paths in dags are equivalent
    }

    @Test
    public void testGetQuestions() {
        int numQuestions = QCMetrics.getQuestions(survey.topLevelBlocks).size();
        assert numQuestions == 4: "Expected 4 questions; got "+numQuestions;
    }

    @Test
    public void testGetPaths() {
        List<List<Block>> paths = QCMetrics.getPaths(survey);
        assert paths.size() == 3 : "Expected 3 paths; got " + paths.size();
    }

    @Test
    public void testMinPath() {
        int minPathLength = QCMetrics.minimumPathLength(survey);
        assert  minPathLength == 2 : "Expected min path length of 2; got " + minPathLength;
        //TODO(etosch): test more survey instances
    }

    @Test
    public void testMaxPath() {
        assert QCMetrics.maximumPathLength(survey) == 4;
        //TODO(etosch): test more survey instances

    }

    @Test
    public void testTruncateResponses(){
        //TODO(etosch): write this
    }

    @Test
    public void testNonRandomRespondentFrequencies() {
//        AbstractRespondent profile = new NonRandomRespondent(survey);
//        List<ISurveyResponse> responses = new ArrayList<ISurveyResponse>();
//        for (int i = 0 ; i < 10 ; i++) {
//            responses.add(profile.getResponse());
//        }
        // none of the respondent profiles should be identical.
    }

}

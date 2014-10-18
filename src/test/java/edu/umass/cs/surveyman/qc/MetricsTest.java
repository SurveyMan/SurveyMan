package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class MetricsTest extends TestLog {

    private static Logger LOGGER = LogManager.getLogger(MetricsTest.class);

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
        block1.questions.add(branchQuestion1);
        // TODO(etosch): should have a better interface for adding options with branching
        branchQuestion1.options.put("a", a);
        branchQuestion1.options.put("b", b);
        // TODO(etosch): shouldn't have to have all these calls -- just creating them them should do the trick
        branchQuestion1.branchMap.put(a, block2);
        branchQuestion1.branchMap.put(b, block4);
        block1.branchParadigm = Block.BranchParadigm.ONE;
        block1.branchQ = branchQuestion1;
        block2.questions.add(branchQuestion2);
        branchQuestion2.options.put("c", c);
        branchQuestion2.options.put("d", d);
        branchQuestion2.branchMap.put(c, block3);
        branchQuestion2.branchMap.put(d, block4);
        block2.branchParadigm = Block.BranchParadigm.ONE;
        block2.branchQ = branchQuestion2;
        block3.questions.add(noBranchQuestion1);
        block4.questions.add(noBranchQuestion2);
        survey.blocks.put(block1.getStrId(), block1);
        survey.blocks.put(block2.getStrId(), block2);
        survey.blocks.put(block3.getStrId(), block3);
        survey.blocks.put(block4.getStrId(), block4);
        survey.topLevelBlocks.add(block1);
        survey.topLevelBlocks.add(block2);
        survey.topLevelBlocks.add(block3);
        survey.topLevelBlocks.add(block4);
    }

    public MetricsTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    @Test
    public void testGetDag(){

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

        assert computedDag1.size() == 3;
        assert computedDag2.size() == 3;
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

}

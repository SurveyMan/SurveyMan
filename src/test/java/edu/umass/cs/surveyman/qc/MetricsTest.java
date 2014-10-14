package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.StringComponent;
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

    public MetricsTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    @Test
    public void getDagTest(){
        Block block1 = new Block("1");
        Block block2 = new Block("2");
        Block block3 = new Block("3");
        Block block4 = new Block("4");
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
        Question branchQuestion1 = new Question("asdf", 1, 1);
        block1.questions.add(branchQuestion1);
        // TODO(etosch): should have a better interface for adding options with branching
        Component a = new StringComponent("a", 1, 2);
        Component b = new StringComponent("b", 2, 2);
        branchQuestion1.options.put("a", a);
        branchQuestion1.options.put("b", b);
        // TODO(etosch): shouldn't have to have all these calls -- just creating them them should do the trick
        branchQuestion1.branchMap.put(a, block2);
        branchQuestion1.branchMap.put(b, block4);
        block1.branchParadigm = Block.BranchParadigm.ONE;
        Question branchQuestion2 = new Question("fdsa", 3, 1);
        block2.questions.add(branchQuestion2);
        Component c = new StringComponent("c", 3, 1);
        Component d = new StringComponent("d", 4, 1);
        branchQuestion2.options.put("c", c);
        branchQuestion2.options.put("d", d);
        branchQuestion2.branchMap.put(c, block3);
        branchQuestion2.branchMap.put(d, block4);
        block2.branchParadigm = Block.BranchParadigm.ONE;
        Question noBranchQuestion1 = new Question("foo", 5, 1);
        block3.questions.add(noBranchQuestion1);
        Question noBranchQuestion2 = new Question("bar", 6, 1);
        block4.questions.add(noBranchQuestion2);

        List<Block> blockList = new ArrayList<Block>();
        blockList.add(block1);
        blockList.add(block2);
        blockList.add(block3);
        blockList.add(block4);

        List<List<Block>> computedDag = QCMetrics.getDag(blockList);

        assert computedDag.size() == answerDag.size();
        // TODO(etosch): show paths in dags are equivalent
    }

}

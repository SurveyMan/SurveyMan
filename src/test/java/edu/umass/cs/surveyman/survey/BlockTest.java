package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class BlockTest extends TestLog {

    public BlockTest()
            throws IOException,
            SyntaxException {
        super.init(this.getClass());
    }

    private static Survey s;
    private static Block b3, b31, b32, b4, b5;

    static {
//        3.1	q11
//        3.1	q12
//        3.1	q13	foo	4
//                  bar	4
//                  baz	5
//        3.2	q14	foo	NEXT
//                  bar	NEXT
//                  baz	NEXT
//        3.2	q15	foo	NEXT
//                  bar	NEXT
//                  baz	NEXT
//        3.2	q16	foo	NEXT
//                  bar	NEXT
//                  baz	NEXT
//        4	    q17
//        5	    q18
        s = new Survey();
        // Create all of the blocks
        b3 = new Block("3");
        b31 = new Block("3.1");
        b32 = new Block("3.2");
        b4 = new Block("4");
        b5 = new Block("5");
        // Add all the blocks to the survey
        try {
            b3.addBlock(b31);
            b3.addBlock(b32);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        s.addBlock(b3);
        s.addBlock(b4);
        s.addBlock(b5);
        // Create the string data
        StringDatum foo = new StringDatum("foo");
        StringDatum bar = new StringDatum("bar");
        StringDatum baz = new StringDatum("baz");
        // Create the questions
        Question q11 = new Question("q11");
        Question q12 = new Question("q12");
        Question q13 = new Question("q13");
        Question q14 = new Question("q14");
        Question q15 = new Question("q15");
        Question q16 = new Question("q16");
        Question q17 = new Question("q17");
        Question q18 = new Question("q18");
        try {
            // Add questions to the blocks.
            b31.addQuestions(q11, q12, q13);
            b32.addQuestions(q14, q15, q16);
            b4.addQuestion(q17);
            b5.addQuestion(q18);
            // Set branching
            q13.setBranchDest(foo, b4);
            q13.setBranchDest(bar, b4);
            q13.setBranchDest(baz, b5);
            q14.setBranchDest(foo, null);
            q14.setBranchDest(bar, null);
            q14.setBranchDest(baz, null);
            q15.setBranchDest(foo, null);
            q15.setBranchDest(bar, null);
            q15.setBranchDest(baz, null);
            q16.setBranchDest(foo, null);
            q16.setBranchDest(bar, null);
            q16.setBranchDest(baz, null);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    @Test
    public void testPropagate() throws SurveyException {
        for (Block b : BlockTest.s.getAllBlocks())
            b.propagateBranchParadigm();
        Assert.assertEquals(b31.branchParadigm, Block.BranchParadigm.ONE);
        Assert.assertEquals(b32.branchParadigm, Block.BranchParadigm.ALL);
        Assert.assertEquals(b3.branchParadigm, Block.BranchParadigm.ONE);
        Assert.assertEquals(b4.branchParadigm, Block.BranchParadigm.NONE);
        Assert.assertEquals(b5.branchParadigm, Block.BranchParadigm.NONE);
    }


}

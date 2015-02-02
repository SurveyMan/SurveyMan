package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.*;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void testRemoveFreetext() throws SurveyException {
        Question freetext = new Question("asdf");
        freetext.freetext = true;
        survey.addQuestion(freetext);
        int fullSize = survey.questions.size();
        int sizeWithoutFreetext = QCMetrics.removeFreetext(survey.questions).size();
        assert fullSize == 5 : String.format(
                "Expected the survey to have 5 questions; it had %d.",
                fullSize);
        assert sizeWithoutFreetext == 4 : String.format(
                "Expected the survey to have 4 questions without freetext; it had %d",
                sizeWithoutFreetext);
    }

    @Test
    public void testMakeFrequenciesForPaths() throws SurveyException {
        List<List<Block>> paths = QCMetrics.getPaths(survey);
        Assert.assertEquals("There should be 3 paths through the survey.", 3, paths.size());
        List<ISurveyResponse> responses = new ArrayList<ISurveyResponse>();
        AbstractRespondent r = new RandomRespondent(survey, RandomRespondent.AdversaryType.FIRST);
        responses.add(r.getResponse());
        Map<List<Block>, List<ISurveyResponse>> pathMap = QCMetrics.makeFrequenciesForPaths(paths, responses);
        Assert.assertEquals("There should be 3 unique paths key.", 3, pathMap.keySet().size());
        int totalRespondents = 0;
        for (List<ISurveyResponse> sr : pathMap.values())
            totalRespondents += sr.size();
        Assert.assertEquals("Expecting 1 response total.", 1, totalRespondents);
        // add another response
        responses.add(r.getResponse());
        pathMap = QCMetrics.makeFrequenciesForPaths(paths, responses);
        Assert.assertEquals("There should be 3 unique paths key.", 3, pathMap.keySet().size());
        totalRespondents = 0;
        for (List<ISurveyResponse> sr : pathMap.values())
            totalRespondents += sr.size();
        Assert.assertEquals("Expecting 2 responses total.", 2, totalRespondents);
    }

    @Test
    public void getEquivalentAnswerVariants() throws SurveyException {
        Block b = new Block("1");
        Question q1 = new Question("sadf");
        Question q2 = new Question("fdsa");
        Component c1 = new StringComponent("a", 1, 2);
        q1.addOption(c1);
        q1.addOptions("b", "c");
        q2.addOptions("d", "e", "f");
        b.addQuestion(q1);
        b.addQuestion(q2);
        List<Component> variants = QCMetrics.getEquivalentAnswerVariants(q1, c1);
        Assert.assertEquals("This variant set should be size 1.", 1, variants.size());
        b.branchParadigm = Block.BranchParadigm.ALL;
        b.propagateBranchParadigm();
        variants = QCMetrics.getEquivalentAnswerVariants(q1, c1);
        Assert.assertEquals("This variant set should be size 2.", 2, variants.size());
    }

    @Test
    public void testSurveyEntropy() throws SurveyException {
        Question q1 = new Question("asdf", true, true);
        Question q2 = new Question("fdsa", true, true);
        q1.randomize = false;
        q2.randomize = false;
        q1.addOption("A1");
        q1.addOption("B1");
        q2.addOption("A2");
        q2.addOption("B2");
        Survey survey1 = new Survey();
        survey1.addQuestions(q1, q2);
        // make two survey responses
        AbstractRespondent rr1 = new RandomRespondent(survey1, RandomRespondent.AdversaryType.FIRST);
        AbstractRespondent rr2 = new RandomRespondent(survey1, RandomRespondent.AdversaryType.LAST);
        List<ISurveyResponse> srs = new ArrayList<ISurveyResponse>();
        srs.add(rr1.getResponse());
        srs.add(rr2.getResponse());
        double expectedEntropy = 2.0;
        double observedEntropy = QCMetrics.surveyEntropy(survey1, srs);
        Assert.assertEquals(expectedEntropy, observedEntropy, 0.001);
    }

    @Test
    public void testSpearmansRank() throws SurveyException {
        final Question q1 = new Question("asdf", true, true);
        final Question q2 = new Question("fdsa", true, true);
        final Component c1 = new StringComponent("a", q1.getSourceRow(), Component.DEFAULT_SOURCE_COL);
        final Component c2 = new StringComponent("d", q2.getSourceRow(), Component.DEFAULT_SOURCE_COL);
        q1.addOption(c1);
        q1.addOptions("b", "c");
        q2.addOption(c2);
        q2.addOptions("e", "f");
        Map<String, IQuestionResponse> ansMap1 = new HashMap<String, IQuestionResponse>();
        Map<String, IQuestionResponse> ansMap2 = new HashMap<String, IQuestionResponse>();
        IQuestionResponse qr1 = new IQuestionResponse() {
            @Override
            public Question getQuestion() {
                return q1;
            }

            @Override
            public List<OptTuple> getOpts() {
                List<OptTuple> opts = new ArrayList<OptTuple>();
                opts.add(new OptTuple(c1, 0));
                return opts;
            }

            @Override
            public int getIndexSeen() {
                return 0;
            }
        };
        IQuestionResponse qr2 = new IQuestionResponse() {
            @Override
            public Question getQuestion() {
                return q2;
            }

            @Override
            public List<OptTuple> getOpts() {
                List<OptTuple> opts = new ArrayList<OptTuple>();
                opts.add(new OptTuple(c2, 0));
                return opts;
            }

            @Override
            public int getIndexSeen() {
                return 0;
            }
        };
        ansMap1.put("a", qr1);
        ansMap1.put("b", qr1);
        ansMap2.put("a", qr2);
        ansMap2.put("b", qr2);
        double rho = QCMetrics.spearmansRho(ansMap1, ansMap2);
        Assert.assertEquals("Rho should be 1", 1, rho, 0.001);
    }

    @Test
    public void testCramersV() throws SurveyException {
        final Question q1 = new Question("asdf");
        final Question q2 = new Question("fdsa");
        final Component c1 = new StringComponent("a", q1.getSourceRow(), Component.DEFAULT_SOURCE_COL);
        final Component c2 = new StringComponent("b", q1.getSourceRow() + 1, Component.DEFAULT_SOURCE_COL);
        final Component c3 = new StringComponent("c", q2.getSourceRow(), Component.DEFAULT_SOURCE_COL);
        final Component c4 = new StringComponent("d", q2.getSourceRow() + 1, Component.DEFAULT_SOURCE_COL);
        q1.addOption(c1);
        q1.addOption(c2);
        q2.addOption(c3);
        q2.addOption(c4);
        Map<String, IQuestionResponse> ansMap1 = new HashMap<String, IQuestionResponse>();
        Map<String, IQuestionResponse> ansMap2 = new HashMap<String, IQuestionResponse>();
        IQuestionResponse qr1 = new IQuestionResponse() {
            @Override
            public Question getQuestion() {
                return q1;
            }

            @Override
            public List<OptTuple> getOpts() {
                List<OptTuple> opts = new ArrayList<OptTuple>();
                opts.add(new OptTuple(c1, 0));
                return opts;
            }

            @Override
            public int getIndexSeen() {
                return 0;
            }
        };
        IQuestionResponse qr2 = new IQuestionResponse() {
            @Override
            public Question getQuestion() {
                return q1;
            }

            @Override
            public List<OptTuple> getOpts() {
                List<OptTuple> opts = new ArrayList<OptTuple>();
                opts.add(new OptTuple(c2, 1));
                return opts;
            }

            @Override
            public int getIndexSeen() {
                return 1;
            }
        };
        IQuestionResponse qr3 = new IQuestionResponse() {
            @Override
            public Question getQuestion() {
                return q2;
            }

            @Override
            public List<OptTuple> getOpts() {
                List<OptTuple> opts = new ArrayList<OptTuple>();
                opts.add(new OptTuple(c3, 0));
                return opts;
            }

            @Override
            public int getIndexSeen() {
                return 0;
            }
        };
        IQuestionResponse qr4 = new IQuestionResponse() {
            @Override
            public Question getQuestion() {
                return q2;
            }

            @Override
            public List<OptTuple> getOpts() {
                List<OptTuple> opts = new ArrayList<OptTuple>();
                opts.add(new OptTuple(c4, 1));
                return opts;
            }

            @Override
            public int getIndexSeen() {
                return 1;
            }
        };
        ansMap1.put("a", qr1);
        ansMap2.put("a", qr3);
        ansMap1.put("b", qr1);
        ansMap2.put("b", qr3);
        ansMap1.put("c", qr2);
        ansMap2.put("c", qr4);
        double v = QCMetrics.cramersV(ansMap1, ansMap2);
        Assert.assertEquals("V should be 1", 1, v, 0.001);
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

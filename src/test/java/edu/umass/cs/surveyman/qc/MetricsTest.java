package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.output.CorrelationStruct;
import edu.umass.cs.surveyman.qc.respondents.AbstractRespondent;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.*;

@RunWith(JUnit4.class)
public class MetricsTest extends TestLog {

    static class QuestionResponse implements IQuestionResponse {

        Question q;
        List<OptTuple> opts = new ArrayList<>();

        public QuestionResponse(Question q, OptTuple... opts) {
            this.q = q;
            this.opts.addAll(Arrays.asList(opts));
        }

        public int compareTo(Object o) {
            throw new RuntimeException("Not implemented.");
        }

        public Question getQuestion() {
            return q;
        }

        public List<OptTuple> getOpts() {
            return opts;
        }

        public int getIndexSeen() {
            throw new RuntimeException("Not implemented.");
        }

        public SurveyDatum getAnswer() throws SurveyException {
            assert this.getOpts().size() == 1 : "Can't call getAnswer when there's more than one response allows.";
            return this.getOpts().get(0).c;
        }

        public List<SurveyDatum> getAnswers() throws SurveyException {
            throw new RuntimeException("Not implemented.");
        }
    };

    private static Block block1;
    private static Block block2;
    private static Block block3;
    private static Block block4;
    private static Question branchQuestion1;
    public static SurveyDatum a;
    public static SurveyDatum b;
    private static Question branchQuestion2;
    public static SurveyDatum c;
    public static SurveyDatum d;
    private static Question noBranchQuestion1;
    private static Question noBranchQuestion2;
    public static Survey survey;
    private static QCMetrics qcMetrics;

    public void init() {
        block1 = new Block("1");
        block2 = new Block("2");
        block3 = new Block("3");
        block4 = new Block("4");
        branchQuestion1 = Question.makeQuestion("asdf", 1, 1);
        a = new StringDatum("a", 1, 2, 0);
        b = new StringDatum("b", 2, 2, 1);
        branchQuestion2 = Question.makeQuestion("fdsa", 3, 1);
        c = new StringDatum("c", 3, 1, 0);
        d = new StringDatum("d", 4, 1, 1);
        noBranchQuestion1 = Question.makeQuestion("foo", 5, 1);
        noBranchQuestion2 = Question.makeQuestion("bar", 6, 1);
        survey = new Survey();
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
            qcMetrics = new QCMetrics(survey);
        } catch (SurveyException e) {
            e.printStackTrace();
        }
    }

    public MetricsTest()
            throws IOException, SyntaxException {
        super.init(this.getClass());
        this.init();
    }

    @Test
    public void testLog2() {
        Assert.assertEquals("2^10 == 1024", 10., QCMetrics.log2(1024.), 0.00001);
        Assert.assertEquals("log_2 10 ~ 3.322", 3.322, QCMetrics.log2(10.), 0.001);
    }

    @Test
    public void testMaxPath() {
        init();
        Assert.assertEquals(4, qcMetrics.maximumPathLength());
        //TODO(etosch): test more survey instances
    }

    @Test
    public void testIsAnalyzable() {
        Assert.assertTrue(QCMetrics.isAnalyzable(branchQuestion1));
        Assert.assertTrue(QCMetrics.isAnalyzable(branchQuestion2));
        Assert.assertFalse(QCMetrics.isAnalyzable(noBranchQuestion1));
        Assert.assertFalse(QCMetrics.isAnalyzable(noBranchQuestion2));
    }

    @Test
    public void testFilterAnalyzable() {
        Assert.assertArrayEquals(
                new Question[]{branchQuestion1, branchQuestion2},
                QCMetrics.filterAnalyzable(Arrays.asList(survey.getQuestionListByIndex())).toArray()
                );
    }

    @Test
    public void getEquivalentAnswerVariants() throws SurveyException {
        init();
        Block b = new Block("1");
        Question q1 = new Question("sadf");
        Question q2 = new Question("fdsa");
        SurveyDatum c1 = new StringDatum("a", 1, 2, 0);
        q1.addOption(c1);
        q1.addOptions("b", "c");
        q2.addOptions("d", "e", "f");
        b.addQuestion(q1);
        b.addQuestion(q2);
        List<SurveyDatum> variants = QCMetrics.getEquivalentAnswerVariants(q1, c1);
        Assert.assertEquals("This variant set should be size 1.", 1, variants.size());
        b.updateBranchParadigm(Block.BranchParadigm.ALL);
        b.propagateBranchParadigm();
        variants = QCMetrics.getEquivalentAnswerVariants(q1, c1);
        Assert.assertEquals("This variant set should be size 2.", 2, variants.size());
    }

    @Test
    public void testSurveyEntropy() throws SurveyException {
        init();
        Question q1 = new RadioButtonQuestion("asdf", true);
        Question q2 = new RadioButtonQuestion("fdsa", true);
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
        List<SurveyResponse> srs = new ArrayList<>();
        srs.add(rr1.getResponse());
        srs.add(rr2.getResponse());
        double expectedEntropy = 2.0;
        double observedEntropy = QCMetrics.surveyEntropy(survey1, srs);
        Assert.assertEquals(expectedEntropy, observedEntropy, 0.001);
    }

    @Test
    public void testMaxEntropyOneQuestion() throws SurveyException {
        double tolerance = 0.0001;
        Assert.assertEquals("Question with two responses.", 1.0,
                QCMetrics.maxEntropyOneQuestion(branchQuestion1), tolerance);
        Assert.assertEquals("Nonanalyzable should return 0.", 0.0,
                QCMetrics.maxEntropyOneQuestion(noBranchQuestion2), tolerance);
        Assert.assertEquals("Question with four responses.", 2.0,
                QCMetrics.maxEntropyOneQuestion(new Question("qnew",
                        new StringDatum("a"),
                        new StringDatum("b"),
                        new StringDatum("c"),
                        new StringDatum("d"))),
                tolerance);
    }

    @Test
    public void testMaxEntropyQuestionList() {
        double tolerance = 0.1;
        Assert.assertEquals("Max entropy in this survey's question list.", 2.0, QCMetrics.maxEntropyQuestionList(survey.questions), tolerance);
    }
    
    @Test
    public void testGetMaxPathForEntropy() {
        //// TODO: 7/10/16 Write this test 
    }
    
    @Test
    public void testMaxEntropy() {
        //// TODO: 7/10/16 Write this test
    }

    @Test
    public void testMinPath() {
        init();
        int minPathLength = qcMetrics.minimumPathLength();
        Assert.assertEquals(2, minPathLength);
        //TODO(etosch): test more survey instances
    }

    @Test
    public void testAvgPath() {
        // // TODO: 7/10/16 Write this test
    }

    @Test
    public void testGetDag() {

        init();

        SurveyPath path1 = new SurveyPath();
        path1.add(block1);
        path1.add(block2);
        path1.add(block4);

        SurveyPath path2 = new SurveyPath();
        path2.add(block1);
        path2.add(block2);
        path2.add(block3);
        path2.add(block4);

        SurveyPath path3 = new SurveyPath();
        path3.add(block1);
        path3.add(block4);

        SurveyDAG answer = new SurveyDAG(survey, path1, path2, path3);

        List<Block> blockList = new ArrayList<>();
        blockList.add(block1);
        blockList.add(block2);
        blockList.add(block3);
        blockList.add(block4);

        Survey cmp = new Survey();
        for (Block block: blockList) {
            cmp.addBlock(block);
        }

        SurveyDAG computedDag = SurveyDAG.getDag(cmp);

        assert computedDag.size() == 3 : "Expected path length of 3; got " + computedDag.size();
        assert answer.size() == 3 : "Expected path length of 3; got " + answer.size();
        // TODO(etosch): show paths in dags are equivalent
        Assert.assertEquals("DAGs are equal", answer, computedDag);
    }

    @Test
    public void testGetPaths() {
        init();
        int numpaths = SurveyDAG.getPaths(survey).size();
        Assert.assertEquals(3, numpaths);
    }

    @Test
    public void testComputeRanks() {
        // // TODO: 7/10/16 write this test
    }

    @Test
    public void testSpearmansRho() throws SurveyException {
        init();
        final Question q1 = new RadioButtonQuestion("asdf", true);
        final Question q2 = new RadioButtonQuestion("fdsa", true);
        final SurveyDatum c1 = new StringDatum("a");
        final SurveyDatum c2 = new StringDatum("d");
        q1.addOption(c1);
        q1.addOptions("b", "c");
        q2.addOption(c2);
        q2.addOptions("e", "f");
        Map<String, IQuestionResponse> ansMap1 = new HashMap<String, IQuestionResponse>();
        Map<String, IQuestionResponse> ansMap2 = new HashMap<String, IQuestionResponse>();
        QuestionResponse qr1 = new QuestionResponse(q1, new OptTuple(c1, 0));
        QuestionResponse qr2 = new QuestionResponse(q2, new OptTuple(c2, 0));
        ansMap1.put("a", qr1);
        ansMap1.put("b", qr1);
        ansMap2.put("a", qr2);
        ansMap2.put("b", qr2);
        double rho = QCMetrics.spearmansRho(ansMap1, ansMap2);
        Assert.assertEquals("Rho should be 1", 1, rho, 0.001);
    }
    
    @Test
    public void testCellExpectation() {
        // TODO: 7/10/16 write this test     
    }
    
    @Test
    public void testChiSquared() {
        // TODO: 7/10/16 write this test 
    }
    
    @Test
    public void testTruncateResponses() {
        //TODO(etosch): write this
    }

    @Test
    public void testRemoveFreetext() throws SurveyException {
        init();
        Question freetext = new Question("asdf");
        freetext.freetext = true;
        survey.addQuestion(freetext);
        int fullSize = survey.questions.size();
        int sizeWithoutFreetextAndInstructions = QCMetrics.filterAnalyzable(survey.questions).size();
        Assert.assertEquals(5, fullSize);
        Assert.assertEquals(2, sizeWithoutFreetextAndInstructions);
    }

    @Test
    public void testMakeFrequenciesForPaths() throws SurveyException {
        init();
        SurveyDAG paths = SurveyDAG.getDag(survey);
        Assert.assertEquals("There should be 3 paths through the survey.", 3, paths.size());
        List<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        AbstractRespondent r = new RandomRespondent(survey, RandomRespondent.AdversaryType.FIRST);
        responses.add(r.getResponse());
        PathFrequencyMap pathMap = PathFrequencyMap.makeFrequenciesForPaths(paths, responses);
        Assert.assertEquals("There should be 3 unique paths key.", 3, pathMap.keySet().size());
        int totalRespondents = 0;
        for (List<SurveyResponse> sr : pathMap.values())
            totalRespondents += sr.size();
        Assert.assertEquals("Expecting 1 response total.", 1, totalRespondents);
        // add another response
        responses.add(r.getResponse());
        pathMap = PathFrequencyMap.makeFrequenciesForPaths(paths, responses);
        Assert.assertEquals("There should be 3 unique paths key.", 3, pathMap.keySet().size());
        totalRespondents = 0;
        for (List<SurveyResponse> sr : pathMap.values())
            totalRespondents += sr.size();
        Assert.assertEquals("Expecting 2 responses total.", 2, totalRespondents);
    }

    @Test
    public void testCramersVSimple() throws SurveyException {
        init();
        final Question q1 = new RadioButtonQuestion("asdf", true);
        final Question q2 = new RadioButtonQuestion("fdsa", true);
        SurveyDatum c11 = new StringDatum("a");
        SurveyDatum c12 = new StringDatum("b");
        SurveyDatum c21 = new StringDatum("c");
        SurveyDatum c22 = new StringDatum("d");
        q1.addOption(c11);
        q1.addOption(c12);
        q2.addOption(c21);
        q2.addOption(c22);
        Map<String, IQuestionResponse> ansMap1 = new HashMap<>();
        Map<String, IQuestionResponse> ansMap2 = new HashMap<>();
        QuestionResponse qr1 = new QuestionResponse(q1, new OptTuple(c11, 0));
        QuestionResponse qr2 = new QuestionResponse(q1, new OptTuple(c12, 1));
        QuestionResponse qr3 = new QuestionResponse(q2, new OptTuple(c21, 0));
        QuestionResponse qr4 = new QuestionResponse(q2, new OptTuple(c22, 1));
        ansMap1.put("a", qr1);
        ansMap2.put("a", qr3);
        ansMap1.put("b", qr1);
        ansMap2.put("b", qr3);
        ansMap1.put("c", qr2);
        ansMap2.put("c", qr4);
        double v = CorrelationStruct.makeStruct(q1, q2, ansMap1, ansMap2).coefficientValue;
        Assert.assertEquals("V should be 1", 1, v, 0.001);
    }

    private int addNResponses(
            Map<String, IQuestionResponse> ansMap1,
            Map<String, IQuestionResponse> ansMap2,
            Question q1,
            Question q2,
            SurveyDatum opt1,
            int pos1,
            SurveyDatum opt2,
            int pos2,
            int ct,
            int offset)
    {
        while (ct > 0) {
            ansMap1.put("rr" + offset, new QuestionResponse(q1, new OptTuple(opt1, pos1)));
            ansMap2.put("rr" + offset, new QuestionResponse(q2, new OptTuple(opt2, pos2)));
            offset++;
            ct--;
        }
        return offset;
    }

    @Test
    public void testCramersVComplex() throws SurveyException {
        init();
        final Question q1 = new RadioButtonQuestion("a", false);
        final Question q2 = new RadioButtonQuestion("b", false);
        SurveyDatum a1 = new StringDatum("a1");
        SurveyDatum a2 = new StringDatum("a2");
        SurveyDatum b1 = new StringDatum("b1");
        SurveyDatum b2 = new StringDatum("b2");
        SurveyDatum b3 = new StringDatum("b3");
        q1.addOption(a1);
        q1.addOption(a2);
        q2.addOption(b1);
        q2.addOption(b2);
        q2.addOption(b3);
        Map<String, IQuestionResponse> ansMap1 = new HashMap<>();
        Map<String, IQuestionResponse> ansMap2 = new HashMap<>();
        // Generate the maps that will produce a contingency table that looks like this:
        //       a1  a2
        //     ----------
        // b1 |  5  |  5 |
        // b2 | 20  | 10 |
        // b3 | 45  | 15 |
        //     ----------
        // There should be 100 responses total.
        int offset = addNResponses(ansMap1, ansMap2, q1, q2, a1, 0, b1, 0, 5, 0); // (a1, b1)
        offset = addNResponses(ansMap1, ansMap2, q1, q2, a2, 1, b1, 0, 5, offset); // (a2, b1)
        offset = addNResponses(ansMap1, ansMap2, q1, q2, a1, 0, b2, 1, 20, offset); // (a1, b2)
        offset = addNResponses(ansMap1, ansMap2, q1, q2, a2, 1, b2, 1, 10, offset); // (a2, b2)
        offset = addNResponses(ansMap1, ansMap2, q1, q2, a1, 0, b3, 2, 45, offset); // (a1, b3)
        addNResponses(ansMap1, ansMap2, q1, q2, a2, 1, b3, 2, 15, offset); // (a2, b3)
        Assert.assertEquals(100, ansMap1.size());
        Assert.assertEquals(100, ansMap2.size());
        CorrelationStruct c = CorrelationStruct.makeStruct(q2, q1, ansMap2, ansMap1);
        double v = c.coefficientValue;
        double p = c.coefficientPValue;
        CoefficentsAndTests co = c.coefficientType;
        Assert.assertEquals(CoefficentsAndTests.V, co);
        Assert.assertEquals("V should be 0.166666...", 0.1666, v, 0.001);
        Assert.assertEquals("p-value should be 0.2491", 0.2491, p, 0.001);
        // Now remove the responses from one of the cells so one response pair has a cell value of 0
        for (int i = 0; i < 5; i++) {
            // remove all respondents who answered (a1, b1)
            ansMap1.remove("rr" + i);
            ansMap2.remove("rr" + i);
        }
        c = CorrelationStruct.makeStruct(q1, q2, ansMap1, ansMap2);
        v = c.coefficientValue;
        p = c.coefficientPValue;
        Assert.assertEquals("V should be close to 0.3565", 0.3566, v, 0.001);
        Assert.assertEquals("p-value should be 0.0024", 0.0024, p, 0.001);
    }


    @Test
    public void testNonRandomRespondentFrequencies() {
//        AbstractRespondent profile = new NonRandomRespondent(survey);
//        List<AbstractSurveyResponse> responses = new ArrayList<AbstractSurveyResponse>();
//        for (int i = 0 ; i < 10 ; i++) {
//            responses.add(profile.getResponse());
//        }
        // none of the respondent profiles should be identical.
        init();
    }

}

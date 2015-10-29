package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.*;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
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
            throw new RuntimeException("Not implemented.");
        }

        public List<SurveyDatum> getAnswers() throws SurveyException {
            throw new RuntimeException("Not implemented.");
        }
    };

    public static Block block1;
    public static Block block2;
    public static Block block3;
    public static Block block4;
    public static Question branchQuestion1;
    public static SurveyDatum a;
    public static SurveyDatum b;
    public static Question branchQuestion2;
    public static SurveyDatum c;
    public static SurveyDatum d;
    public static Question noBranchQuestion1;
    public static Question noBranchQuestion2;
    public static Survey survey;
    public static QCMetrics qcMetrics;

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
        } catch (SurveyException e) {
            e.printStackTrace();
        }
        qcMetrics = new QCMetrics(survey, false);
    }

    public MetricsTest()
            throws IOException, SyntaxException {
        super.init(this.getClass());
        this.init();
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

        SurveyDAG answer = new SurveyDAG(path1, path2, path3);

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
    public void testGetQuestions() {
        init();
        Assert.assertEquals(survey.topLevelBlocks.size(), 4);
        SurveyDAG surveyDAG = SurveyDAG.getDag(survey);
        Assert.assertEquals("Number of paths in the survey.", 1, surveyDAG.size());
        int numQuestions = surveyDAG.get(0).getPathLength();
        Assert.assertEquals("Number of questions on the path.", 4, numQuestions);
    }

    @Test
    public void testGetPaths() {
        init();
        int numpaths = SurveyDAG.getPaths(survey).size();
        Assert.assertEquals(3, numpaths);
    }

    @Test
    public void testMinPath() {
        init();
        int minPathLength = qcMetrics.minimumPathLength();
        Assert.assertEquals(2, minPathLength);
        //TODO(etosch): test more survey instances
    }

    @Test
    public void testMaxPath() {
        init();
        Assert.assertEquals(4, qcMetrics.maximumPathLength());
        //TODO(etosch): test more survey instances
    }

    @Test
    public void testTruncateResponses() {
        //TODO(etosch): write this
    }

    @Test
    public void testRemoveFreetext()
            throws SurveyException {
        init();
        Question freetext = new Question("asdf");
        freetext.freetext = true;
        survey.addQuestion(freetext);
        int fullSize = survey.questions.size();
        int sizeWithoutFreetext = QCMetrics.filterAnalyzable(survey.questions).size();
        Assert.assertEquals(5, fullSize);
        Assert.assertEquals(4, sizeWithoutFreetext);
    }

    @Test
    public void testMakeFrequenciesForPaths()
            throws SurveyException {
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
    public void getEquivalentAnswerVariants()
            throws SurveyException {
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
    public void testSurveyEntropy()
            throws SurveyException {
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
        List<SurveyResponse> srs = new ArrayList<SurveyResponse>();
        srs.add(rr1.getResponse());
        srs.add(rr2.getResponse());
        double expectedEntropy = 2.0;
        double observedEntropy = QCMetrics.surveyEntropy(survey1, srs);
        Assert.assertEquals(expectedEntropy, observedEntropy, 0.001);
    }

    @Test
    public void testSpearmansRank()
            throws SurveyException {
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
    public void testCramersVSimple()
            throws SurveyException {
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
        double v = QCMetrics.cramersV(ansMap1, ansMap2);
        Assert.assertEquals("V should be 1", 1, v, 0.001);
    }

    @Test
    public void testCramersVComplex()
        throws SurveyException {
        init();
        final Question q1 = new RadioButtonQuestion("a", true);
        final Question q2 = new RadioButtonQuestion("b", true);
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
        int respondent_index = 0;
        while (respondent_index < 5) {
            // (a1, b1)
            ansMap1.put("rr" + respondent_index, new QuestionResponse(q1, new OptTuple(a1, 0)));
            ansMap2.put("rr" + respondent_index, new QuestionResponse(q2, new OptTuple(b1, 0)));
            respondent_index++;
        }
        respondent_index = 0;
        while (respondent_index < 5) {
            // (a2, b1)
            ansMap1.put("rr" + respondent_index + 1, new QuestionResponse(q1, new OptTuple(a2, 1)));
            ansMap2.put("rr" + respondent_index + 1, new QuestionResponse(q2, new OptTuple(b1, 0)));
            respondent_index++;
        }
        respondent_index = 0;
        while (respondent_index < 20) {
            // (a1, b2)
            ansMap1.put("rr" + respondent_index + 2, new QuestionResponse(q1, new OptTuple(a1, 0)));
            ansMap2.put("rr" + respondent_index + 2, new QuestionResponse(q2, new OptTuple(b2, 1)));
            respondent_index++;
        }
        respondent_index = 0;
        while (respondent_index < 10) {
            // (a2, b2)
            ansMap1.put("rr" + respondent_index + 3, new QuestionResponse(q1, new OptTuple(a2, 1)));
            ansMap2.put("rr" + respondent_index + 3, new QuestionResponse(q2, new OptTuple(b2, 1)));
            respondent_index++;
        }
        respondent_index = 0;
        while (respondent_index < 45) {
            // (a1, b3)
            ansMap1.put("rr" + respondent_index + 4, new QuestionResponse(q1, new OptTuple(a1, 0)));
            ansMap2.put("rr" + respondent_index + 4, new QuestionResponse(q2, new OptTuple(b3, 2)));
            respondent_index++;
        }
        respondent_index = 0;
        while (respondent_index < 15) {
            // (a2, b3)
            ansMap1.put("rr" + respondent_index + 5, new QuestionResponse(q1, new OptTuple(a2, 1)));
            ansMap2.put("rr" + respondent_index + 5, new QuestionResponse(q2, new OptTuple(b3, 2)));
            respondent_index++;
        }
        double v = QCMetrics.cramersV(ansMap1, ansMap2);
        Assert.assertEquals("V should be 0.166666...", 0.1666, v, 0.001);
        // Now remove the responses from one of the cells so one response pair has a cell value of 0
        for (int i = 0; i < 5; i++) {
            // remove all respondents who answered (a1, b1)
            ansMap1.remove("rr" + i);
            ansMap2.remove("rr" + i);
        }
        v = QCMetrics.cramersV(ansMap1, ansMap2);
        Assert.assertEquals("V should be close to 0.3565", 0.3565, v, 0.001);
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

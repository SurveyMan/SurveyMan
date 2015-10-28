package edu.umass.cs.surveyman.qc.respondents;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.qc.AnswerProbabilityMap;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.qc.respondents.AbstractRespondent;
import edu.umass.cs.surveyman.qc.respondents.LexicographicRespondent;
import edu.umass.cs.surveyman.qc.respondents.NonRandomRespondent;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class RespondentTest extends TestLog {

    public RespondentTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    private boolean between(double upper, double lower, double i) {
        return i < upper && i > lower;
    }

    @Test
    public void testUniformAdversary()
            throws InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException,
                   IOException
    {
        // assert that for each survey, this adversary chooses the position at random
        LOGGER.info("Executing testUniformAdversary.");
        try {
            Question[] questions = new Question[10];
            Question.makeUnorderedRadioQuestions(questions, "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
            for (Question q : questions) {
                q.addOption("");
                q.addOption("");
            }
            Survey survey = new Survey(questions);
            RandomRespondent randomRespondent = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
            SurveyResponse surveyResponse = randomRespondent.getResponse();
            // assert that we don't deviate more than what's expected
            double posPref  =   0.0,
                   eps      =   Math.pow((surveyResponse.getNonCustomResponses().size() * Math.log(0.05)) / - 2.0, 0.5),
                   mean     =   0.5;
            assert surveyResponse.getNonCustomResponses().size() > 0 : String.format("Survey response (%s) is empty for survey %s"
                    , surveyResponse.getSrid(), survey.sourceName);
            for (IQuestionResponse qr : surveyResponse.getNonCustomResponses()) {
                if (qr.getIndexSeen() > -1 && qr.getQuestion().getOptListByIndex().length > 1)
                    posPref += ((double) (qr.getIndexSeen() + 1)) / (double) randomRespondent.getDenominator(qr.getQuestion());
                else LOGGER.warn(String.format("Question %s has index %d with opt list size %d"
                        , qr.getQuestion().id
                        , qr.getIndexSeen()
                        , qr.getOpts().size()));
            }
            posPref = posPref / surveyResponse.getNonCustomResponses().size();
            LOGGER.info(String.format("posPref : %f\teps : %f", posPref, eps));
            assert between(mean + eps, mean - eps, posPref) :
                    String.format("Position preference (%f) deviates too far from the mean (%f, with eps %f) in survey %s for the uniform adversary"
                                , posPref, mean, eps, survey.sourceName);
        } catch (SurveyException se) {
            System.out.println(String.format("Were we expecting survey %s to succeed? %b", super.testsFiles[0], super.outcome[0]));
            if (super.outcome[0])
                throw se;
        } catch (NullPointerException npe) {
            System.out.println(String.format("Were we expecting survey %s to succeed? %b", super.testsFiles[0], super.outcome[0]));
            if (super.outcome[0])
                throw npe;
            else System.out.println("THIS NEEDS TO FAIL GRACEFULLY");
        }
    }

    @Test
    public void testProfile() throws InvocationTargetException, SurveyException, IllegalAccessException,
            NoSuchMethodException, IOException {
        LOGGER.info("Executing testProfile.");
        // write a survey with 5 yes/no answers.
        StringReader surveyReader = new StringReader(
                "question,options\n" +
                "q1,true\n,false\n" +
                "q2,true\n,false\n" +
                "q3,true\n,false\n" +
                "q4,true\n,false\n" +
                "q5,true\n,false");
        Survey survey1 = new CSVParser(new CSVLexer(surveyReader)).parse();
        assert survey1.questions.size() == 5;
        // 32 possible answers
        NonRandomRespondent profile = new NonRandomRespondent(survey1);
        assert profile.answers.size() == 5 : "Expected answer set size 5; got " + profile.answers.size();
        assert profile.strength.size() == 5 : "Expected string size 5; got " + profile.strength.size();
        LOGGER.debug("Preference Profile:");
        for (Map.Entry<Question, SurveyDatum> entry : profile.answers.entrySet()) {
            double strength = profile.strength.get(entry.getValue());
            LOGGER.debug(String.format("%s\t%s\t%f",
                    entry.getKey().id,
                    entry.getValue().getId(),
                    strength)
            );
        }
        SurveyResponse sr1 = profile.getResponse();
        SurveyResponse sr2 = profile.getResponse();
        SurveyResponse sr3 = profile.getResponse();
        assert sr1 != null : String.format("getResponse on the profile for %s is not working.", profile.getClass().getName());
        LOGGER.debug("Actual responses:");
        for (IQuestionResponse qr1 : sr1.getNonCustomResponses()) {
            IQuestionResponse qr2 = sr2.resultsAsMap().get(qr1.getQuestion().id);
            IQuestionResponse qr3 = sr3.resultsAsMap().get(qr1.getQuestion().id);
            StringBuilder sb1 = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            StringBuilder sb3 = new StringBuilder();
            for (OptTuple optTuple : qr1.getOpts())
                sb1.append(optTuple.c.getId());
            for (OptTuple optTuple : qr2.getOpts())
                sb2.append(optTuple.c.getId());
            for (OptTuple optTuple : qr3.getOpts())
                sb3.append(optTuple.c.getId());
            LOGGER.debug(String.format("%s\t%s\t%s\t%s",
                    qr1.getQuestion().id,
                    sb1.toString(),
                    sb2.toString(),
                    sb3.toString())
                    );
        }
        LOGGER.info("Finished executing testProfile.");
        // They should be nonrandom, but they should also not be exactly the same.
    }

    @Test
    public void testNonRandomRespondent() throws InvocationTargetException, SurveyException, IllegalAccessException,
            NoSuchMethodException, IOException {
        LOGGER.info("Executing testNonRandomRespondent.");
        Survey survey = new CSVParser(new CSVLexer("./src/test/resources/pick_randomly.csv", ",")).parse();
        AbstractRespondent profile = new NonRandomRespondent(survey);
        SurveyResponse sr1 = profile.getResponse();
        SurveyResponse sr2 = profile.getResponse();
        SurveyResponse sr3 = profile.getResponse();
        SurveyResponse sr4 = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM).getResponse();
        SurveyResponse sr5 = new RandomRespondent(survey, RandomRespondent.AdversaryType.FIRST).getResponse();
        SurveyResponse sr6 = profile.getResponse();
        SurveyResponse sr7 = profile.getResponse();
        LOGGER.info("Generated 4 profiled responses, 1 uniform response, 1 first position preference");
        List<SurveyResponse> srs = new ArrayList<SurveyResponse>();
        srs.add(sr1);
        srs.add(sr2);
        srs.add(sr3);
        LOGGER.info("Added 3 profiled responses to the list of responses.");
        QCMetrics qcMetrics = new QCMetrics(survey, false);
        qcMetrics.makeProbabilities(srs);
        double ll1 = qcMetrics.getLLForResponse(sr1);
        double ent1 = qcMetrics.getEntropyForResponse(sr1);
        double ll2 = qcMetrics.getLLForResponse(sr2);
        double ent2 = qcMetrics.getEntropyForResponse(sr2);
        double ll3 = qcMetrics.getLLForResponse(sr3);
        double ent3 = qcMetrics.getEntropyForResponse(sr3);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f",
                ll1, ent1, ll2, ent2, ll3, ent3)
        );
        LOGGER.debug(String.format("\n\tFirst ent bot?:\t%b\tFirst LL bot?:\t%b\n" +
                "\n\tSecond ent bot?:%b\tSecond LL bot?:\t%b\n" +
                "\n\tThird ent bot?:\t%b\tThird LL bot?:\t%b\n",
                qcMetrics.entropyClassification(sr1, srs,  0.05),
                qcMetrics.logLikelihoodClassification(sr1, srs,  0.05),
                qcMetrics.entropyClassification(sr2, srs,  0.05),
                qcMetrics.logLikelihoodClassification(sr2, srs, 0.05),
                qcMetrics.entropyClassification(sr3, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr3, srs, 0.05))
        );
        LOGGER.debug("Adding a uniform responder.");
        srs.add(sr4);
        qcMetrics.makeProbabilities(srs);
        ll1 = qcMetrics.getLLForResponse(sr1);
        ent1 = qcMetrics.getEntropyForResponse(sr1);
        ll2 = qcMetrics.getLLForResponse(sr2);
        ent2 = qcMetrics.getEntropyForResponse(sr2);
        ll3 = qcMetrics.getLLForResponse(sr3);
        ent3 = qcMetrics.getEntropyForResponse(sr3);
        double ll4 = qcMetrics.getLLForResponse(sr4);
        double ent4 = qcMetrics.getEntropyForResponse(sr4);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f\n" +
                "\tUnif ll:\t%f\tUnif ent:\t%f\n",
                ll1, ent1, ll2, ent2, ll3, ent3, ll4, ent4));
        LOGGER.debug(String.format("\n\tFirst ent bot?:\t%b\tFirst LL bot?:\t%b\n" +
                "\tSecond ent bot?:%b\tSecond LL bot?:\t%b\n" +
                "\tThird ent bot?:\t%b\tThird LL bot?:\t%b\n" +
                "\tUnif ent bot?:\t%b\tUnif LL bot?:\t%b\n",
                qcMetrics.entropyClassification(sr1, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr1, srs, 0.05),
                qcMetrics.entropyClassification(sr2, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr2, srs, 0.05),
                qcMetrics.entropyClassification(sr3, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr3, srs, 0.05),
                qcMetrics.entropyClassification(sr4, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr4, srs, 0.05))
        );
        LOGGER.debug("Adding positional preference.");
        srs.add(sr5);
        qcMetrics.makeProbabilities(srs);
        ll1 = qcMetrics.getLLForResponse(sr1);
        ent1 = qcMetrics.getEntropyForResponse(sr1);
        ll2 = qcMetrics.getLLForResponse(sr2);
        ent2 = qcMetrics.getEntropyForResponse(sr2);
        ll3 = qcMetrics.getLLForResponse(sr3);
        ent3 = qcMetrics.getEntropyForResponse(sr3);
        ll4 = qcMetrics.getLLForResponse(sr4);
        ent4 = qcMetrics.getEntropyForResponse(sr4);
        double ll5 = qcMetrics.getLLForResponse(sr5);
        double ent5 = qcMetrics.getEntropyForResponse(sr5);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f\n" +
                "\tUnif ll:\t%f\tUnif ent:\t%f\n" +
                "\tPos 1 ll:\t%f\tPos 1 ent:\t%f\n",
                ll1, ent1, ll2, ent2, ll3, ent3, ll4, ent4, ll5, ent5));
        LOGGER.debug(String.format("\n\tFirst ent bot?:\t%b\tFirst LL bot?:\t%b\n" +
                "\tSecond ent bot?:%b\tSecond LL bot?:\t%b\n" +
                "\tThird ent bot?:\t%b\tThird LL bot?:\t%b\n" +
                "\tUnif ent bot?:\t%b\tUnif LL bot?:\t%b\n" +
                "\tPos 1 ent bot?:\t%b\tPos 1 LL bot?\t%b\b",
                qcMetrics.entropyClassification(sr1, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr1, srs, 0.05),
                qcMetrics.entropyClassification(sr2, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr2, srs, 0.05),
                qcMetrics.entropyClassification(sr3, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr3, srs, 0.05),
                qcMetrics.entropyClassification(sr4, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr4, srs, 0.05),
                qcMetrics.entropyClassification(sr5, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr5, srs, 0.05)
        ));
        srs.add(sr6);
        qcMetrics.makeProbabilities(srs);
        ll1 = qcMetrics.getLLForResponse(sr1);
        ent1 = qcMetrics.getEntropyForResponse(sr1);
        ll2 = qcMetrics.getLLForResponse(sr2);
        ent2 = qcMetrics.getEntropyForResponse(sr2);
        ll3 = qcMetrics.getLLForResponse(sr3);
        ent3 = qcMetrics.getEntropyForResponse(sr3);
        ll4 = qcMetrics.getLLForResponse(sr4);
        ent4 = qcMetrics.getEntropyForResponse(sr4);
        ll5 = qcMetrics.getLLForResponse(sr5);
        ent5 = qcMetrics.getEntropyForResponse(sr5);
        double ll6 = qcMetrics.getLLForResponse(sr6);
        double ent6 = qcMetrics.getEntropyForResponse(sr6);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f\n" +
                "\tUnif ll:\t%f\tUnif ent:\t%f\n" +
                "\tPos 1 ll:\t%f\tPos 1 ent:\t%f\n" +
                "\tFourth ll:\t%f\tFourth ent:\t%f",
                ll1, ent1, ll2, ent2, ll3, ent3, ll4, ent4, ll5, ent5, ll6, ent6));
        LOGGER.debug(String.format("\n\tFirst LL bot?:\t%b\tFirst ent bot?:\t%b\n" +
                "\tSecond LL bot?:\t%b\tSecond ent bot?:%b\n" +
                "\tThird LL bot?:\t%b\tThird ent bot?:\t%b\n" +
                "\tUnif LL bot?:\t%b\tUnif ent bot?:\t%b\n" +
                "\tPos 1 LL bot?:\t%b\tPos 1 ent bot?:\t%b\n" +
                "\tFourth LL bot?:\t%b\tFourth ent bot?:\t%b\n",
                qcMetrics.logLikelihoodClassification(sr1, srs, 0.05),
                qcMetrics.entropyClassification(sr1, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr2, srs, 0.05),
                qcMetrics.entropyClassification(sr2, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr3, srs, 0.05),
                qcMetrics.entropyClassification(sr3, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr4, srs, 0.05),
                qcMetrics.entropyClassification(sr4, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr5, srs, 0.05),
                qcMetrics.entropyClassification(sr5, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr6, srs, 0.05),
                qcMetrics.entropyClassification(sr6, srs, 0.05)
        ));
        srs.add(sr7);
        qcMetrics.makeProbabilities(srs);
        ll1 = qcMetrics.getLLForResponse(sr1);
        ent1 = qcMetrics.getEntropyForResponse(sr1);
        ll2 = qcMetrics.getLLForResponse(sr2);
        ent2 = qcMetrics.getEntropyForResponse(sr2);
        ll3 = qcMetrics.getLLForResponse(sr3);
        ent3 = qcMetrics.getEntropyForResponse(sr3);
        ll4 = qcMetrics.getLLForResponse(sr4);
        ent4 = qcMetrics.getEntropyForResponse(sr4);
        ll5 = qcMetrics.getLLForResponse(sr5);
        ent5 = qcMetrics.getEntropyForResponse(sr5);
        ll6 = qcMetrics.getLLForResponse(sr6);
        ent6 = qcMetrics.getEntropyForResponse(sr6);
        double ll7 = qcMetrics.getLLForResponse(sr7);
        double ent7 = qcMetrics.getEntropyForResponse(sr7);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f\n" +
                "\tUnif ll:\t%f\tUnif ent:\t%f\n" +
                "\tPos 1 ll:\t%f\tPos 1 ent:\t%f\n" +
                "\tFourth ll:\t%f\tFourth ent:\t%f\n" +
                "\tFifth ll:\t%f\tFifth ent:\t%f",
                ll1, ent1, ll2, ent2, ll3, ent3, ll4, ent4, ll5, ent5, ll6, ent6, ll7, ent7));
        LOGGER.debug(String.format("\n\tFirst LL bot?:\t%b\tFirst ent bot?:\t%b\n" +
                "\tSecond LL bot?:\t%b\tSecond ent bot?:%b\n" +
                "\tThird LL bot?:\t%b\tThird ent bot?:\t%b\n" +
                "\tUnif LL bot?:\t%b\tUnif ent bot?:\t%b\n" +
                "\tPos 1 LL bot?:\t%b\tPos 1 ent bot?:\t%b\n" +
                "\tFourth LL bot?:\t%b\tFourth ent bot?:\t%b\n" +
                "\tFifth LL bot?:\t%b\tFifth ent bot?:\t%b",
                qcMetrics.logLikelihoodClassification(sr1, srs, 0.05),
                qcMetrics.entropyClassification( sr1, srs, 0.05),
                qcMetrics.logLikelihoodClassification( sr2, srs, 0.05),
                qcMetrics.entropyClassification( sr2, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr3, srs, 0.05),
                qcMetrics.entropyClassification( sr3, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr4, srs, 0.05),
                qcMetrics.entropyClassification( sr4, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr5, srs, 0.05),
                qcMetrics.entropyClassification( sr5, srs,  0.05),
                qcMetrics.logLikelihoodClassification(sr6, srs, 0.05),
                qcMetrics.entropyClassification( sr6, srs, 0.05),
                qcMetrics.logLikelihoodClassification(sr7, srs, 0.05),
                qcMetrics.entropyClassification(sr7, srs, 0.05)
        ));
    }

    @Test
    public void testSortByData()
    {
        List<SurveyDatum> surveyDatumList = new ArrayList<SurveyDatum>();
        SurveyDatum fdsa = new StringDatum("fdsa", 1, 1, 0),
                  asdf = new StringDatum("asdf", 2, 1, 1),
                  pstuff = new HTMLDatum("<p>stuff</p>");
        surveyDatumList.add(pstuff);
        surveyDatumList.add(fdsa);
        surveyDatumList.add(asdf);
        LexicographicRespondent.sortByData(surveyDatumList);
        Assert.assertEquals(pstuff, surveyDatumList.get(0));
        Assert.assertEquals(asdf, surveyDatumList.get(1));
        Assert.assertEquals(fdsa, surveyDatumList.get(2));
    }

    @Test
    public void testLexicographicRespondent()
            throws SurveyException
    {
        Question q1 = new Question("q1");
        Question q2 = new Question("q2");
        Question q3 = new Question("q3");
        q1.addOptions("a", "b", "c");
        q2.addOptions("b", "c", "a");
        q3.addOptions("d", "aa", "a");
        Survey s = new Survey(q1, q2, q3);
        AbstractRespondent abstractRespondent = new LexicographicRespondent(s);
        SurveyResponse SurveyResponse = abstractRespondent.getResponse();
        Assert.assertTrue(SurveyResponse.getResponseForQuestion(q1).getOpts().get(0).c.dataEquals("a"));
        Assert.assertTrue(SurveyResponse.getResponseForQuestion(q2).getOpts().get(0).c.dataEquals("a"));
        Assert.assertTrue(SurveyResponse.getResponseForQuestion(q3).getOpts().get(0).c.dataEquals("a"));
    }
}

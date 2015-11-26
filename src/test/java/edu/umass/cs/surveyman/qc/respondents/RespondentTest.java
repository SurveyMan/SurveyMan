package edu.umass.cs.surveyman.qc.respondents;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.qc.AnswerProbabilityMap;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.qc.classifiers.AbstractClassifier;
import edu.umass.cs.surveyman.qc.classifiers.EntropyClassifier;
import edu.umass.cs.surveyman.qc.classifiers.LogLikelihoodClassifier;
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
        List<SurveyResponse> srs = new ArrayList<>();
        srs.add(sr1);
        srs.add(sr2);
        srs.add(sr3);
        LOGGER.info("Added 3 profiled responses to the list of responses.");
        AbstractClassifier logLikelihoodClassifier = new LogLikelihoodClassifier(survey, false, 0.05, 2);
        AbstractClassifier entropyClassifier = new EntropyClassifier(survey, false, 0.05, 2);
        logLikelihoodClassifier.makeProbabilities(srs);
        entropyClassifier.makeProbabilities(srs);
        double ll1 = logLikelihoodClassifier.getScoreForResponse(sr1);
        double ent1 = entropyClassifier.getScoreForResponse(sr1);
        double ll2 = logLikelihoodClassifier.getScoreForResponse(sr2);
        double ent2 = entropyClassifier.getScoreForResponse(sr2);
        double ll3 = logLikelihoodClassifier.getScoreForResponse(sr3);
        double ent3 = entropyClassifier.getScoreForResponse(sr3);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f",
                ll1, ent1, ll2, ent2, ll3, ent3)
        );
        LOGGER.debug(String.format("\n\tFirst ent valid?:\t%b\tFirst LL valid?:\t%b\n" +
                "\n\tSecond ent valid?:%b\tSecond LL valid?:\t%b\n" +
                "\n\tThird ent valid?:\t%b\tThird LL valid?:\t%b\n",
                entropyClassifier.classifyResponse(sr1),
                logLikelihoodClassifier.classifyResponse(sr1),
                entropyClassifier.classifyResponse(sr2),
                logLikelihoodClassifier.classifyResponse(sr2),
                entropyClassifier.classifyResponse(sr3),
                logLikelihoodClassifier.classifyResponse(sr3))
        );
        LOGGER.debug("Adding a uniform responder.");
        srs.add(sr4);
        logLikelihoodClassifier.makeProbabilities(srs);
        entropyClassifier.makeProbabilities(srs);
        ll1 = logLikelihoodClassifier.getScoreForResponse(sr1);
        ent1 = entropyClassifier.getScoreForResponse(sr1);
        ll2 = logLikelihoodClassifier.getScoreForResponse(sr2);
        ent2 = entropyClassifier.getScoreForResponse(sr2);
        ll3 = logLikelihoodClassifier.getScoreForResponse(sr3);
        ent3 = entropyClassifier.getScoreForResponse(sr3);
        double ll4 = logLikelihoodClassifier.getScoreForResponse(sr4);
        double ent4 = entropyClassifier.getScoreForResponse(sr4);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f\n" +
                "\tUnif ll:\t%f\tUnif ent:\t%f\n",
                ll1, ent1, ll2, ent2, ll3, ent3, ll4, ent4));
        LOGGER.debug(String.format("\n\tFirst ent valid?:\t%b\tFirst LL valid?:\t%b\n" +
                "\tSecond ent valid?:%b\tSecond LL valid?:\t%b\n" +
                "\tThird ent valid?:\t%b\tThird LL valid?:\t%b\n" +
                "\tUnif ent valid?:\t%b\tUnif LL valid?:\t%b\n",
                entropyClassifier.classifyResponse(sr1),
                logLikelihoodClassifier.classifyResponse(sr1),
                entropyClassifier.classifyResponse(sr2),
                logLikelihoodClassifier.classifyResponse(sr2),
                entropyClassifier.classifyResponse(sr3),
                logLikelihoodClassifier.classifyResponse(sr3),
                entropyClassifier.classifyResponse(sr4),
                logLikelihoodClassifier.classifyResponse(sr4)));
        LOGGER.debug("Adding positional preference.");
        srs.add(sr5);
        logLikelihoodClassifier.makeProbabilities(srs);
        entropyClassifier.makeProbabilities(srs);
        ll1 = logLikelihoodClassifier.getScoreForResponse(sr1);
        ent1 = entropyClassifier.getScoreForResponse(sr1);
        ll2 = logLikelihoodClassifier.getScoreForResponse(sr2);
        ent2 = entropyClassifier.getScoreForResponse(sr2);
        ll3 = logLikelihoodClassifier.getScoreForResponse(sr3);
        ent3 = entropyClassifier.getScoreForResponse(sr3);
        ll4 = logLikelihoodClassifier.getScoreForResponse(sr4);
        ent4 = entropyClassifier.getScoreForResponse(sr4);
        double ll5 = logLikelihoodClassifier.getScoreForResponse(sr5);
        double ent5 = entropyClassifier.getScoreForResponse(sr5);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f\n" +
                "\tUnif ll:\t%f\tUnif ent:\t%f\n" +
                "\tPos 1 ll:\t%f\tPos 1 ent:\t%f\n",
                ll1, ent1, ll2, ent2, ll3, ent3, ll4, ent4, ll5, ent5));
        LOGGER.debug(String.format("\n\tFirst ent valid?:\t%b\tFirst LL valid?:\t%b\n" +
                "\tSecond ent valid?:%b\tSecond LL valid?:\t%b\n" +
                "\tThird ent valid?:\t%b\tThird LL valid?:\t%b\n" +
                "\tUnif ent valid?:\t%b\tUnif LL valid?:\t%b\n" +
                "\tPos 1 ent valid?:\t%b\tPos 1 LL valid?\t%b\b",
                entropyClassifier.classifyResponse(sr1),
                logLikelihoodClassifier.classifyResponse(sr1),
                entropyClassifier.classifyResponse(sr2),
                logLikelihoodClassifier.classifyResponse(sr2),
                entropyClassifier.classifyResponse(sr3),
                logLikelihoodClassifier.classifyResponse(sr3),
                entropyClassifier.classifyResponse(sr4),
                logLikelihoodClassifier.classifyResponse(sr4),
                entropyClassifier.classifyResponse(sr5),
                logLikelihoodClassifier.classifyResponse(sr5)
        ));
        srs.add(sr6);
        logLikelihoodClassifier.makeProbabilities(srs);
        entropyClassifier.makeProbabilities(srs);
        ll1 = logLikelihoodClassifier.getScoreForResponse(sr1);
        ent1 = entropyClassifier.getScoreForResponse(sr1);
        ll2 = logLikelihoodClassifier.getScoreForResponse(sr2);
        ent2 = entropyClassifier.getScoreForResponse(sr2);
        ll3 = logLikelihoodClassifier.getScoreForResponse(sr3);
        ent3 = entropyClassifier.getScoreForResponse(sr3);
        ll4 = logLikelihoodClassifier.getScoreForResponse(sr4);
        ent4 = entropyClassifier.getScoreForResponse(sr4);
        ll5 = logLikelihoodClassifier.getScoreForResponse(sr5);
        ent5 = entropyClassifier.getScoreForResponse(sr5);
        double ll6 = logLikelihoodClassifier.getScoreForResponse(sr6);
        double ent6 = entropyClassifier.getScoreForResponse(sr6);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f\n" +
                "\tUnif ll:\t%f\tUnif ent:\t%f\n" +
                "\tPos 1 ll:\t%f\tPos 1 ent:\t%f\n" +
                "\tFourth ll:\t%f\tFourth ent:\t%f",
                ll1, ent1, ll2, ent2, ll3, ent3, ll4, ent4, ll5, ent5, ll6, ent6));
        LOGGER.debug(String.format("\n\tFirst LL valid?:\t%b\tFirst ent valid?:\t%b\n" +
                "\tSecond LL valid?:\t%b\tSecond ent valid?:%b\n" +
                "\tThird LL valid?:\t%b\tThird ent valid?:\t%b\n" +
                "\tUnif LL valid?:\t%b\tUnif ent valid?:\t%b\n" +
                "\tPos 1 LL valid?:\t%b\tPos 1 ent valid?:\t%b\n" +
                "\tFourth LL valid?:\t%b\tFourth ent valid?:\t%b\n",
                logLikelihoodClassifier.classifyResponse(sr1),
                entropyClassifier.classifyResponse(sr1),
                logLikelihoodClassifier.classifyResponse(sr2),
                entropyClassifier.classifyResponse(sr2),
                logLikelihoodClassifier.classifyResponse(sr3),
                entropyClassifier.classifyResponse(sr3),
                logLikelihoodClassifier.classifyResponse(sr4),
                entropyClassifier.classifyResponse(sr4),
                logLikelihoodClassifier.classifyResponse(sr5),
                entropyClassifier.classifyResponse(sr5),
                logLikelihoodClassifier.classifyResponse(sr6),
                entropyClassifier.classifyResponse(sr6)));
        srs.add(sr7);
        logLikelihoodClassifier.makeProbabilities(srs);
        entropyClassifier.makeProbabilities(srs);
        ll1 = logLikelihoodClassifier.getScoreForResponse(sr1);
        ent1 = entropyClassifier.getScoreForResponse(sr1);
        ll2 = logLikelihoodClassifier.getScoreForResponse(sr2);
        ent2 = entropyClassifier.getScoreForResponse(sr2);
        ll3 = logLikelihoodClassifier.getScoreForResponse(sr3);
        ent3 = entropyClassifier.getScoreForResponse(sr3);
        ll4 = logLikelihoodClassifier.getScoreForResponse(sr4);
        ent4 = entropyClassifier.getScoreForResponse(sr4);
        ll5 = logLikelihoodClassifier.getScoreForResponse(sr5);
        ent5 = entropyClassifier.getScoreForResponse(sr5);
        ll6 = logLikelihoodClassifier.getScoreForResponse(sr6);
        ent6 = entropyClassifier.getScoreForResponse(sr6);
        double ll7 = logLikelihoodClassifier.getScoreForResponse(sr7);
        double ent7 = entropyClassifier.getScoreForResponse(sr7);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f\n" +
                "\tUnif ll:\t%f\tUnif ent:\t%f\n" +
                "\tPos 1 ll:\t%f\tPos 1 ent:\t%f\n" +
                "\tFourth ll:\t%f\tFourth ent:\t%f\n" +
                "\tFifth ll:\t%f\tFifth ent:\t%f",
                ll1, ent1, ll2, ent2, ll3, ent3, ll4, ent4, ll5, ent5, ll6, ent6, ll7, ent7));
        LOGGER.debug(String.format("\n\tFirst LL valid?:\t%b\tFirst ent valid?:\t%b\n" +
                "\tSecond LL valid?:\t%b\tSecond ent valid?:%b\n" +
                "\tThird LL valid?:\t%b\tThird ent valid?:\t%b\n" +
                "\tUnif LL valid?:\t%b\tUnif ent valid?:\t%b\n" +
                "\tPos 1 LL valid?:\t%b\tPos 1 ent valid?:\t%b\n" +
                "\tFourth LL valid?:\t%b\tFourth ent valid?:\t%b\n" +
                "\tFifth LL valid?:\t%b\tFifth ent valid?:\t%b",
                logLikelihoodClassifier.classifyResponse(sr1),
                entropyClassifier.classifyResponse(sr1),
                logLikelihoodClassifier.classifyResponse(sr2),
                entropyClassifier.classifyResponse(sr2),
                logLikelihoodClassifier.classifyResponse(sr3),
                entropyClassifier.classifyResponse(sr3),
                logLikelihoodClassifier.classifyResponse(sr4),
                entropyClassifier.classifyResponse(sr4),
                logLikelihoodClassifier.classifyResponse(sr5),
                entropyClassifier.classifyResponse(sr5),
                logLikelihoodClassifier.classifyResponse(sr6),
                entropyClassifier.classifyResponse(sr6),
                logLikelihoodClassifier.classifyResponse(sr7),
                entropyClassifier.classifyResponse(sr7)));
    }

    @Test
    public void testSortByData() {
        List<SurveyDatum> surveyDatumList = new ArrayList<>();
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

package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.analyses.ISurveyResponse;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.survey.Survey;
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
            throws InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException, IOException {
        // assert that for each survey, this adversary chooses the position at random
        LOGGER.info("Executing testUniformAdversary.");
        for (int i = 0 ; i < super.testsFiles.length ; i ++) {
            try {
                Survey survey = new CSVParser(new CSVLexer(super.testsFiles[i], String.valueOf(super.separators[i]))).parse();
                RandomRespondent randomRespondent = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
                ISurveyResponse surveyResponse = randomRespondent.getResponse();
                // assert that we don't deviate more than what's expected
                double posPref  =   0.0,
                       eps      =   Math.pow((surveyResponse.getResponses().size() * Math.log(0.05)) / - 2.0, 0.5),
                       mean     =   0.5;
                assert surveyResponse.getResponses().size() > 0 : String.format("Survey response (%s) is empty for survey %s"
                        , surveyResponse.getSrid(), survey.sourceName);
                for (IQuestionResponse qr : surveyResponse.getResponses()) {
//                    System.out.println(qr.getQuestion() + " " + posPref + " " + qr.getIndexSeen());
                    if (qr.getIndexSeen() > -1 && qr.getQuestion().getOptListByIndex().length > 1)
                        posPref += ((double) (qr.getIndexSeen() + 1)) / (double) randomRespondent.getDenominator(qr.getQuestion());
                    else LOGGER.warn(String.format("Question %s has index %d with opt list size %d"
                            , qr.getQuestion().quid
                            , qr.getIndexSeen()
                            , qr.getOpts().size()));
                }
                posPref = posPref / surveyResponse.getResponses().size();
                LOGGER.info(String.format("posPref : %f\teps : %f", posPref, eps));
                assert between(mean + eps, mean - eps, posPref) :
                        String.format("Position preference (%f) deviates too far from the mean (%f, with eps %f) in survey %s for the uniform adversary"
                                    , posPref, mean, eps, survey.sourceName);
            } catch (SurveyException se) {
                System.out.println(String.format("Were we expecting survey %s to succeed? %b", super.testsFiles[i], super.outcome[i]));
                if (super.outcome[i])
                    throw se;
            } catch (NullPointerException npe) {
                System.out.println(String.format("Were we expecting survey %s to succeed? %b", super.testsFiles[i], super.outcome[i]));
                if (super.outcome[i])
                    throw npe;
                else System.out.println("THIS NEEDS TO FAIL GRACEFULLY");
            }
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
        for (Map.Entry<Question, Component> entry : profile.answers.entrySet()) {
            double strength = profile.strength.get(entry.getValue());
            LOGGER.debug(String.format("%s\t%s\t%f",
                    entry.getKey().quid,
                    entry.getValue().getCid(),
                    strength)
            );
        }
        ISurveyResponse sr1 = profile.getResponse();
        ISurveyResponse sr2 = profile.getResponse();
        ISurveyResponse sr3 = profile.getResponse();
        assert sr1 != null : String.format("getResponse on the profile for %s is not working.", profile.getClass().getName());
        LOGGER.debug("Actual responses:");
        for (IQuestionResponse qr1 : sr1.getResponses()) {
            IQuestionResponse qr2 = sr2.resultsAsMap().get(qr1.getQuestion().quid);
            IQuestionResponse qr3 = sr3.resultsAsMap().get(qr1.getQuestion().quid);
            StringBuilder sb1 = new StringBuilder();
            StringBuilder sb2 = new StringBuilder();
            StringBuilder sb3 = new StringBuilder();
            for (OptTuple optTuple : qr1.getOpts())
                sb1.append(optTuple.c.getCid());
            for (OptTuple optTuple : qr2.getOpts())
                sb2.append(optTuple.c.getCid());
            for (OptTuple optTuple : qr3.getOpts())
                sb3.append(optTuple.c.getCid());
            LOGGER.debug(String.format("%s\t%s\t%s\t%s",
                    qr1.getQuestion().quid,
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
        ISurveyResponse sr1 = profile.getResponse();
        ISurveyResponse sr2 = profile.getResponse();
        ISurveyResponse sr3 = profile.getResponse();
        ISurveyResponse sr4 = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM).getResponse();
        ISurveyResponse sr5 = new RandomRespondent(survey, RandomRespondent.AdversaryType.FIRST).getResponse();
        ISurveyResponse sr6 = profile.getResponse();
        ISurveyResponse sr7 = profile.getResponse();
        LOGGER.info("Generated 4 profiled responses, 1 uniform response, 1 first position preference");
        List<ISurveyResponse> srs = new ArrayList<ISurveyResponse>();
        srs.add(sr1);
        srs.add(sr2);
        srs.add(sr3);
        LOGGER.info("Added 3 profiled responses to the list of responses.");
        Map<String, Map<String, Double>> probs = QCMetrics.makeProbabilities(QCMetrics.makeFrequencies(srs));
        double ll1 = QCMetrics.getLLForResponse(sr1, probs);
        double ent1 = QCMetrics.getEntropyForResponse(sr1, probs);
        double ll2 = QCMetrics.getLLForResponse(sr2, probs);
        double ent2 = QCMetrics.getEntropyForResponse(sr2, probs);
        double ll3 = QCMetrics.getLLForResponse(sr3, probs);
        double ent3 = QCMetrics.getEntropyForResponse(sr3, probs);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f",
                ll1, ent1, ll2, ent2, ll3, ent3)
        );
        LOGGER.debug(String.format("\n\tFirst ent bot?:\t%b\tFirst LL bot?:\t%b\n" +
                "\n\tSecond ent bot?:%b\tSecond LL bot?:\t%b\n" +
                "\n\tThird ent bot?:\t%b\tThird LL bot?:\t%b\n",
                QCMetrics.entropyClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr3, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr3, srs, false, 0.05))
        );
        LOGGER.debug("Adding a uniform responder.");
        srs.add(sr4);
        probs = QCMetrics.makeProbabilities(QCMetrics.makeFrequencies(srs));
        ll1 = QCMetrics.getLLForResponse(sr1, probs);
        ent1 = QCMetrics.getEntropyForResponse(sr1, probs);
        ll2 = QCMetrics.getLLForResponse(sr2, probs);
        ent2 = QCMetrics.getEntropyForResponse(sr2, probs);
        ll3 = QCMetrics.getLLForResponse(sr3, probs);
        ent3 = QCMetrics.getEntropyForResponse(sr3, probs);
        double ll4 = QCMetrics.getLLForResponse(sr4, probs);
        double ent4 = QCMetrics.getEntropyForResponse(sr4, probs);
        LOGGER.debug(String.format("\n\tFirst ll:\t%f\tFirst ent:\t%f\n" +
                "\tSecond ll:\t%f\tSecond ent:\t%f\n" +
                "\tThird ll:\t%f\tThird ent:\t%f\n" +
                "\tUnif ll:\t%f\tUnif ent:\t%f\n",
                ll1, ent1, ll2, ent2, ll3, ent3, ll4, ent4));
        LOGGER.debug(String.format("\n\tFirst ent bot?:\t%b\tFirst LL bot?:\t%b\n" +
                "\tSecond ent bot?:%b\tSecond LL bot?:\t%b\n" +
                "\tThird ent bot?:\t%b\tThird LL bot?:\t%b\n" +
                "\tUnif ent bot?:\t%b\tUnif LL bot?:\t%b\n",
                QCMetrics.entropyClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr3, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr3, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr4, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr4, srs, false, 0.05))
        );
        LOGGER.debug("Adding positional preference.");
        srs.add(sr5);
        probs = QCMetrics.makeProbabilities(QCMetrics.makeFrequencies(srs));
        ll1 = QCMetrics.getLLForResponse(sr1, probs);
        ent1 = QCMetrics.getEntropyForResponse(sr1, probs);
        ll2 = QCMetrics.getLLForResponse(sr2, probs);
        ent2 = QCMetrics.getEntropyForResponse(sr2, probs);
        ll3 = QCMetrics.getLLForResponse(sr3, probs);
        ent3 = QCMetrics.getEntropyForResponse(sr3, probs);
        ll4 = QCMetrics.getLLForResponse(sr4, probs);
        ent4 = QCMetrics.getEntropyForResponse(sr4, probs);
        double ll5 = QCMetrics.getLLForResponse(sr5, probs);
        double ent5 = QCMetrics.getEntropyForResponse(sr5, probs);
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
                QCMetrics.entropyClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr3, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr3, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr4, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr4, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr5, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr5, srs, false, 0.05)
        ));
        srs.add(sr6);
        probs = QCMetrics.makeProbabilities(QCMetrics.makeFrequencies(srs));
        ll1 = QCMetrics.getLLForResponse(sr1, probs);
        ent1 = QCMetrics.getEntropyForResponse(sr1, probs);
        ll2 = QCMetrics.getLLForResponse(sr2, probs);
        ent2 = QCMetrics.getEntropyForResponse(sr2, probs);
        ll3 = QCMetrics.getLLForResponse(sr3, probs);
        ent3 = QCMetrics.getEntropyForResponse(sr3, probs);
        ll4 = QCMetrics.getLLForResponse(sr4, probs);
        ent4 = QCMetrics.getEntropyForResponse(sr4, probs);
        ll5 = QCMetrics.getLLForResponse(sr5, probs);
        ent5 = QCMetrics.getEntropyForResponse(sr5, probs);
        double ll6 = QCMetrics.getLLForResponse(sr6, probs);
        double ent6 = QCMetrics.getEntropyForResponse(sr6, probs);
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
                QCMetrics.logLikelihoodClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr3, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr3, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr4, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr4, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr5, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr5, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr6, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr6, srs, false, 0.05)
        ));
        srs.add(sr7);
        probs = QCMetrics.makeProbabilities(QCMetrics.makeFrequencies(srs));
        ll1 = QCMetrics.getLLForResponse(sr1, probs);
        ent1 = QCMetrics.getEntropyForResponse(sr1, probs);
        ll2 = QCMetrics.getLLForResponse(sr2, probs);
        ent2 = QCMetrics.getEntropyForResponse(sr2, probs);
        ll3 = QCMetrics.getLLForResponse(sr3, probs);
        ent3 = QCMetrics.getEntropyForResponse(sr3, probs);
        ll4 = QCMetrics.getLLForResponse(sr4, probs);
        ent4 = QCMetrics.getEntropyForResponse(sr4, probs);
        ll5 = QCMetrics.getLLForResponse(sr5, probs);
        ent5 = QCMetrics.getEntropyForResponse(sr5, probs);
        ll6 = QCMetrics.getLLForResponse(sr6, probs);
        ent6 = QCMetrics.getEntropyForResponse(sr6, probs);
        double ll7 = QCMetrics.getLLForResponse(sr7, probs);
        double ent7 = QCMetrics.getEntropyForResponse(sr7, probs);
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
                QCMetrics.logLikelihoodClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr1, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr2, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr3, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr3, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr4, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr4, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr5, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr5, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr6, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr6, srs, false, 0.05),
                QCMetrics.logLikelihoodClassification(survey, sr7, srs, false, 0.05),
                QCMetrics.entropyClassification(survey, sr7, srs, false, 0.05)
        ));
    }
}

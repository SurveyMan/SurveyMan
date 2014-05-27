import input.csv.CSVLexer;
import input.csv.CSVParser;
import input.exceptions.SyntaxException;
import interstitial.ISurveyResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import interstitial.IQuestionResponse;
import qc.RandomRespondent;
import survey.Survey;
import survey.exceptions.SurveyException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@RunWith(JUnit4.class)
public class RandomRespondentTest extends TestLog {

    public RandomRespondentTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    private boolean between(double upper, double lower, double i) {
        return i < upper && i > lower;
    }

    @Test
    public void testUniformAdversary()
            throws InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException, IOException {
        // assert that for each survey, this adversary chooses the position at random
        for (int i = 0 ; i < super.testsFiles.length ; i ++) {
            try {
                Survey survey = new CSVParser(new CSVLexer(super.testsFiles[i], String.valueOf(super.separators[i]))).parse();
                RandomRespondent randomRespondent = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
                ISurveyResponse surveyResponse = randomRespondent.response;
                // assert that we don't deviate more than what's expected
                double posPref  =   0.0,
                       eps      =   Math.pow((surveyResponse.getResponses().size() * Math.log(0.05)) / - 2.0, 0.5),
                       mean     =   0.5;
                assert surveyResponse.getResponses().size() > 0 : String.format("Survey response (%s) is empty for survey %s"
                        , surveyResponse.srid(), survey.sourceName);
                for (IQuestionResponse qr : surveyResponse.getResponses()) {
                    System.out.println(qr.getQuestion() + " " + posPref + " " + qr.getIndexSeen());
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
}

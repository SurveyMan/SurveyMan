import csv.CSVLexer;
import csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import qc.RandomRespondent;
import survey.Survey;
import survey.SurveyException;
import survey.SurveyResponse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@RunWith(JUnit4.class)
public class RandomRespondentTest extends TestLog {

    public RandomRespondentTest() {
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
            Survey survey = new CSVParser(new CSVLexer(super.testsFiles[i], String.valueOf(super.separators[i]))).parse();
            RandomRespondent randomRespondent = new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM);
            SurveyResponse surveyResponse = randomRespondent.response;
            // assert that we don't deviate more than what's expected
            double posPref  =   0.0,
                   eps      =   Math.pow((surveyResponse.responses.size() * Math.log(0.05)) / - 2.0, 0.5),
                   mean     =   0.5;
            assert surveyResponse.responses.size() > 0 : String.format("Survey response (%s) is empty for survey %s"
                    , surveyResponse.srid, survey.sourceName);
            for (SurveyResponse.QuestionResponse qr : surveyResponse.responses) {
                System.out.println(qr.q + " " + posPref + " " + qr.indexSeen);
                if (qr.indexSeen > -1 && qr.q.getOptListByIndex().length > 0)
                    posPref += ((double) (qr.indexSeen + 1)) / (double) randomRespondent.getDenominator(qr.q);
                else LOGGER.warn(String.format("Question %s has index %d with opt list size %d", qr.q.quid, qr.indexSeen, qr.opts.size()));
            }
            posPref = posPref / surveyResponse.responses.size();
            LOGGER.info(String.format("posPref : %f\teps : %f", posPref, eps));
            assert between(mean + eps, mean - eps, posPref) : "position preference deviates too far from the mean for the uniform adversary";
        }
    }
}

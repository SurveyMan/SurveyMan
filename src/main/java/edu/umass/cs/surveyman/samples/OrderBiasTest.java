package edu.umass.cs.surveyman.samples;


import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.DynamicAnalysis;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.logging.log4j.Level;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by etosch on 10/24/15.
 */
public class OrderBiasTest {

    private static Survey survey;

    private static Survey makeSurvey() throws SurveyException {
        Question[] questions = new Question[26];
        Question.makeUnorderedRadioQuestions(questions, "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");
        for (Question q: questions) {
            q.addOption("first");
            q.addOption("second");
            q.addOption("third");
            q.addOption("fourth");
            q.addOption("fifth");
        }
        return new Survey(questions);
    }

    private static void experiment1() throws SurveyException {
        // all uniformly random
        // 10 questions == 100 comparisons. 5 should be significant.
        List<DynamicAnalysis.DynamicSurveyResponse> responseList = new ArrayList<>();
        for (int i = 1000; i > 0; i--) {
            responseList.add(new DynamicAnalysis.DynamicSurveyResponse(new RandomRespondent(survey, RandomRespondent.AdversaryType.UNIFORM).getResponse()));
        }
        DynamicAnalysis.Report report = DynamicAnalysis.dynamicAnalysis(survey, responseList, Classifier.ALL, false, 0.05, 2);
        try {
            FileOutputStream out = new FileOutputStream("OrderBiasTests.txt");
            report.print(out);
            out.close();
        } catch (IOException io) {
            SurveyMan.LOGGER.log(Level.WARN, io.getMessage());
        }
    }

    private static void experiment2() {

    }

    private static void experiment3() {

    }

    public static void main(String[] args) throws SurveyException {
        survey = makeSurvey();
        experiment1();
        experiment2();
        experiment3();
    }
}

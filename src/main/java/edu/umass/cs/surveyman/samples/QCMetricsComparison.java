package edu.umass.cs.surveyman.samples;

import edu.umass.cs.surveyman.analyses.AbstractSurveyResponse;
import edu.umass.cs.surveyman.analyses.Simulation;
import edu.umass.cs.surveyman.qc.*;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compares quality control metrics for the machine learning final project.
 */
public class QCMetricsComparison {

    public static void generateFeaturesForExperiment1(List<AbstractSurveyResponse> responses) {
        // Treat features as if they are drawn independently and there are no interactions

    }

    public static void experiment0(Survey survey,
                                   List<AbstractSurveyResponse> surveyRespondents) throws SurveyException
    {
        Simulation.ROC entropyROC = Simulation.analyze(survey, surveyRespondents, Classifier.ENTROPY, 0.05);
        Simulation.ROC llROC = Simulation.analyze(survey, surveyRespondents, Classifier.LOG_LIKELIHOOD, 0.05);
        OutputStreamWriter osw = null;
        try {
            osw = new OutputStreamWriter(new FileOutputStream("output/experiment0_entropyROC"));
            osw.write(entropyROC.toString());
            osw.close();
            osw = new OutputStreamWriter(new FileOutputStream("output/experiment0_llROC"));
            osw.write(llROC.toString());
            osw.close();
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    public static void experiment1(Survey survey,
                                   List<AbstractSurveyResponse> honestRespondents,
                                   List<AbstractSurveyResponse> badActors) {
        // TODO(cibelemf): Implement experiment 1.
        // get features
        // learn separating hyperplane between honest respondents and bad actors

    }

    public static void experiment2(Survey survey,
                                   List<AbstractSurveyResponse> honestRespondents,
                                   List<AbstractSurveyResponse> badActors) {
        // TODO(etosch): Implement experiment 2.
    }

    public static void main(String[] args) throws SurveyException {
        // Generate surveys of varying size
        Survey survey1 = new Survey();
        for (char c : "ABCDEFGHIJ".toCharArray())
            survey1.addQuestion(new Question(Character.toString(c)));
        for (Question q : survey1.questions)
            for (char c : "abcde".toCharArray())
                q.addOption(Character.toString(c));
        LexicographicRespondent lexicographicRespondent = new LexicographicRespondent(survey1);
        NonRandomRespondent nonRandomRespondent = new NonRandomRespondent(survey1);
        RandomRespondent randomRespondent = new RandomRespondent(survey1, RandomRespondent.AdversaryType.UNIFORM);
        List<AbstractSurveyResponse> lexicographicSurveyResponses = new ArrayList<AbstractSurveyResponse>();
        List<AbstractSurveyResponse> nonRandomSurveyResponses = new ArrayList<AbstractSurveyResponse>();
        List<AbstractSurveyResponse> randomSurveyResponses = new ArrayList<AbstractSurveyResponse>();
        for (int i=0; i < 1000; i++) {
            lexicographicSurveyResponses.add(lexicographicRespondent.getResponse());
            nonRandomSurveyResponses.add(nonRandomRespondent.getResponse());
            randomSurveyResponses.add(randomRespondent.getResponse());
        }
        Collections.shuffle(lexicographicSurveyResponses);
        Collections.shuffle(nonRandomSurveyResponses);
        Collections.shuffle(randomSurveyResponses);
        List<AbstractSurveyResponse> experiment0responses = new ArrayList<AbstractSurveyResponse>();
        experiment0responses.addAll(lexicographicSurveyResponses.subList(0, 800));
        experiment0responses.addAll(randomSurveyResponses.subList(0, 200));
        experiment0(survey1, experiment0responses);
        experiment1(survey1, lexicographicSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
        experiment2(survey1, nonRandomSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
    }
}

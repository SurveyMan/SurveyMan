package edu.umass.cs.surveyman.samples;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.Simulation;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.*;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Compares quality control metrics for the machine learning final project.
 */
public class QCMetricsComparison {

    public static float[][] generateFeaturesForExperiment1(Survey survey, List<? extends SurveyResponse> responses) {
        // Treat features as if they are drawn independently and there are no interactions
        int features = survey.questions.size();
        int numData = responses.size();
        System.out.println("Is is getting here?");
        System.out.println(features);
        System.out.println(numData);
        float[][] dataPoints = new float[features][numData];
        int count = 0;
        for (int j=0; j<numData; j++) {
            count = 0;
            //List<IQuestionResponse> q = responses.get(j).getAllResponses();
            //q.get(0).getQuestion().getOptListByIndex();
            for (IQuestionResponse q: responses.get(j).getAllResponses()) {
                dataPoints[count][j] = q.getOpts().get(0).i;
                count++;
            }
        }
        return dataPoints;

    }

    public static void experiment0(Survey survey,
                                   List<? extends SurveyResponse> surveyRespondents)
            throws SurveyException
    {
//        Simulation.ROC entropyROC = Simulation.analyze(survey, surveyRespondents, Classifier.ENTROPY, 0.05);
        Simulation.ROC llROC = Simulation.analyze(survey, surveyRespondents, Classifier.LOG_LIKELIHOOD, 0.05);
        OutputStreamWriter osw;
        try {
//            osw = new OutputStreamWriter(new FileOutputStream("output/experiment0_entropyROC"));
//            osw.write(entropyROC.toString());
//            osw.close();
            osw = new OutputStreamWriter(new FileOutputStream("output/experiment0_llROC"));
            osw.write(llROC.toString());
            osw.close();
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    public static void experiment1(Survey survey,
                                   List<? extends SurveyResponse> honestRespondents,
                                   List<? extends SurveyResponse> badActors) {
        // TODO(cibelemf): Implement experiment 1.
        // get features
        List<SurveyResponse> respondentAll = new ArrayList();
        respondentAll.addAll(honestRespondents);
        respondentAll.addAll(badActors);
        Collections.shuffle(respondentAll);
        float[][] x = generateFeaturesForExperiment1(survey, respondentAll);
        // learn separating hyperplane between honest respondents and bad actors
    }

    public static void experiment2(Survey survey,
                                   List<? extends SurveyResponse> honestRespondents,
                                   List<? extends SurveyResponse> badActors) {
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

        // Respondents
        LexicographicRespondent lexicographicRespondent = new LexicographicRespondent(survey1);
        NonRandomRespondent nonRandomRespondent = new NonRandomRespondent(survey1);
        RandomRespondent randomRespondent = new RandomRespondent(survey1, RandomRespondent.AdversaryType.UNIFORM);

        // Response pools
        List<SurveyResponse> lexicographicSurveyResponses = new ArrayList<SurveyResponse>();
        List<SurveyResponse> nonRandomSurveyResponses = new ArrayList<SurveyResponse>();
        List<SurveyResponse> randomSurveyResponses = new ArrayList<SurveyResponse>();

        for (int i=0; i < 1000; i++) {
            lexicographicSurveyResponses.add(lexicographicRespondent.copy().getResponse());
            nonRandomSurveyResponses.add(nonRandomRespondent.copy().getResponse());
            randomSurveyResponses.add(randomRespondent.copy().getResponse());
        }

        Collections.shuffle(lexicographicSurveyResponses);
        Collections.shuffle(nonRandomSurveyResponses);
        Collections.shuffle(randomSurveyResponses);

        List<SurveyResponse> experiment0responses = new ArrayList<SurveyResponse>();
        experiment0responses.addAll(lexicographicSurveyResponses.subList(0, 800));
        experiment0responses.addAll(randomSurveyResponses.subList(0, 200));
        //experiment0(survey1, experiment0responses);
        experiment1(survey1, lexicographicSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
        //experiment2(survey1, nonRandomSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
    }

}

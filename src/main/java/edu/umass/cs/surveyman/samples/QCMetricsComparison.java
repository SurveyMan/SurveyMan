package edu.umass.cs.surveyman.samples;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.Simulation;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.*;
import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.ml.clustering.CentroidCluster;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Compares quality control metrics for the machine learning final project.
 */
public class QCMetricsComparison {

    public static float[][] generateFeaturesForExperiment1(Survey survey, List<? extends SurveyResponse> responses) throws SurveyException {
        // Treat features as if they are drawn independently and there are no interactions
        int features = survey.questions.size();
        int numData = responses.size();
        System.out.println("Is is getting here?");
        System.out.println(features);
        System.out.println(numData);
        float[][] dataPoints = new float[features][numData];
        for (int j=0; j<numData; j++) {
            //List<IQuestionResponse> q = responses.get(j).getAllResponses();
            //q.get(0).getQuestion().getOptListByIndex();
            List<IQuestionResponse> qList = responses.get(j).getAllResponses();
            Collections.sort(qList);
            Question[] indexLookup = survey.getQuestionListByIndex();
            for (int i = 0; i < qList.size(); i++) {
                //System.out.println(qList.get(i).getAnswer());
                System.out.print("he");
            }
        }
        return dataPoints;

    }

    public static void experiment0(Survey survey,
                                   String filename,
                                   List<? extends SurveyResponse>... surveyRespondentsLists
                                   )
            throws SurveyException
    {
        List<SurveyResponse> surveyRespondents = new ArrayList<SurveyResponse>();
        for (List<? extends SurveyResponse> srs : surveyRespondentsLists)
            surveyRespondents.addAll(srs);
        Collections.shuffle(surveyRespondents);
//        Simulation.ROC entropyROC = Simulation.analyze(survey, surveyRespondents, Classifier.ENTROPY, 0.05);
        Simulation.ROC llROC = Simulation.analyze(survey, surveyRespondents, Classifier.LOG_LIKELIHOOD, 0.05);
        OutputStreamWriter osw;
        try {
//            osw = new OutputStreamWriter(new FileOutputStream("output/experiment0_entropyROC"));
//            osw.write("percBots,empiricalEntropy,truePositive,falsePositive,trueNegative,falseNegative\n");
//            osw.write(entropyROC.toString());
//            osw.close();
            osw = new OutputStreamWriter(new FileOutputStream(filename));
            osw.write("percBots,empiricalEntropy,truePositive,falsePositive,trueNegative,falseNegative\n");
            osw.write(llROC.toString());
            osw.close();
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    public static void experiment3(Survey survey,
                                   String filename,
                                   int clusters,
                                   List<? extends SurveyResponse>... responseLists)
            throws SurveyException
    {
        List<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        for (List<? extends SurveyResponse> srs: responseLists)
            responses.addAll(srs);
        Collections.shuffle(responses);
        Simulation.ROC rocs = Simulation.analyze(survey, responses, Classifier.CLUSTER, clusters);
        try {
            OutputStreamWriter osw;
            osw = new OutputStreamWriter(new FileOutputStream(filename));
            osw.write("percBots,empiricalEntropy,truePositive,falsePositive,trueNegative,falseNegative\n");
            osw.write(rocs.toString());
            osw.close();
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }

    public static void experiment4(Survey survey,
                                   String filename,
                                   List<? extends SurveyResponse>... responseLists)
        throws SurveyException
    {
        List<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        for (List<? extends SurveyResponse> srs: responseLists)
            responses.addAll(srs);
        Collections.shuffle(responses);
        // do PCA
        Simulation.ROC rocs = Simulation.analyze(survey, responses, Classifier.LINEAR, 0.0);
        try {
            OutputStreamWriter osw;
            osw = new OutputStreamWriter(new FileOutputStream(filename));
            osw.write("percBots,empiricalEntropy,truePositive,falsePositive,trueNegative,falseNegative\n");
            osw.write(rocs.toString());
            osw.close();
        } catch (IOException io) {
            throw new RuntimeException(io);
        }
    }


    public static void experiment1(Survey survey,
                                   List<? extends SurveyResponse> honestRespondents,
                                   List<? extends SurveyResponse> badActors) throws SurveyException {
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


    public static void dumpData(Survey survey,
                         List<? extends SurveyResponse> surveyResponses,
                         String clz,
                         boolean honestRespondent,
                         OutputStreamWriter osw)
            throws SurveyException, IOException
    {
        for (SurveyResponse surveyResponse : surveyResponses) {
            String srid = surveyResponse.getSrid();
            List<Question> questions = Arrays.asList(survey.getQuestionListByIndex());
            for (IQuestionResponse questionResponse : surveyResponse.getAllResponses()) {
                Question q = questionResponse.getQuestion();
                Component a = questionResponse.getAnswer();
                List<Component> as = Arrays.asList(q.getOptListByIndex());
                String[] vals = {srid, clz, Boolean.toString(honestRespondent),
                        q.quid,
                        Integer.toString(questions.indexOf(q)),
                        Integer.toString(questionResponse.getIndexSeen()),
                        a.getCid(),
                        Integer.toString(as.indexOf(a)),
                        Integer.toString(questionResponse.getIndexSeen())
                };
                osw.write(StringUtils.join(vals, ",") + "\n");
            }
        }
        osw.flush();
    }

    public static void main(String[] args) throws SurveyException, IOException
    {
        int experiment = 4;
        // Generate surveys of varying size
        Survey survey1 = new Survey();
        for (char c : "ABCDEFGHIJ".toCharArray())
            survey1.addQuestion(new Question(Character.toString(c)));
        for (Question q : survey1.questions)
            for (char c : "abcde".toCharArray())
                q.addOption(Character.toString(c));

        // Respondents
        LexicographicRespondent lexicographicRespondent = new LexicographicRespondent(survey1);
        NoisyLexicographicRespondent noisyLexicographicRespondent = new NoisyLexicographicRespondent(survey1, 0.01);
        NonRandomRespondent nonRandomRespondent = new NonRandomRespondent(survey1);
        RandomRespondent randomRespondent = new RandomRespondent(survey1, RandomRespondent.AdversaryType.UNIFORM);

        // Response pools
        List<SurveyResponse> lexicographicSurveyResponses = new ArrayList<SurveyResponse>();
        List<SurveyResponse> noisyLexicographicSurveyResponses = new ArrayList<SurveyResponse>();
        List<SurveyResponse> nonRandomSurveyResponses = new ArrayList<SurveyResponse>();
        List<SurveyResponse> randomSurveyResponses = new ArrayList<SurveyResponse>();

        for (int i = 0; i < 2000; i++) {
            lexicographicSurveyResponses.add(lexicographicRespondent.copy().getResponse());
            noisyLexicographicSurveyResponses.add(noisyLexicographicRespondent.copy().getResponse());
            nonRandomSurveyResponses.add(nonRandomRespondent.copy().getResponse());
            randomSurveyResponses.add(randomRespondent.copy().getResponse());
        }

        Collections.shuffle(lexicographicSurveyResponses);
        Collections.shuffle(noisyLexicographicSurveyResponses);
        Collections.shuffle(nonRandomSurveyResponses);
        Collections.shuffle(randomSurveyResponses);

//        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("survey_responses.csv"));
//        String[] headers = { "response_id", "class", "honest_respondent",
//                "question_id", "question_index", "question_index_seen",
//                "answer_id", "answer_index", "answer_index_seen"};
//        osw.write(StringUtils.join(headers, ",") + "\n");
//        dumpData(survey1, lexicographicSurveyResponses, "lexicographic", true, osw);
//        dumpData(survey1, nonRandomSurveyResponses, "profiled", true, osw);
//        dumpData(survey1, randomSurveyResponses, "random", false, osw);
//        osw.close();

        if (experiment == 0) {
            /**
             * lexicographic vs random
             */
            experiment0(survey1, "output/experiment0_20rand_80lexico", lexicographicSurveyResponses.subList(0, 800),
                    randomSurveyResponses.subList(0, 200));
            experiment0(survey1, "output/experiment0_40rand_60lexico", lexicographicSurveyResponses.subList(0, 600),
                    randomSurveyResponses.subList(0, 400));
            experiment0(survey1, "output/experiment0_60rand_40lexico", lexicographicSurveyResponses.subList(0, 400),
                    randomSurveyResponses.subList(0, 600));
            /**
             * noisy lexicographic vs random
             */
            experiment0(survey1, "output/experiment0_20rand_80noisy", noisyLexicographicSurveyResponses.subList(0, 800),
                    randomSurveyResponses.subList(0, 200));
            experiment0(survey1, "output/experiment0_40rand_60noisy", noisyLexicographicSurveyResponses.subList(0, 600),
                    randomSurveyResponses.subList(0, 400));
            experiment0(survey1, "output/experiment0_60rand_40noisy", lexicographicSurveyResponses.subList(0, 400),
                    randomSurveyResponses.subList(0, 600));
            /**
             * profiled resondent vs random
             */
            experiment0(survey1, "output/experiment0_20rand_80profiled", nonRandomSurveyResponses.subList(0, 800),
                    randomSurveyResponses.subList(0, 200));
            experiment0(survey1, "output/experiment0_40rand_60profiled", nonRandomSurveyResponses.subList(0, 600),
                    randomSurveyResponses.subList(0, 400));
            experiment0(survey1, "output/experiment0_60rand_20profiled", nonRandomSurveyResponses.subList(0, 400),
                    randomSurveyResponses.subList(0, 600));
        } else if (experiment == 3) {
            experiment3(survey1, "output/experiment3_20rand_80lexico_2clusters", 2, lexicographicSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
            experiment3(survey1, "output/experiment3_40rand_60lexico_2clusters", 2, lexicographicSurveyResponses.subList(0, 600), randomSurveyResponses.subList(0, 400));
            experiment3(survey1, "output/experiment3_60rand_40lexico_2clusters", 2, lexicographicSurveyResponses.subList(0, 400), randomSurveyResponses.subList(0, 600));

            experiment3(survey1, "output/experiment3_20rand_80profiled_2clusters", 2, nonRandomSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
            experiment3(survey1, "output/experiment3_40rand_60profiled_2clusters", 2, nonRandomSurveyResponses.subList(0, 600), randomSurveyResponses.subList(0, 400));
            experiment3(survey1, "output/experiment3_60rand_40profiled_2clusters", 2, nonRandomSurveyResponses.subList(0, 400), randomSurveyResponses.subList(0, 600));

            experiment3(survey1, "output/experiment3_20rand_80lexico_3clusters", 3, lexicographicSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
            experiment3(survey1, "output/experiment3_40rand_60profiled_3clusters", 3, nonRandomSurveyResponses.subList(0, 600), randomSurveyResponses.subList(0, 400));
            experiment3(survey1, "output/experiment3_60rand_20profiled_3clusters", 3, nonRandomSurveyResponses.subList(0, 400), randomSurveyResponses.subList(0, 600));

            experiment3(survey1, "output/experiment3_33rand_33lexico_33profiled_2clusters", 2, lexicographicSurveyResponses.subList(0, 333), nonRandomSurveyResponses.subList(0, 333), randomSurveyResponses.subList(0, 333));
            experiment3(survey1, "output/experiment3_33rand_33lexico_33profiled_3clusters", 3, lexicographicSurveyResponses.subList(0, 333), nonRandomSurveyResponses.subList(0, 333), randomSurveyResponses.subList(0, 333));
        } else if (experiment == 4) {
            experiment4(survey1, "output/experiment4_20rand_80lexico", lexicographicSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
        }


        //experiment1(survey1, lexicographicSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
        //experiment2(survey1, nonRandomSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
        List<SurveyResponse> experiment3responses = new ArrayList<SurveyResponse>();
        experiment3responses.addAll(lexicographicSurveyResponses.subList(0, 333));
        experiment3responses.addAll(randomSurveyResponses.subList(0, 333));
        experiment3responses.addAll(nonRandomSurveyResponses.subList(0, 333));
    }

}

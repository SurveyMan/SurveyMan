package edu.umass.cs.surveyman.samples;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.Simulation;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.*;
import edu.umass.cs.surveyman.qc.respondents.LexicographicRespondent;
import edu.umass.cs.surveyman.qc.respondents.NoisyLexicographicRespondent;
import edu.umass.cs.surveyman.qc.respondents.NonRandomRespondent;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.lang3.StringUtils;

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
        QCMetrics.rng.shuffle(surveyRespondents.toArray());
        assert surveyRespondents.size() > 0;
        Simulation.ROC entropyROC = Simulation.analyze(survey, surveyRespondents, Classifier.ENTROPY, 0.05, 2);
        Simulation.ROC llROC = Simulation.analyze(survey, surveyRespondents, Classifier.LOG_LIKELIHOOD, 0.05, 2);
        OutputStreamWriter osw;
        try {
            osw = new OutputStreamWriter(new FileOutputStream(filename + "_entropyROC"));
            osw.write("percBots,empiricalEntropy,truePositive,falsePositive,trueNegative,falseNegative\n");
            osw.write(entropyROC.toString());
            osw.close();
            osw = new OutputStreamWriter(new FileOutputStream(filename + "_llROC"));
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
        Collections.shuffle(responses, QCMetrics.rng);
        Simulation.ROC rocs = Simulation.analyze(survey, responses, Classifier.CLUSTER, clusters, 2);
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
        Collections.shuffle(responses, QCMetrics.rng);
        // do PCA
        Simulation.ROC rocs = Simulation.analyze(survey, responses, Classifier.LINEAR, 0.0, 2);
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

    public static void experiment5(Survey survey,
                                   String filename,
                                   List<? extends SurveyResponse>... responseLists)
            throws SurveyException
    {
        List<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        for (List<? extends SurveyResponse> srs: responseLists)
            responses.addAll(srs);
        Collections.shuffle(responses, QCMetrics.rng);
        // do PCA
        Simulation.ROC rocs = Simulation.analyze(survey, responses, Classifier.LPO, 0.0, 2);
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
        Collections.shuffle(respondentAll, QCMetrics.rng);
        float[][] x = generateFeaturesForExperiment1(survey, respondentAll);
        // learn separating hyperplane between honest respondents and bad actors
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
                SurveyDatum a = questionResponse.getAnswer();
                List<SurveyDatum> as = Arrays.asList(q.getOptListByIndex());
                String[] vals = {srid, clz, Boolean.toString(honestRespondent),
                        q.id,
                        Integer.toString(questions.indexOf(q)),
                        Integer.toString(questionResponse.getIndexSeen()),
                        a.getId(),
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
        // Generate surveys of varying size
        Survey survey1 = new Survey();
        for (char c1 : "ABCDEFGHIJ".toCharArray()) {
            Question q = new Question(Character.toString(c1));
            survey1.addQuestion(q);
            for (char c : "abcde".toCharArray())
                q.addOption(Character.toString(c));
        }


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

        Collections.shuffle(lexicographicSurveyResponses, QCMetrics.rng);
        Collections.shuffle(noisyLexicographicSurveyResponses, QCMetrics.rng);
        Collections.shuffle(nonRandomSurveyResponses, QCMetrics.rng);
        Collections.shuffle(randomSurveyResponses, QCMetrics.rng);

//        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("survey_responses_moar_entropy.csv"));
//        String[] headers = { "response_id", "class", "honest_respondent",
//                "question_id", "question_index", "question_index_seen",
//                "answer_id", "answer_index", "answer_index_seen"};
//        osw.write(StringUtils.join(headers, ",") + "\n");
//        dumpData(survey1, lexicographicSurveyResponses, "lexicographic", true, osw);
//        dumpData(survey1, nonRandomSurveyResponses, "profiled", true, osw);
//        dumpData(survey1, randomSurveyResponses, "random", false, osw);
//        osw.close();

        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("0")) {
                for (int j = 0; j < 1000; j += 100) {
                    // lexicographic vs random
                    experiment0(survey1, String.format("output/experiment0_%drand_%dlexico", j, 1000 - j),
                            randomSurveyResponses.subList(0, j),
                            lexicographicSurveyResponses.subList(0, 1000 - j));
                    // noisy lexicographic vs random
                    experiment0(survey1, String.format("output/experiment0_%drand_%dnoisy", j, 1000 - j),
                            randomSurveyResponses.subList(0, j),
                            noisyLexicographicSurveyResponses.subList(0, 1000 - j));
                    // profiled resondent vs random
                    experiment0(survey1, String.format("output/experiment0_%drand_%dprofiled", j, 1000 - j),
                            randomSurveyResponses.subList(0, j),
                            nonRandomSurveyResponses.subList(0, 1000 - j));
                    // noisy vs profiled
                    experiment0(survey1, String.format("output/experiment0_%dnoisy_%dprofiled", j, 1000 - j),
                            noisyLexicographicSurveyResponses.subList(0, j),
                            nonRandomSurveyResponses.subList(0, 1000 - j));
                }
            } else if (args[i].equals("3")) {
                // 2 clusters
                for (int c : new int[]{2, 3}) {
                    for (int j = 0; j < 1000; j += 100) {
                        // lexicographic vs random
                        experiment3(survey1, String.format("output/experiment3_%drand_%dlexico_%dclusters", j, 1000 - j, c),
                                c, randomSurveyResponses.subList(0, j), lexicographicSurveyResponses.subList(0, 1000 - j));
                        // noisy lexicographic vs random
                        experiment3(survey1, String.format("output/experiment3_%drand_%dnoisy_%dclusters", j, 1000 - j, c),
                                c, randomSurveyResponses.subList(0, j), noisyLexicographicSurveyResponses.subList(0, 1000 - j));
                        // profiled vs random
                        experiment3(survey1, String.format("output/experiment3_%drand_%dprofiled_%dclusters", j, 1000 - j, c),
                                c, nonRandomSurveyResponses.subList(0, 1000 - j), randomSurveyResponses.subList(0, j));
                        // noisy vs profiled
                        experiment3(survey1, String.format("output/experiment3_%dnoisy_%dprofiled_%dclusters", j, 1000 - j, c),
                                c, noisyLexicographicSurveyResponses.subList(0, j), nonRandomSurveyResponses.subList(0, 1000 - j));
                    }
                }
            } else if (args[i].equals("4")) {
                experiment4(survey1, "output/experiment4_20rand_80lexico", lexicographicSurveyResponses.subList(0, 800), randomSurveyResponses.subList(0, 200));
            } else if (args[i].equals("5")) {
                for (int j = 0; j < 1000; j += 100) {
                    // lexicographic vs random
                    experiment5(survey1, String.format("output/experiment5_%drand_%dlexico", j, 1000 - j),
                            randomSurveyResponses.subList(0, j),
                            lexicographicSurveyResponses.subList(0, 1000 - j));
                    // noisy lexicographic vs random
                    experiment5(survey1, String.format("output/experiment5_%drand_%dnoisy", j, 1000 - j),
                            randomSurveyResponses.subList(0, j),
                            noisyLexicographicSurveyResponses.subList(0, 1000 - j));
                    // profiled resondent vs random
                    experiment5(survey1, String.format("output/experiment5_%drand_%dprofiled", j, 1000 - j),
                            randomSurveyResponses.subList(0, j),
                            nonRandomSurveyResponses.subList(0, 1000 - j));
                    // noisy vs profiled
                    experiment5(survey1, String.format("output/experiment5_%dnoisy_%dprofiled", j, 1000 - j),
                            noisyLexicographicSurveyResponses.subList(0, j),
                            nonRandomSurveyResponses.subList(0, 1000 - j));
                }
            }
        }
    }
}

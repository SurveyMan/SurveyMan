package qc;

import csv.CSVLexer;
import csv.CSVParser;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import qc.QCMetrics.FreqProb;
import qc.QCMetrics.QCMetric;
import qc.RandomRespondent.AdversaryType;
import survey.*;

/**
 * Entry point for quality control.
 * SurveyPoster functionality should be called in this class
 * 
 */

public class QC {


    public enum QCActions {
        REJECT, BLOCK, APPROVE, DEQUALIFY;
    }

    public static final String BOT = "This worker has been determined to be a bot.";
    public static final String QUAL = "This worker has already taken one of our surveys.";
    public static final Random rng = new Random(System.currentTimeMillis());
    public static final int bootstrapReps = 200;
    
    private Survey survey;

    private List<SurveyResponse> validResponses = new LinkedList<SurveyResponse>();
    private List<SurveyResponse> botResponses = new LinkedList<SurveyResponse>();
    
    public QC(Survey survey) throws SurveyException {
        this.survey = survey;
    }
    
    /**
     * Finds outliers in the raw responses returned; uses parametric bootstrap for now.
     * @param responses
     * @return 
     */
    public List<SurveyResponse> getOutliers(List<SurveyResponse> responses, QCMetric metric) {
        List<SurveyResponse> outliers = new ArrayList<SurveyResponse>();
        List<Double> appliedStat = new ArrayList<Double>();
        FreqProb fp = new FreqProb(survey, responses);
        for (SurveyResponse sr : responses)
            switch (metric) {
              case LIKELIHOOD:
                appliedStat.add(QCMetrics.getLogLikelihood(sr, fp));
                break;
            }
        double[] bootstrapSample = makeBootstrapSample(appliedStat);
        double bootstrapMean = getBootstrapMean(bootstrapSample);
        double bootstrapSD = getBootstrapSD(bootstrapSample, bootstrapMean);
        for (SurveyResponse sr : responses) {
          double likelihood = QCMetrics.getLogLikelihood(sr, fp);
          if (Math.abs(bootstrapMean - likelihood) > 2*bootstrapSD)
            outliers.add(sr);
        }
        return outliers;
    }

    private int getMaxM() {
      int m = 0;
      for (Question q : survey.questions) {
        int size = q.options.size();
        if (size > m)
          m = size;
      }
      return m;
    }
    
    public List<RandomRespondent> makeBotPopulation (QCMetrics qcMetrics) throws SurveyException {
        List<RandomRespondent> syntheticBots = new ArrayList<RandomRespondent>();
        int m = getMaxM();
        double alpha = 0.05;
        double delta = 0.1;
        // want our bot population to be large enough that every question has the expected number of bots with high prob
        int n = (int) Math.ceil((-3 * m * Math.log(alpha)) / Math.pow(delta, 2));
        AdversaryType adversaryType = RandomRespondent.selectAdversaryProfile(qcMetrics);
        for (int i = 0 ; i < n ; i++)
          syntheticBots.add(new RandomRespondent(survey, adversaryType));
        return syntheticBots;
    }
    
    public List<SurveyResponse> getBots(List<SurveyResponse> responses, QCMetrics qcMetrics) throws SurveyException {
      List<RandomRespondent> syntheticBots = makeBotPopulation(qcMetrics);
      List<SurveyResponse> allResponses = new ArrayList<SurveyResponse>();
      Collections.copy(responses, allResponses);
      for (RandomRespondent rr : syntheticBots)
        allResponses.add(rr.response);
      return getOutliers(allResponses, QCMetric.LIKELIHOOD);
    }
            
    public boolean complete(List<SurveyResponse> responses, Properties props) {
        // this needs to be improved
        String numSamples = props.getProperty("numparticipants");
        if (numSamples!=null)
            return validResponses.size() >= Integer.parseInt(numSamples);
        else return true;
    }

    private double[] makeBootstrapSample(List<Double> rawSample){

        double[] bootstrapSample = new double[bootstrapReps];

        System.out.println("rawsample");
        for (Double d : rawSample)
            System.out.print(" " + d + " ");
        System.out.println();

        for (int i = 0 ; i < bootstrapReps ; i++)
            bootstrapSample[i] = rawSample.get(rng.nextInt(rawSample.size()));

        return bootstrapSample;
    }

    private double getBootstrapMean(double[] bootstrapSample) {
        double sum = 0.0;
        for (int i = 0 ; i < bootstrapSample.length ; i++)
            sum += bootstrapSample[i];
        return sum / bootstrapSample.length;
    }

    private double getBootstrapSD(double[] bootstrapSample, double bootstrapMean) {
        double sumOfSquaredDiffs = 0.0;
        for (int i = 0 ; i < bootstrapSample.length ; i++)
            sumOfSquaredDiffs += Math.pow(bootstrapSample[i] - bootstrapMean, 2.0);
        return Math.pow(sumOfSquaredDiffs / (bootstrapSample.length - 1), 0.5);
    }

    /**
     * Assess the validity of the SurveyResponse {@link SurveyResponse}.
     * @param sr
     * @return A list of QCActions to be interpreted by the service's specification.
     */
    public QCActions[] assess(SurveyResponse sr) {
        // add this survey response to the list of valid responses
        validResponses.add(sr);
        // update the frequency map to reflect this new response
        //updateFrequencyMap();
        //updateAverageLikelihoods();
        // classify this answer as a bot or not
        boolean bot = false; //isBot(sr);
        // classify any old responses as bot or not
        //updateValidResponses();
        // recompute likelihoods
        //updateAverageLikelihoods();
        if (bot) {
            return new QCActions[]{ QCActions.REJECT, QCActions.DEQUALIFY };
        } else {
            //service.assignQualification("survey", a.getWorkerId(), 1, false);
            return new QCActions[]{ QCActions.APPROVE, QCActions.DEQUALIFY };
        }
    }
    
    public static void main(String[] args) 
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
      if (args.length < 2)
        System.out.println(String.format("USAGE:\t java -cp /path/to/jar qc.QC <survey_filename> <sep> <result_filename>"));
      String surveyFilename = args[0];
      String sep = args[1];
      String resultFilename = args[2];
      CSVParser parser = new CSVParser(new CSVLexer(surveyFilename, sep));
      Survey survey = parser.parse();
      
    }

}

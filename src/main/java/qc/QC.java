package qc;

import java.util.*;

import com.amazonaws.mturk.requester.AssignmentStatus;
import scala.Tuple2;
import survey.Component;
import survey.Question;
import survey.Survey;
import survey.SurveyResponse;
import system.mturk.MturkLibrary;

/**
 * Entry point for quality control.
 * SurveyPoster functionality should be called in this class
 * 
 */

public class QC {

    public static final String BOT = "This worker has been determined to be a bot.";
    public static final String QUAL = "This worker has already taken one of our surveys.";
    public static final Random rng = new Random(System.currentTimeMillis());
    public static final int bootstrapReps = 200;

    private Survey survey;
    private List<SurveyResponse> validResponses = new LinkedList<SurveyResponse>();
    private List<SurveyResponse> botResponses = new LinkedList<SurveyResponse>();
    private Map<Question, HashMap<String, Integer>> frequencyMap = new HashMap<Question, HashMap<String, Integer>>();
    // do we really need to be efficient?
    //public double[] averageLikelihood = new double[Integer.parseInt(MturkLibrary.props.getProperty("numparticipants"))];
    private List<Double> averageLikelihoods = new LinkedList<Double>();
    private List<Double> averageRandomLikelihoods = new LinkedList<Double>();
    private boolean pastChangepoint = false;

    public QC(Survey survey) {
        this.survey = survey;
        double randomLikelihood = 0.0;
        // don't initialize the interior map, since this could cause us to try to create a power set
        for (Question q : survey.questions) {
            frequencyMap.put(q, new HashMap<String, Integer>());
            // need to seed with random respondents
            // pick the proportion that corresponds to empirical uniform
            randomLikelihood += 1.0 / q.options.size();
        }
        // we know that we can detect random respondents when 95% of our random respondents are classified as random
        // how to choose how many random respondents? let's start with 10
        // add a small amount of noise around the random likelihood
        randomLikelihood /= survey.questions.size();
        for (int i = 0 ; i < 10 ; i++)
            averageLikelihoods.add(randomLikelihood + rng.nextGaussian());
    }

    public boolean complete(List<SurveyResponse> responses, Properties props) {
        // this needs to be improved
        String numSamples = props.getProperty("numparticipants");
        if (numSamples!=null)
            return responses.size() >= Integer.parseInt(numSamples);
        else return true;
    }

    private String getOptionId(List<Tuple2<Component, Integer>> options) {
        String id = "";
        for (Tuple2<Component, Integer> tupe : options)
            id = id + tupe._1().cid;
        return id;
    }

    private void updateFrequencyMap(List<SurveyResponse> responses) {
        for (SurveyResponse sr : responses)
            updateFrequencyMap(sr);
    }

    private void updateFrequencyMap(SurveyResponse sr) {
        for (SurveyResponse.QuestionResponse qr : sr.responses){
            // get a reference to the appropriate question's frequency map
            HashMap<String, Integer> frequencies = frequencyMap.get(qr.q);
            String id = getOptionId(qr.opts);
            if (frequencies.containsKey(id))
                frequencies.put(id, frequencies.get(id)+1);
            else frequencies.put(id, 1);
        }
    }

    /**
     * Recomputes the frequency map on the basis of the valid responses
     */
    private void updateFrequencyMap(){
        for (SurveyResponse sr : validResponses)
            for (SurveyResponse.QuestionResponse qr : sr.responses) {
                HashMap<String, Integer> frequencies = frequencyMap.get(qr.q);
                String id = getOptionId(qr.opts);
                if (frequencies.containsKey(id))
                    frequencies.put(id, frequencies.get(id)+1);
                else frequencies.put(id, 1);
            }
    }

    private double computeAverageLikelihood(SurveyResponse sr) {
        double likelihood = 0.0;
        for (SurveyResponse.QuestionResponse qr : sr.responses) {
            String id = getOptionId(qr.opts);
            likelihood += frequencyMap.get(qr.q).get(id) / (double) validResponses.size();
        }
        return likelihood / sr.responses.size();
    }

    /**
     * Recomputes the list of average likelihoods according to what's currently in the valid response list
     */
    private void updateAverageLikelihoods() {
        averageLikelihoods.clear();
        for (SurveyResponse sr : validResponses)
            averageLikelihoods.add(computeAverageLikelihood(sr));
    }

    /**
     * Moves responses that were previously flagged as valid over to bots
     */
    private void updateValidResponses() {
        // probably don't have to run this post-changepoint
        for (SurveyResponse sr : validResponses) {
            if (isBot(sr)) {
                validResponses.remove(sr);
                botResponses.add(sr);
            }
        }
    }


    private double[] makeBootstrapSample(){
        double[] bootstrapSample = new double[bootstrapReps];
        double[] rawSample;
        Double[] temp = averageLikelihoods.toArray(new Double[averageLikelihoods.size()]);
        if (pastChangepoint) {
            rawSample = new double[averageLikelihoods.size()];
            for (int i = 0 ; i < rawSample.length ; i++)
                rawSample[i] = temp[i];
        } else {
            rawSample = new double[averageLikelihoods.size() + averageRandomLikelihoods.size()];
            Double[] temp2 = averageRandomLikelihoods.toArray(new Double[averageLikelihoods.size()]);
            for (int i = 0 ; i < temp.length ; i++)
                rawSample[i] = temp[i];
            for (int i = 0 ; i < temp2.length ; i++)
                rawSample[temp.length+i] = temp2[i];
        }
        for (int i = 0 ; i < bootstrapReps ; i++)
            bootstrapSample[i] = rawSample[rng.nextInt(rawSample.length)];

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

    private boolean isBot(SurveyResponse sr) {
        // then the bootstrap outliers are true outliers
        double[] bootstrapSample = makeBootstrapSample();
        double bootstrapMean = getBootstrapMean(bootstrapSample);
        double bootstrapSD = getBootstrapSD(bootstrapSample, bootstrapMean);
        double distance = Math.abs(bootstrapMean - computeAverageLikelihood(sr));
        double confidence = bootstrapSD * 2.0;
        if (pastChangepoint)
            return distance > confidence;
        else return distance < confidence;
    }

    public double maxEntropy(Survey s){
        double entropy = 0;
        for (Question q : s.questions){
            // assume equal probability for every question
            entropy += q.data.size() * Math.log(q.data.size());
        }
        return entropy*-1;
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
        updateFrequencyMap();
        // classify this answer as a bot or not
        boolean bot = isBot(sr);
        // classify any old responses as bot or not
        updateValidResponses();
        // recompute likelihoods
        updateAverageLikelihoods();
        if (bot) {
            return new QCActions[]{ QCActions.REJECT };
        } else {
            //service.assignQualification("survey", a.getWorkerId(), 1, false);
            return new QCActions[]{ QCActions.APPROVE };
        }
    }

}

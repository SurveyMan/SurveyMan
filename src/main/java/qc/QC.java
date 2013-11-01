package qc;

import java.util.*;

import com.amazonaws.mturk.requester.AssignmentStatus;
import scala.Tuple2;
import survey.*;
import system.mturk.MturkLibrary;

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

    public Map<Question, HashMap<String, Integer>> frequencyMap = new HashMap<Question, HashMap<String, Integer>>();

//    private List<Double> averageLikelihoods = new Vector<Double>();
//    private List<Double> averageEntropies = new Vector<Double>();
//    private List<Double> averageRandomLikelihoods = new Vector<Double>();
//    private List<Double> averageRandomEntropies = new Vector<Double>();

    public QC(Survey survey) throws SurveyException {
        this.survey = survey;
        // don't initialize the interior map, since this could cause us to try to create a power set
//        for (Question q : survey.questions)
//            frequencyMap.put(q, new HashMap<String, Integer>());
        // generate random frequencies
        HashMap<Question, HashMap<String, Integer>> randFreqMap = new HashMap<Question, HashMap<String, Integer>>();
        // even though we think random respondents behave differently for ordered questions, let's just play our usual model for now
        // we can add this part later
        // get max options
//        int numRandSamples = 0;
//        for (Question q : survey.questions) {
//            int numOpts = 0;
//            if (q.exclusive)
//                numOpts = q.options.size();
//            else numOpts = (int) Math.pow(2, q.options.size())-1;
//            if (numOpts > numRandSamples)
//                numRandSamples = numOpts;
//        }
//        for (Question q : survey.questions){
//            HashMap<String, Integer> optFreq = new HashMap<String, Integer>();
//            // initialize frequencies
//            for (Component c : q.options.values())
//                optFreq.put(c.getCid(), 0);
//            for (int i = 0 ; i < numRandSamples ; i++) {
//                // choose a random response
//                if (q.exclusive){
//                    if (q.ordered) {
//                        int buckets = q.options.size() / 2 + 1;
//                        double grossProb = 1.0 / buckets;
//                        Component[] opts = q.getOptListByIndex();
//                        String id = "";
//                        double determiner = rng.nextDouble();
//                        for (int j = 0 ; j < buckets ; j++)
//                            if (determiner < grossProb + (j * grossProb))
//                                if (j==0)
//                                    id = opts[q.options.size() / 2].getCid();
//                                else if (determiner < grossProb / 2.0)
//                                    id = opts[(q.options.size() / 2) + 1].getCid();
//                                else
//                                    id = opts[(q.options.size() / 2) - 1].getCid();
//                        optFreq.put(id, optFreq.get(id)+1);
//                    } else {
//                        int responseIndex = rng.nextInt(q.options.size());
//                        Collection<Component> ids = q.options.values();
//                        Component[] idarray = ids.toArray(new Component[ids.size()]);
//                        String id = idarray[responseIndex].getCid();
//                        optFreq.put(id, optFreq.get(id)+1);
//                    }
//                } else {
//                    // never select 0, can select up to all
//                    String id = "";
//                    while (id.equals(""))
//                        for (Component c : q.options.values())
//                            if (rng.nextDouble() > 0.5)
//                                id += c.getCid();
//                    if (optFreq.containsKey(id))
//                        optFreq.put(id, optFreq.get(id)+1);
//                    else optFreq.put(id, 1);
//                }
//            }
//            randFreqMap.put(q, optFreq);
//        }
        // add to random likelihoods and random entropies by selecting a random path through the questions
//        for (int i = 0 ; i < 10 ; i++){
//            double randomLogLikelihood = 0.0;
//            for (Question q : survey.questions) {
//                if (!q.ordered){
//                    Set<String> ids = randFreqMap.get(q).keySet();
//                    String randId = ids.toArray(new String[ids.size()])[rng.nextInt(ids.size())];
//                    double ct = (double) randFreqMap.get(q).get(randId);
//                    double total = 0.0;
//                    for (Integer j : randFreqMap.get(q).values())
//                        total += (double) j;
//                    //System.out.println(String.format("randId : %s, ct : %f, total : %f", randId, ct, total));
//                    assert(total!=0.0);
//                    // if we get a zero count, punt and try again
//                    if (ct==0.0){
//                        i--;
//                        break;
//                    } else randomLogLikelihood += Math.log(ct / total);
//                }
//            }
            //System.out.println(randomLogLikelihood);
            //averageRandomLikelihoods.add(randomLogLikelihood);
        }
        // not adding entropies for now


    public boolean complete(List<SurveyResponse> responses, Properties props) {
        // this needs to be improved
        String numSamples = props.getProperty("numparticipants");
        if (numSamples!=null)
            return validResponses.size() >= Integer.parseInt(numSamples);
        else return true;
    }

    private String getOptionId(List<Tuple2<Component, Integer>> options) {
        String id = "";
        for (Tuple2<Component, Integer> tupe : options)
            id = id + tupe._1().getCid();
        return id;
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

//    private double computeLogLikelihood(SurveyResponse sr) {
//        double likelihood = 0.0;
//        for (SurveyResponse.QuestionResponse qr : sr.responses) {
//            String id = getOptionId(qr.opts);
//            double numAtThisID = frequencyMap.get(qr.q).get(id);
//            double totalAnsweredForThisQuestion = 0.0;
//            for (Integer i : frequencyMap.get(qr.q).values())
//                totalAnsweredForThisQuestion += (double) i;
//            likelihood += Math.log(numAtThisID / totalAnsweredForThisQuestion);
//        }
//        return likelihood;
//    }

    /**
     * Recomputes the list of average likelihoods according to what's currently in the valid response list
     */
//    private void updateAverageLikelihoods() {
//        averageLikelihoods.clear();
//        for (SurveyResponse sr : validResponses)
//            averageLikelihoods.add(computeLogLikelihood(sr));
//    }

    /**
     * Moves responses that were previously flagged as valid over to bots
     */
//    private void updateValidResponses() {
//        // probably don't have to run this post-changepoint
//        Iterator<SurveyResponse> itr = validResponses.iterator();
//        while(itr.hasNext()) {
//            SurveyResponse sr = itr.next();
//            if (isBot(sr)) {
//                //System.out.println(String.format("Classified %s as bot.", sr.toString()));
//                itr.remove();
//                botResponses.add(sr);
//            }
//        }
//    }

    private double[] makeBootstrapSample(Vector<Double> rawSample){

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

//    private boolean isBot(SurveyResponse sr) {
//        // then the bootstrap outliers are true outliers
//        // bot testing is always done against the random population
//        Vector<Double> rawSample = new Vector<Double>();
//        // should add classified bot responses in to this later
//        for (Double d : averageRandomLikelihoods)
//            rawSample.add(d);
//        double[] bootstrapSample = makeBootstrapSample(rawSample);
//        double bootstrapMean = getBootstrapMean(bootstrapSample);
//        double bootstrapSD = getBootstrapSD(bootstrapSample, bootstrapMean);
//        double logLikelihood = computeLogLikelihood(sr);
//        double distance = Math.abs(bootstrapMean - logLikelihood);
//        double confidence = bootstrapSD * 2.0;
//        System.out.println(String.format("bsmean : %f, bsstd : %f, this likelihood : %f", bootstrapMean, bootstrapSD, logLikelihood));
//        return distance < confidence;
//    }



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

}

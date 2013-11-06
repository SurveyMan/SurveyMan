package qc;

import csv.CSVLexer;
import csv.CSVParser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import qc.QCMetrics.FreqProb;
import qc.QCMetrics.QCMetric;
import qc.RandomRespondent.AdversaryType;
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
    public int numSyntheticBots =  0;
    
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
        double[][] bootstrapSample = QCMetrics.makeBootstrapSample(appliedStat, bootstrapReps, rng);
        double[] bootstrapMeans = QCMetrics.getBootstrapMeans(bootstrapSample);
        double bootstrapMean = QCMetrics.getBootstrapMean(bootstrapMeans);
        double bootstrapSD = QCMetrics.getBootstrapSD(bootstrapMeans, bootstrapMean);
        System.out.println(String.format("bootstrap mean : %f \t bootstrap s.d. : %f", bootstrapMean, bootstrapSD));
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
        numSyntheticBots = n;
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
        String qcFileName = String.format("%s%sqc_%s_%s.csv", MturkLibrary.OUTDIR, MturkLibrary.fileSep, survey.sourceName, MturkLibrary.TIME);
        QC qc = new QC(survey);
        Map<AdversaryType, Integer> adversaryTypeIntegerMap = new HashMap<AdversaryType, Integer>();
        adversaryTypeIntegerMap.put(AdversaryType.UNIFORM, 1);
        QCMetrics qcMetrics = new QCMetrics(adversaryTypeIntegerMap);
        List<SurveyResponse> responses = SurveyResponse.readSurveyResponses(survey, resultFilename);
        // results to print
        List<SurveyResponse> outliers = qc.getOutliers(responses, QCMetric.LIKELIHOOD);
        List<SurveyResponse> bots = qc.getBots(responses, qcMetrics);
        BufferedWriter bw = new BufferedWriter(new FileWriter(qcFileName));
        bw.write(String.format("// %d OUTLIERS%s", outliers.size(), SurveyResponse.newline));
        for (SurveyResponse sr : outliers)
            bw.write(sr.toString() + SurveyResponse.newline);
        bw.write(String.format("// %d BOTS detected (out of %d synthesized) %s", bots.size(), qc.numSyntheticBots, SurveyResponse.newline));
        for (SurveyResponse sr : bots)
            bw.write(sr.toString() + SurveyResponse.newline);
        bw.close();
    }

}

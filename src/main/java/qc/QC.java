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
import scala.Tuple2;
import survey.*;
import survey.SurveyResponse.QuestionResponse;
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
    public double alpha = 0.005;
    
    public QC(Survey survey) throws SurveyException {
        this.survey = survey;
    }
    
    /**
     * Finds outliers in the raw responses returned; uses parametric bootstrap for now.
     * @param responses
     * @return 
     */
    public List<SurveyResponse> getOutliers(List<SurveyResponse> responses, QCMetric metric) throws SurveyException {
        List<SurveyResponse> outliers = new ArrayList<SurveyResponse>();
        List<Double> appliedStat = new ArrayList<Double>();
        FreqProb fp = new FreqProb(survey, responses);
        for (SurveyResponse sr : responses)
            switch (metric) {
                case LIKELIHOOD:
                    appliedStat.add(QCMetrics.getLogLikelihood(sr, fp));
                    break;
                case LEAST_POPULAR:
                    appliedStat.add(QCMetrics.getLeastPopularOptions(sr, fp));
                    break;
            }
        double[][] bootstrapSample = QCMetrics.makeBootstrapSample(appliedStat, bootstrapReps, rng);
        double[] bootstrapMeans = QCMetrics.getBootstrapMeans(bootstrapSample);
        //double[] bootstrapSEs = QCMetrics.getBootstrapSEs(bootstrapSample, bootstrapMeans);
        double bootstrapMean = QCMetrics.getBootstrapAvgMetric(bootstrapMeans);
        //double bootstrapSD = QCMetrics.getBootstrapSE(bootstrapSEs);
        double[] bootstrapUpperQuants = QCMetrics.getBootstrapUpperQuants(bootstrapSample, alpha);
        double[] bootstrapLowerQuants = QCMetrics.getBootstrapLowerQuants(bootstrapSample, alpha);
        double upperQuant = QCMetrics.getBootstrapAvgMetric(bootstrapUpperQuants);
        double lowerQuant = QCMetrics.getBootstrapAvgMetric(bootstrapLowerQuants);
        System.out.println(String.format("bootstrap mean : %f \t bootstrap upper %f : %f\t bootstrap lower %f : %f"
                , bootstrapMean, alpha, upperQuant, alpha, lowerQuant));
        for (SurveyResponse sr : responses) {
            double likelihood = QCMetrics.getLogLikelihood(sr, fp);
            sr.score = likelihood;
            if (likelihood < lowerQuant || likelihood > upperQuant )
                outliers.add(sr);
            else System.out.println(String.format("%s : %f", sr.srid, likelihood));
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
        double beta = 0.05;
        double delta = 0.1;
        // want our bot population to be large enough that every question has the expected number of bots with high prob
        int n = (int) Math.ceil((-3 * m * Math.log(beta)) / Math.pow(delta, 2));
        numSyntheticBots = n;
        AdversaryType adversaryType = RandomRespondent.selectAdversaryProfile(qcMetrics);
        for (int i = 0 ; i < n ; i++)
            syntheticBots.add(new RandomRespondent(survey, adversaryType));
        return syntheticBots;
    }
    
    public List<SurveyResponse> getSyntheticOutliers(List<SurveyResponse> responses, QCMetrics qcMetrics) throws SurveyException {
        List<RandomRespondent> syntheticBots = makeBotPopulation(qcMetrics);
        SurveyResponse[] allResponses = new SurveyResponse[responses.size() + syntheticBots.size()];
        System.arraycopy(responses.toArray(new SurveyResponse[responses.size()]), 0, allResponses, 0, responses.size());
        int i = responses.size();
        for (RandomRespondent rr : syntheticBots){
            allResponses[i] = rr.response;
            i++;
        }
        return getOutliers(Arrays.asList(allResponses), QCMetric.LEAST_POPULAR);
    }

    public List<SurveyResponse> getBots(List<SurveyResponse> responses) throws SurveyException {
        Map<AdversaryType, Integer> adversaryTypeIntegerMap = new EnumMap<AdversaryType, Integer>(AdversaryType.class);
        adversaryTypeIntegerMap.put(AdversaryType.UNIFORM, 1);
        List<SurveyResponse> bots = new LinkedList<SurveyResponse>();
        FreqProb fp = new FreqProb(survey,responses);
        List<String> leastPopular = QCMetrics.leastPopularOptions(fp);
        double[] mus = new double[survey.questions.size()];
        for (Question q : survey.getQuestionsByIndex()){
            double m = q.options.size();
            double k = 0;
            for (String quid : q.options.keySet())
                if (leastPopular.contains(quid))
                    k++;
            mus[q.index] = m / k;
        }
        double logLikelihood = 0.0;
        for (SurveyResponse sr : responses) {
            for (QuestionResponse qr : sr.responses)
                for (Tuple2<Component, Integer> tupe : qr.opts)
                    if (leastPopular.contains(tupe._1().getCid())){
                        int index = survey.getQuestionById(qr.q.quid).index;
                        logLikelihood += Math.log(mus[index]);
                    }
            if (logLikelihood < Math.log(alpha))
                bots.add(sr);
        }
        return bots;
    }

    public List<SurveyResponse> getLazy(List<SurveyResponse> responses) throws SurveyException {
        Map<AdversaryType, Integer> adversaryTypeIntegerMap = new EnumMap<AdversaryType, Integer>(AdversaryType.class);
        adversaryTypeIntegerMap.put(AdversaryType.FIRST, 1);
        adversaryTypeIntegerMap.put(AdversaryType.LAST, 1);
        return getSyntheticOutliers(responses, new QCMetrics(adversaryTypeIntegerMap));
     }

    public List<SurveyResponse> getNoncommittal(List<SurveyResponse> responses) throws SurveyException {
        Map<AdversaryType, Integer> adversaryTypeIntegerMap = new EnumMap<AdversaryType, Integer>(AdversaryType.class);
        adversaryTypeIntegerMap.put(AdversaryType.INNER, 1);
        return getSyntheticOutliers(responses, new QCMetrics(adversaryTypeIntegerMap));
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
        List<SurveyResponse> responses = SurveyResponse.readSurveyResponses(survey, resultFilename);
        // take this out later
        List<Question> actualQuestionsAnswered = new LinkedList<Question>();
        for (SurveyResponse sr : responses) {
            for (SurveyResponse.QuestionResponse qr : sr.responses) {
                boolean foundQ = false;
                for (Question q : actualQuestionsAnswered)
                    if (q.quid.equals(qr.q.quid)){
                        foundQ = true;
                        break;
                    }
                if (!foundQ) actualQuestionsAnswered.add(qr.q);
            }
        }
        for (int i = 0 ; i < survey.questions.size() ; i++) {
            Question q = survey.questions.get(i);
            boolean foundQ = false;
            for (Question qq : actualQuestionsAnswered)
                if (qq.quid.equals(q.quid)) {
                    foundQ = true; break;
                }
            if (!foundQ)
                survey.removeQuestion(q.quid);
        }
        // results to print
        List<SurveyResponse> outliers = qc.getOutliers(responses, QCMetric.LIKELIHOOD);
        //List<SurveyResponse> lazy = qc.getLazy(responses);
        //List<SurveyResponse> boring = qc.getNoncommittal(responses);
        BufferedWriter bw = new BufferedWriter(new FileWriter(qcFileName));
        bw.write(String.format("// %d %s OUTLIERS (out of %d obtained from mturk)%s"
                , outliers.size()
                , QCMetric.LIKELIHOOD.name()
                , responses.size()
                , SurveyResponse.newline
            ));
        for (SurveyResponse sr : outliers)
            bw.write(sr.srid + sep + sr.real + sep + sr.score + SurveyResponse.newline);
        outliers = qc.getOutliers(responses, QCMetric.LEAST_POPULAR);
        bw.write(String.format("// %d %s OUTLIERS (out of %d obtained from mturk)%s"
                , outliers.size()
                , QCMetric.LEAST_POPULAR.name()
                , responses.size()
                , SurveyResponse.newline
            ));
        for (SurveyResponse sr : outliers)
            bw.write(sr.srid + sep + sr.real + sep + sr.score + SurveyResponse.newline);
        List<SurveyResponse> bots = qc.getBots(responses);
        bw.write(String.format("// %d BOTS detected (out of %d synthesized) %s", bots.size(), qc.numSyntheticBots, SurveyResponse.newline));
        for (SurveyResponse sr : bots){
            bw.write(sr.srid + sep + sr.real + sep + sr.score);
//            for (QuestionResponse qr : sr.responses)
//                for (Tuple2<Component, Integer> tupe : qr.opts)
//                    bw.write(sep + tupe._1().getCid());
            bw.write(SurveyResponse.newline);
        }
        bw.close();
    }

}

package qc;

import interstitial.ISurveyResponse;
import survey.Survey;
import survey.exceptions.SurveyException;

import java.util.*;

public class QC {


    public enum QCActions {
        REJECT, BLOCK, APPROVE, DEQUALIFY
    }

    public static final Random rng = new Random(System.currentTimeMillis());

    public final static List<String> repeaters = new ArrayList<String>();
    public final static Map<String, List<String>> participantIDMap = new HashMap<String, List<String>>();
    
    public Survey survey;
    public List<ISurveyResponse> validResponses = new ArrayList<ISurveyResponse>();
    public List<ISurveyResponse> botResponses = new ArrayList<ISurveyResponse>();
    public double alpha = 0.05;

    public QC(Survey survey) throws SurveyException {
        this.survey = survey;
        participantIDMap.put(survey.sid, new ArrayList<String>());
    }

    public boolean complete(List<ISurveyResponse> responses, Properties props) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        // this needs to be improved
        IQCMetrics qc = (IQCMetrics) Class.forName("qc.Metrics").newInstance();
        String numSamples = props.getProperty("numparticipants");
        assert(numSamples!=null);
        for (ISurveyResponse response : responses) {
            if (response==null) {
                System.err.println("response was null.");
                return true;
            }
            if (qc.entropyClassification(survey, response, responses))
                this.botResponses.add(response);
            else this.validResponses.add(response);
        }
        return validResponses.size() >= Integer.parseInt(numSamples);
    }

    public QCActions[] assess(ISurveyResponse sr) {
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
        List<String> participants = participantIDMap.get(survey.sid);
        /*
        if (participants.contains(sr.workerId)) {
            sr.msg = QC.QUAL;
            return new QCActions[]{ QCActions.REJECT, QCActions.DEQUALIFY };
        } else if (bot) {
            participants.add(sr.workerId);
            sr.msg = QC.BOT;
            return new QCActions[]{ QCActions.BLOCK, QCActions.DEQUALIFY };
        } else {
            //service.assignQualification("survey", a.getWorkerId(), 1, false);
            participants.add(sr.workerId);
            return new QCActions[]{ QCActions.APPROVE, QCActions.DEQUALIFY };
        }
        * */
        return new QCActions[] {QCActions.APPROVE, QCActions.DEQUALIFY};
    }


}

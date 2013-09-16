package qc;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.amazonaws.mturk.requester.AssignmentStatus;
import survey.Survey;
import survey.SurveyResponse;

/**
 * Entry point for quality control.
 * SurveyPoster functionality should be called in this class
 * 
 */

public class QC {
    
    public static Survey survey;
    public static final String BOT = "This worker has been determined to be a bot.";
    public static final String QUAL = "This worker has already taken one of our surveys.";
    
    public static boolean complete(List<SurveyResponse> responses, Properties props) {
        // this needs to be improved
        String numSamples = props.getProperty("numparticipants");
        if (numSamples!=null)
            return responses.size() >= Integer.parseInt(numSamples);
        else return true;
    }
    
    public static boolean isBot(SurveyResponse sr) {
        return false;
    }

    /**
     * Assess the validity of the SurveyResponse {@link SurveyResponse}.
     * @param sr
     * @return A list of QCActions to be interpreted by the service's specification.
     */
    public static QCActions[] assess(SurveyResponse sr) {
        if (QC.isBot(sr)) {
            return new QCActions[]{ QCActions.REJECT };
        } else {
            //service.assignQualification("survey", a.getWorkerId(), 1, false);
            return new QCActions[]{ QCActions.APPROVE };
        }
    }

}

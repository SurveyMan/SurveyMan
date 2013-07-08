package qc;

import java.util.HashMap;
import survey.Survey;
import survey.SurveyResponse;
import system.mturk.SurveyPoster;

/**
 * Entry point for quality control.
 * SurveyPoster functionality should be called in this class
 * 
 */

public class QC {
    
    public static int minSamples = SurveyPoster.parameters.getMaxAssignments();
    public static Survey survey;
    public static final String BOT = "This worker has been determined to be a bot.";
    public static final String QUAL = "This worker has already taken one of our surveys.";
    
    public static boolean complete(HashMap<String, SurveyResponse> responses) {
        // this needs to be improved
        return responses.size() >= minSamples;
    }
    
    public static boolean isBot(SurveyResponse sr) {
        return false;
    }
}
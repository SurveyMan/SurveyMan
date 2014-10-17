package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.analyses.ISurveyResponse;
import edu.umass.cs.surveyman.survey.Survey;

/**
 * Base class for all respondents. Respondent constructors take the survey and set up all preferences.
 */
public abstract class AbstractRespondent {

    public abstract ISurveyResponse getResponse();

}

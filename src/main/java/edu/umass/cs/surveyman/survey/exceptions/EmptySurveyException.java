package edu.umass.cs.surveyman.survey.exceptions;

/**
 * A Survey must have some content.
 */
public class EmptySurveyException extends SurveyException {

    public EmptySurveyException() {
        super("A survey must have at least one question.");
    }
}

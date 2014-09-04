package edu.umass.cs.surveyman.input.exceptions;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public class SyntaxException extends SurveyException {
    public SyntaxException(String msg) {
        super(msg);
    }
}

package edu.umass.cs.surveyman.input.exceptions;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public class MalformedBooleanException extends SurveyException {
    public MalformedBooleanException(String boolStr, String column) {
        super(String.format("Unrecognized boolean string (%s) in column %s. See the SurveyMan wiki for accepted strings.", boolStr, column));
    }
}

package edu.umass.cs.surveyman.qc.exceptions;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public class UnanalyzableException extends SurveyException {

    public UnanalyzableException(String msg) {
        super(msg);
    }
}

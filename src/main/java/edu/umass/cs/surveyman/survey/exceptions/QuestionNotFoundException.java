package edu.umass.cs.surveyman.survey.exceptions;

public class QuestionNotFoundException extends SurveyException {
    public QuestionNotFoundException(String quid, String sid) {
        super(String.format("Question with id %s not found in edu.umass.cs.surveyman.survey with id %s", quid, sid));
    }
    public QuestionNotFoundException(int i) {
        super(String.format("No question found at line %d", i));
    }
}

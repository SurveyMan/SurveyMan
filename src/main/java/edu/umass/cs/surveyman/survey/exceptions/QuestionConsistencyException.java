package edu.umass.cs.surveyman.survey.exceptions;

import edu.umass.cs.surveyman.survey.Question;

public class QuestionConsistencyException extends SurveyException {

    public QuestionConsistencyException(
            Question question,
            String property,
            boolean error)
    {
        super(String.format("Attemped to set %s in question %s to %b",
                property,
                question,
                error)
        );
    }
}

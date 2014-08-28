package edu.umass.cs.surveyman.survey.exceptions;

import edu.umass.cs.surveyman.survey.Question;

public class DuplicateQuestions extends SurveyException {
    public DuplicateQuestions(Question q1, Question q2) {
        super(String.format("Question (%s) is a duplicate of Question 2 (%s)", q1, q2));
    }
}

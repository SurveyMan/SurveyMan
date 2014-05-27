package survey.exceptions;

import survey.Question;

public class DuplicateQuestions extends SurveyException {
    public DuplicateQuestions(Question q1, Question q2) {
        super(String.format("Question (%s) is a duplicate of Question 2 (%s)", q1, q2));
    }
}

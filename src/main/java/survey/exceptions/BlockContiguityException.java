package survey.exceptions;

import survey.Question;

public class BlockContiguityException extends SurveyException {

    public BlockContiguityException(Question q0, Question q1) {
        super(String.format("Gap in question index between %s and %s", q0.toString(), q1.toString()));
    }
}

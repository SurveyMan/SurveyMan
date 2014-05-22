package survey.exceptions;

import survey.Question;

public class BlockContiguityException extends SurveyException {

    public BlockContiguityException(Question q0, Question q1) {
        super(String.format("Gap in question index between %s (index %d) and %s (index %d)"
                , q0.toString(), q0.index, q1.toString(), q1.index));
    }
}

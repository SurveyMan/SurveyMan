package input.exceptions;

import survey.exceptions.SurveyException;

import java.lang.reflect.Method;

public class SyntaxException extends SurveyException {
    public SyntaxException(String msg, Object caller, Method lastAction) {
        super(msg);
    }
}

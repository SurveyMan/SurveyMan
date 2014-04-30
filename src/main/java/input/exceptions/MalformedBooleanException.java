package input.exceptions;

import input.AbstractParser;
import survey.exceptions.SurveyException;

import java.lang.reflect.Method;

public class MalformedBooleanException extends SurveyException {
    public MalformedBooleanException(String boolStr, String column, AbstractParser caller, Method lastAction) {
        super(String.format("Unrecognized boolean string (%s) in column %s. See the SurveyMan wiki for accepted strings.", boolStr, column));
    }
}

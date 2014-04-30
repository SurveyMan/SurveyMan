package input.exceptions;

import input.csv.CSVParser;
import survey.exceptions.SurveyException;

import java.lang.reflect.Method;

/** Inner/nested classes*/
public class MalformedBlockException extends SurveyException {
    public MalformedBlockException(String strId, CSVParser caller, Method lastAction) {
        super(String.format("Malformed block identifier: %s", strId));
    }
}

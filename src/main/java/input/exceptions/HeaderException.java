package input.exceptions;

import input.csv.CSVLexer;
import survey.exceptions.SurveyException;

import java.lang.reflect.Method;

/** Inner/nested classes */
public class HeaderException extends SurveyException {
    public HeaderException(String msg, CSVLexer lexer, Method method) {
        super(msg);
    }
}

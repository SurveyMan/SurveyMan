package input.exceptions;

import input.csv.CSVLexer;
import survey.SurveyException;
import system.Bug;
import system.Debugger;

import java.lang.reflect.Method;

/** Inner/nested classes */
public class HeaderException extends SurveyException implements Bug {
    Object caller;
    Method lastAction;
    public HeaderException(String msg, CSVLexer lexer, Method method) {
        super(msg);
        caller = lexer;
        lastAction = method;
        Debugger.addBug(this);
    }
    @Override
    public Object getCaller(){
        return caller;
    }
    @Override
    public Method getLastAction(){
        return lastAction;
    }
}

package input.exceptions;

import input.csv.CSVParser;
import survey.SurveyException;
import system.Bug;
import system.Debugger;

import java.lang.reflect.Method;

/** Inner/nested classes*/
public class MalformedBlockException extends SurveyException implements Bug {
    Object caller;
    Method lastAction;
    public MalformedBlockException(String strId, CSVParser caller, Method lastAction) {
        super(String.format("Malformed block identifier: %s", strId));
        this.caller = caller;
        this.lastAction = lastAction;
        Debugger.addBug(this);
    }
    public Object getCaller() {
        return caller;
    }
    public Method getLastAction() {
        return lastAction;
    }
}

package input.exceptions;

import survey.SurveyException;
import system.Bug;
import system.Debugger;

import java.lang.reflect.Method;

public class SyntaxException extends SurveyException implements Bug {
    Object caller;
    Method lastAction;
    public SyntaxException(String msg, Object caller, Method lastAction) {
        super(msg);
        this.caller = caller;
        this.lastAction = lastAction;
        Debugger.addBug(this);
    }

    @Override
    public Object getCaller() {
        return lastAction;
    }

    @Override
    public Method getLastAction() {
        return lastAction;
    }
}

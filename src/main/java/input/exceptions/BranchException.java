package input.exceptions;

import input.csv.CSVParser;
import survey.SurveyException;
import system.Bug;
import system.Debugger;

import java.lang.reflect.Method;

public class BranchException extends SurveyException implements Bug {
    Object caller;
    Method lastAction;
    public BranchException(String fromBlockId, String toBlockId, CSVParser caller, Method lastAction) {
        super(String.format("Cannot jump from block %s to block %s. Must always jump into a block whose outermost numbering is greater."
                , fromBlockId, toBlockId));
        this.caller = caller;
        this.lastAction = lastAction;
        Debugger.addBug(this);
    }
    public BranchException(String msg, CSVParser caller, Method lastAction) {
        super(msg);
        this.caller = caller;
        this.lastAction = lastAction;
    }

    @Override
    public Object getCaller() {
        return caller;
    }

    @Override
    public Method getLastAction() {
        return lastAction;
    }
}

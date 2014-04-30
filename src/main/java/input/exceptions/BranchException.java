package input.exceptions;

import input.csv.CSVParser;
import survey.exceptions.SurveyException;

import java.lang.reflect.Method;

public class BranchException extends SurveyException {
    public BranchException(String fromBlockId, String toBlockId, CSVParser caller, Method lastAction) {
        super(String.format("Cannot jump from block %s to block %s. Must always jump into a block whose outermost numbering is greater."
                , fromBlockId, toBlockId));
    }
    public BranchException(String msg, CSVParser caller, Method lastAction) {
        super(msg);
    }
}

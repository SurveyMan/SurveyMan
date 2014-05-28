package input.exceptions;

import survey.exceptions.SurveyException;

public class BranchException extends SurveyException {
    public BranchException(String fromBlockId, String toBlockId) {
        super(String.format("Cannot jump from block %s to block %s. Must always jump into a block whose outermost numbering is greater."
                , fromBlockId, toBlockId));
    }
    public BranchException(String msg) {
        super(msg);
    }
}

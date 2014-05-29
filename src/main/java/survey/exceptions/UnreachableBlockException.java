package survey.exceptions;

import survey.Block;

public class UnreachableBlockException extends SurveyException {
    public UnreachableBlockException(Block b) {
        super(String.format("Cannot reach block %s", b.getStrId()));
    }
}

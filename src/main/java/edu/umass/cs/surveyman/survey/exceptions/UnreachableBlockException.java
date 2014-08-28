package edu.umass.cs.surveyman.survey.exceptions;

import edu.umass.cs.surveyman.survey.Block;

public class UnreachableBlockException extends SurveyException {
    public UnreachableBlockException(Block b) {
        super(String.format("Cannot reach block %s", b.getStrId()));
    }
}

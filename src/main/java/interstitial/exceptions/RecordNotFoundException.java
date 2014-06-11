package interstitial.exceptions;

import survey.exceptions.SurveyException;

public class RecordNotFoundException extends SurveyException {
    public RecordNotFoundException() {
        super(String.format("Survey is currently uninitialized; try \"Preview HIT\" first."));
    }
}

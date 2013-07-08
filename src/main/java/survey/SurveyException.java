package survey;

/**
 * All survey exceptions inherit from this one.
 * 
 */
public abstract class SurveyException extends Exception {
    public SurveyException(String msg){
        super(msg);
    }
}

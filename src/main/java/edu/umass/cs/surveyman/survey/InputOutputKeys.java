package edu.umass.cs.surveyman.survey;

/**
 * Created by etosch on 7/11/16.
 */
public class InputOutputKeys {
    /**
     * The options column header/JSON key. Used for both input and output.
     */
    public static final String OPTIONS = "OPTIONS";
    /**
     * The correlation column header/JSON key. Used for both input and output.
     */
    public static final String CORRELATION = "CORRELATION";
    /**
     * The expected response column header/JSON key. If the question is not freetext, this is boolean-valued. Used for
     * both input and output.
     */
    public static final String ANSWER = "ANSWER";
    /**
     * The freetext column header/JSON key. The default value is {@code false}. Used for both input and output.
     */
    public static final String FREETEXT = "FREETEXT";
    /**
     * The ordered column header/JSON key. Used to indicate that answer options' order is semantically meaningful. The
     * default value is {@code false}. Used for both input and output.
     */
    public static final String ORDERED = "ORDERED";
    /**
     * The randomize column header/JSON key. Used to indicate that the options may be shuffled. The default value is
     * {@code true}. Used for both input and output.
     */
    public static final String RANDOMIZE = "RANDOMIZE";
    /**
     * The branch column/JSON key.
     */
    public static final String BRANCH = "BRANCH";
    /**
     * Whether breakoff is permitted for the survey as a whole. Used for both input and output.
     */
    public static final String BREAKOFF = "BREAKOFF";
    /**
     * The question column header/JSON key.
     */
    public static final String QUESTION = "QUESTION";
    /**
     * The block column header/JSON key.
     */
    public static final String BLOCK = "BLOCK";
    /**
     * The resource column header/JSON key.
     */
    @Deprecated
    public static final String RESOURCE = "RESOURCE";
    /**
     * The exclusive column header/JSON key. Used to indicate that answer options are mutually exclusive. The default
     * value is {@code true}. Used for both input and output.
     */
    public static final String EXCLUSIVE = "EXCLUSIVE";


    public static final String BRANCH_MAP = "BRANCH_MAP";
    public static final String RESPONSEID = "responseid";
    public static final String CLASSIFIER = "classifier";
    public static final String NUMANSWERED = "numanswered";
    public static final String SCORE = "score";
    public static final String THRESHOLD = "threshold";
    public static final String VALID = "valid";
    public static final String COEFFICIENTTYPE = "coefficientType";
    public static final String COEFFICIENTVALUE = "coefficientValue";
    public static final String COEFFICIENTPVALUE = "coefficientPValue";
    public static final String THINGA = "thingA";
    public static final String NUMSAMPLESA = "numSamplesA";
    public static final String THINGB = "thingB";
    public static final String NUMSAMPLESB = "numSamplesB";
    public static final String QTEXT = "qtext";
}

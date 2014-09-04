package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.*;
import java.util.regex.Pattern;


/**
 * The class representing a Question object. The Question object includes instructional "questions."
 */
public class Question extends SurveyObj{

    /**
     * Determines whether the input question id corresponds to a known custom question pattern. Custom questions are
     * those that the user plugs in after parsing the input survey. They typically encompass ad hoc data such as timing
     * information and custom freetext questions added in the course of debugging.
     *
     * @param quid The identifier of the question we are testing.
     * @return boolean indicating whether the question has a known custom question id pattern.
     */
    public static boolean customQuestion(String quid) {
        return quid.startsWith("custom") || quid.contains("-1");
    }

    public static class MalformedOptionException extends SurveyException {
        public MalformedOptionException(String msg) {
            super(msg);
        }
    }

    public static class OptionNotFoundException extends SurveyException {
        public OptionNotFoundException(String oid, String quid){
            super(String.format("Option %s not found in Question %s", oid, quid));
        }
    }

    /**
     * Unique question identifier. Typically generated upon parsing.
     */
    public String quid;
    /**
     * Data to be displayed when the user takes the survye.
     */
    public Component data;
    /**
     * Answer to the question, if it exists.
     */
    public Component answer;
    /**
     * Map from component identifiers to answer option objects ({@link edu.umass.cs.surveyman.survey.Component}).
     */
    public Map<String, Component> options;
    /**
     * Map from answer options to branch destinations ({@link edu.umass.cs.surveyman.survey.Block}). This may be left
     * empty if there is no branching.
     */
    public Map<Component, Block> branchMap = new HashMap<Component, Block>();
    /**
     * Source data line numbers corresponding to this question. Used for parsing and debugging.
     */
    public List<Integer> sourceLineNos = new ArrayList<Integer>();
    /**
     * Map from other input column headers to their values, when they exist for this question.
     */
    public Map<String, String> otherValues = new HashMap<String, String>();
    /**
     * The enclosing block for this question.
     */
    public Block block;
    /**
     * True if respondents may only answer one of the answer options.
     */
    public Boolean exclusive;
    /**
     * True if the answer options have a natural ordering (e.g., Likert scales).
     */
    public Boolean ordered;
    /**
     * True if the answer options may be randomized. If the ordered field is true, then there are only two possible
     * permutations. If the ordered field is false, there are factorial permutations in the number of options.
     */
    public Boolean randomize;
    /**
     * True if this question requires a text response.
     */
    public Boolean freetext;
    /**
     * Set if this question requires a text response and must conform to a regular expression.
     */
    public Pattern freetextPattern;
    /**
     * Set if this question requires a text response and should display example text.
     */
    public String freetextDefault;
    /**
     * Indicates whether respondents may submit their results immediately after answering this questions, regardless of
     * its position in the survey.
     */
    public boolean permitBreakoff = true;
    /**
     * A correlation label.
     */
    public String correlation = "";

    /**
     * Creates a question identifier corresponding to the input data location.
     * @param row This question's initial input row index.
     * @param col The question column index.
     * @return A unique identifier based on input location.
     */
    public static String makeQuestionId(int row, int col) {
        return String.format("q_%d_%d", row, col);
    }

    /**
     */
    public Question(int row, int col){
        this.quid = makeQuestionId(row, col);
    }

    /**
     * Creates a question whose identifier is based on the question's input location and whose associated data
     * {@link edu.umass.cs.surveyman.survey.Component} is {@param data}.
     *
     * @param data The data associated with this question.
     * @param row This question's initial input row index.
     * @param col The question column index.
     */
    public Question(String data, int row, int col) {
        this(row, col);
        if (HTMLComponent.isHTMLComponent(data))
            this.data = new HTMLComponent(data, row, col);
        else this.data = new StringComponent(data, row, col);
    }

    /**
     * Returns the answer option associated with this question having the input
     * {@link edu.umass.cs.surveyman.survey.Component} identifier.
     *
     * @param oid The input {@link edu.umass.cs.surveyman.survey.Component} identifier.
     * @return The appropriate {@link edu.umass.cs.surveyman.survey.Component} subclass.
     * @throws edu.umass.cs.surveyman.survey.Question.OptionNotFoundException if there is no answer option associated
     * with this question.
     */
    public Component getOptById(String oid) throws SurveyException {
        if (oid.equals("comp_-1_-1"))
            return null;
        if (options.containsKey(oid))
            return options.get(oid);
        throw new OptionNotFoundException(oid, this.quid);
    }

    /**
     * Returns a sorted array of the answer options.
     * @return {@link edu.umass.cs.surveyman.survey.Component} array of the answer options, sorted by their relative
     * indices.
     * @throws edu.umass.cs.surveyman.survey.Question.MalformedOptionException if there is an error with the options'
     * indices.
     */
    public Component[] getOptListByIndex() throws SurveyException {
        if (freetext) return new Component[0];
        Component[] opts = new Component[options.size()];
        for (Component c : options.values())
            if (c.index > options.size())
                throw new MalformedOptionException(String.format("Option \r\n{%s}\r\n has an index that exceeds max index %d"
                        , c.toString()
                        , options.size() - 1));
            else if (opts[c.index] != null)
                throw new MalformedOptionException(String.format("Options \r\n{%s}\r\n and \r\n{%s}\r\n have the same index. (Entries (%d, %d) and (%d, %d)."
                        , opts[c.index]
                        , c.toString()
                        , opts[c.index].getSourceRow(), opts[c.index].getSourceCol()
                        , c.getSourceRow(), c.getSourceCol()
                        )
                    );
            else
                opts[c.index] = c;
         return opts;
    }

    /**
     * Tests whether this question precedes the input question, according to their enclosing blocks. See
     * {@link edu.umass.cs.surveyman.survey.Block before}.
     *
     * @param q Question to compare.
     * @return {@code true} if the input question follows this question. {@code false} if randomization and/or partial
     * ordering cannot determine a strict ordering.
     */
    public boolean before(Question q) {
        int[] myBLockID = this.block.getBlockId();
        for (int i = 0 ; i < myBLockID.length ; i++) {
            if (i >= q.block.getBlockId().length)
                return false; // can't say it's strictly before
            else if (myBLockID[i] < q.block.getBlockId()[i])
                return true;
        }
        return false;
    }

    /**
     * Getter for the input source line.
     * @return {@code int} corresponding to the first input source line.
     */
    public int getSourceRow() {
        return Integer.parseInt(quid.split("_")[1]);
    }

    /**
     * Getter for the input source column. This should be the same for every question in a survey.
     * @return {@code int} corresponding to the QUESTION column.
     */
    public int getSourceCol() {
        return Integer.parseInt(quid.split("_")[2]);
    }

    /**
     * Returns a string of the question data.
     * @return String of the data field.
     */
    @Override
    public String toString() {
        return data.toString();
    }

    /**
     * Two questions are equal if the following are equal:
     * <p>
     *     <ul>
     *         <li>quid</li>
     *         <li>data</li>
     *         <li>option map</li>
     *         <li>enclosing block</li>
     *         <li>exclusive flag</li>
     *         <li>ordered flag</li>
     *         <li>randomize flag</li>
     *     </ul>
     * </p>
     * @param o
     * @return
     */
    @Override
    public boolean equals(Object o){
        assert(o instanceof Question);
        Question q = (Question) o;
        return ! this.quid.equals(AbstractParser.CUSTOM_ID)
                && this.data.equals(q.data)
                && this.options.equals(q.options)
                && this.block.equals(q.block)
                && this.exclusive.equals(q.exclusive)
                && this.ordered.equals(q.ordered)
                && this.randomize.equals(q.randomize);
    }

    /**
     * Hashcodes are computed from the quid.
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return this.quid.hashCode();
    }

}

package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.SurveyMan;
import edu.umass.cs.surveyman.analyses.OptTuple;
import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.exceptions.BranchException;
import edu.umass.cs.surveyman.input.exceptions.OptionException;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.exceptions.QuestionConsistencyException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;


/**
 * The class representing a Question object. The Question object includes instructional "questions."
 */
public class Question extends SurveyObj implements Serializable, Comparable {

    protected static int QUESTION_COL = 1;
    protected static int OPTION_COL = 2;
    protected static int nextRow = 1;

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

    /**
     * Determines whether the question id corresponds to a known custom question pattern. Custom questions are
     * those that the user plugs in after parsing the input survey. They typically encompass ad hoc data such as timing
     * information and custom freetext questions added in the course of debugging.
     *
     * @return boolean indicating whether the question has a known custom question id pattern.
     */
    public boolean isCustomQuestion() {
        return Question.customQuestion(this.id);
    }

    @Override
    public int compareTo(Object o)
    {
        if (o instanceof Question) {
            Question that = (Question) o;
            if (this.equals(that))
                return 0;
            else if (this.getSourceRow() == that.getSourceRow())
                throw new RuntimeException("Have two questions that are not equal to each other but have the same " +
                        "source row.");
            else if (this.getSourceRow() < that.getSourceRow())
                return -1;
            else return 1;
        } else throw new RuntimeException(String.format("Cannot compare %s with %s.",
                o.getClass().getName(), this.getClass().getName()));
    }

    /**
     * Thrown by the Parser. It should never be thrown when surveys are constructed programmatically.
     */
    public static class MalformedOptionException extends SurveyException {
        public MalformedOptionException(String msg) {
            super(msg);
        }
    }

    /**
     * Thrown whenever the user or system attempts to find an option by text, input cell, etc. and that option does
     * not exist for this question object.
     */
    public static class OptionNotFoundException extends SurveyException {
        public OptionNotFoundException(String oid, String quid){
            super(String.format("Option %s not found in Question %s", oid, quid));
        }
    }

    /**
     * Unique question identifier. Typically generated upon parsing.
     */
    public String id;

    /**
     * Data to be displayed when the user takes the survey.
     */
    public SurveyDatum data;

    /**
     * Answer to the question, if it exists.
     */
    public SurveyDatum answer;

    /**
     * Map from data identifiers to answer option objects ({@link SurveyDatum}).
     */
    public Map<String, SurveyDatum> options = new HashMap<String, SurveyDatum>();

    /**
     * Map from answer options to branch destinations ({@link edu.umass.cs.surveyman.survey.Block}). This may be left
     * empty if there is no branching.
     */
    protected BranchMap branchMap = new BranchMap();

    /**
     * Source data line numbers corresponding to this question. Used for parsing and debugging.
     */
    public final List<Integer> sourceLineNos = new ArrayList<Integer>();

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
    public Boolean randomize = true;

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
    private static String makeQuestionId(int row, int col) {
        return String.format("q_%d_%d", row, col);
    }

    protected Question(String data, int row, int col)
    {
        this(row, col);
        if (HTMLDatum.isHTMLComponent(data))
            this.data = new HTMLDatum(data, row, col, -1);
        else this.data = new StringDatum(data, row, col, -1);
        assert !this.sourceLineNos.contains(row) : String.format(
                "Question already contains data from row %d", row);
        this.sourceLineNos.add(row);
    }

    /**
     * Creates a question whose identifier is based on the question's input location and whose associated data
     * {@link SurveyDatum} is data.
     *
     * @param data The data associated with this question.
     * @param row This question's initial input row index.
     * @param col The question column index.
     * @return A new Question.
     */
    public static Question makeQuestion(String data, int row, int col) {
        return new Question(data, row, col);
    }

    protected Question(SurveyDatum data, int row, int col)
    {
        this(row, col);
        this.data = data;
        assert !this.sourceLineNos.contains(row) : String.format(
                "Question already contains data from row %d", row);
        this.sourceLineNos.add(row);
    }

    /**
     * Used by the {@link edu.umass.cs.surveyman.input} parsers.
     * @param data The parsed, internal representation of the data in a cell.
     * @param row The input row (literal or calculated, as with JSON).
     * @param col The input column (literal or calculated, as with JSON).
     */
    public static Question makeQuestion(SurveyDatum data, int row, int col) {
        return new Question(data, row, col);
    }

    public Question(SurveyDatum data) {
        this(data, data.getSourceRow(), data.getSourceCol());
    }

    private Question(int row, int col)
    {
        this.id = makeQuestionId(row, col);
    }

    /**
     * Constructor for the programmatic creation of questions.
     * @param data The data associated with this question.
     */
    public Question(String data)
    {
        this(data, Question.nextRow, Question.QUESTION_COL);
        Question.nextRow++;
    }

    protected int countLines()
    {
        int optLines = this.options.size();
        int retval = optLines == 0 ? 1 : optLines;
        assert retval == this.sourceLineNos.size() : String.format(
                "Lines computed: %d; lines stored: %d", retval, this.sourceLineNos.size());
        return retval;
    }

    protected void updateFromSurvey(Survey s)
    {
        assert !s.questions.contains(this);
        int otherRows = 0;
        for (Question q : s.questions) {
            otherRows += q.countLines();
        }
        this.id = makeQuestionId(otherRows + 1, Question.QUESTION_COL);
    }

    private void resetLineNosAndIds() throws SurveyException {
        this.id = makeQuestionId(Question.nextRow, QUESTION_COL);
        this.data.resetId(this.getSourceRow(), QUESTION_COL);
        int startingRow = this.getSourceRow();
        Map<String, SurveyDatum> newOptions = new HashMap<>();
        for (SurveyDatum c : this.getOptListByIndex()) {
            c.resetId(startingRow, Question.OPTION_COL);
            startingRow++;
            newOptions.put(c.getId(), c);
        }
        this.options = newOptions;
        this.sourceLineNos.clear();
        for (int i = 0; i < this.options.size(); i++)
            this.sourceLineNos.add(i+this.getSourceRow());
        Question.nextRow += this.options.size();
    }

    /**
     * Adds an option to the question.
     * @param surfaceText The answer option text to display.
     * @param exclusive Boolean indicating whether this should be a radio button question. Must agree with the default
     *                  question setting.
     * @param ordered Boolean indicating whether the answer options are ordered. Must agree with the default question
     *                setting.
     * @throws SurveyException
     */
    public void addOption(
            String surfaceText,
            boolean exclusive,
            boolean ordered)
            throws SurveyException
    {
        int sourceRow = this.getSourceRow() + this.options.size();
        if (this.exclusive!=null && this.exclusive!=exclusive)
            throw new QuestionConsistencyException(this, "exclusive", exclusive);
        if (this.ordered!=null && this.ordered!=ordered)
            throw new QuestionConsistencyException(this, "ordered", ordered);
        if (HTMLDatum.isHTMLComponent(surfaceText))
            this.addOption(new HTMLDatum(surfaceText, sourceRow, Question.OPTION_COL, this.options.size()),
                    exclusive, ordered);
        else this.addOption(new StringDatum(surfaceText, sourceRow, Question.OPTION_COL, this.options.size()),
                exclusive, ordered);
        this.freetext = false;
        resetLineNosAndIds();
    }

    /**
     * Add an option with the input surface text.
     * @param surfaceText The text this option should display.
     * @throws SurveyException
     */
    public void addOption(String surfaceText)
            throws SurveyException
    {
        boolean exclusive = this.exclusive == null ? true : this.exclusive;
        boolean ordered = this.ordered == null? false : this.ordered;
        addOption(surfaceText, exclusive, ordered);
    }

    /**
     * Adds options with the input surface text.
     * @param surfaceTexts The surface texts to display.
     * @throws SurveyException
     */
    public void addOptions(String... surfaceTexts)
            throws SurveyException
    {
        for (String s : surfaceTexts) {
            this.addOption(s);
        }
    }

    /**
     * Adds the input surveyDatum as an answer option to the question.
     * @param surveyDatum The option to add.
     * @param exclusive Boolean indicating whether this should be a radio button question. Must agree with the default
     *                  question setting.
     * @param ordered Boolean indicating whether the answer options are ordered. Must agree with the default question
     *                setting.
     * @throws SurveyException
     */
    public void addOption(
            SurveyDatum surveyDatum,
            boolean exclusive,
            boolean ordered)
            throws SurveyException
    {
        if (this.isBranchQuestion() || (this.block != null && this.block.branchParadigm.equals(Block.BranchParadigm.ALL)))
            throw new BranchException("This question is a branch question.");
        if (this.exclusive != null && this.exclusive != exclusive)
            throw new QuestionConsistencyException(this, "exclusive", exclusive);
        if (this.ordered != null && this.ordered != ordered)
            throw new QuestionConsistencyException(this, "ordered", ordered);
        if (this.options.containsKey(surveyDatum.getId()))
            SurveyMan.LOGGER.warn("Attempted to add option " + surveyDatum + " more than once.");
        else {
            surveyDatum.setIndex(this.options.size());
            this.options.put(surveyDatum.getId(), surveyDatum);
            this.sourceLineNos.add(surveyDatum.getSourceRow());
            nextRow += (surveyDatum.getSourceRow() - nextRow);
        }
        this.freetext = false;
        this.exclusive = exclusive;
        this.ordered = ordered;
        resetLineNosAndIds();
    }

    /**
     * Adds the input surveyDatum as an answer option to the question.
     * @param surveyDatum The option to add.
     * @throws SurveyException
     */
    public void addOption(
            SurveyDatum surveyDatum)
            throws SurveyException
    {
        boolean exclusive = this.exclusive == null ?  true : this.exclusive;
        boolean ordered = this.ordered == null ? false : this.ordered;
        this.addOption(surveyDatum, exclusive, ordered);
    }

    /**
     * Adds an answer option as part of a branch question.
     * @param surveyDatum The option to add.
     * @param branchTo The block this option points to.
     * @param exclusive Boolean indicating whether this should be a radio button question. Must agree with the default
     *                  question setting.
     * @param ordered Boolean indicating whether the answer options are ordered. Must agree with the default question
     *                setting.
     * @throws SurveyException
     */
    public void addOption(
            SurveyDatum surveyDatum,
            Block branchTo,
            boolean exclusive,
            boolean ordered)
            throws SurveyException
    {
        if (this.exclusive!=null && this.exclusive!=exclusive)
            throw new QuestionConsistencyException(this, "exclusive", exclusive);
        if (this.ordered!=null && this.ordered!=ordered)
            throw new QuestionConsistencyException(this, "ordered", ordered);
        if (this.block == null || this.isBranchQuestion() || this.block.branchParadigm.equals(Block.BranchParadigm.ALL)) {
            if (this.options.containsKey(surveyDatum.getId())) {
                throw new OptionException("Attempted to add option " + surveyDatum + " more than once.");
            }
            surveyDatum.setIndex(this.options.size());
            this.options.put(surveyDatum.getId(), surveyDatum);
            nextRow += (surveyDatum.getSourceRow() - nextRow);
            this.branchMap.put(surveyDatum, branchTo);
            this.freetext = false;
            this.ordered = ordered;
            this.exclusive = exclusive;
            this.resetLineNosAndIds();
        } else throw new BranchException("This question is not a branch question.");
    }

    /**
     * Adds an answer option as part of a branch question.
     * @param surveyDatum The option to add.
     * @param branchTo The block this option points to.
     * @throws SurveyException
     */
    public void addOption(
            SurveyDatum surveyDatum,
            Block branchTo)
            throws SurveyException
    {
        boolean exclusive = this.exclusive == null ? true : this.exclusive;
        boolean ordered = this.ordered == null ? false : this.ordered;
        this.addOption(surveyDatum, branchTo, exclusive, ordered);
    }

    /**
     * Loops through the branch destinations for this question and returns the set of blocks this question branches to.
     * If the question is not a branch question, returns an empty set.
     * @return The set of blocks this question branches to.
     */
    public Set<Block> getBranchDestinations() {
        Set<Block> retval = new HashSet<Block>();
        for (Block b : this.branchMap.values())
            retval.add(b);
        return retval;
    }

    /**
     * Tests whether this question is a branch question. Note that Branch-ALL types will return true here, even though
     * they are not "true" branch questions.
     * @return Boolean indicating whether this block is a branch question.
     */
    public boolean isBranchQuestion() {
        return !this.branchMap.isEmpty();
    }

    /**
     * Tests whether this question is instructional. In simulation, we do not need to provide answers to instructional
     * questions.
     * @return Boolean Indicates whether this question is instructional.
     */
    public boolean isInstructional() {
        return this.options.size() == 0 && !this.freetext;
    }

    /**
     * Returns the branch destination for the input question.
     * @param c The answer option whose branch destination we want to know.
     * @return The Block correponding to the branch destination.
     */
    public Block getBranchDest(
            SurveyDatum c)
            throws SurveyException
    {
        if (this.isBranchQuestion())
            return this.branchMap.get(c);
        else throw new BranchException("Not a branch question.");
    }

    /**
     * Sets the branch destination of this question at the input option to the input destination. Adds the input option
     * to this question's options, if it hasn't been added already. Resets the branch paradigm, if needed, and
     * propagates the branch paradigm.
     * @param c The answer option whose branch destination we need to set.
     * @param dest The branch destination for the input answer option.
     * @throws SurveyException
     */
    public void setBranchDest(
            SurveyDatum c,
            Block dest)
            throws SurveyException
    {
        if (!this.options.values().contains(c))
            this.addOption(c);
        this.branchMap.put(c, dest);

        if (this.block.branchQ == null || !this.block.branchQ.equals(this)) {
            this.block.branchQ = this;
            switch (this.block.branchParadigm) {
                case NONE:
                    this.block.updateBranchParadigm(Block.BranchParadigm.ONE); break;
                case ONE:
                    this.block.updateBranchParadigm(Block.BranchParadigm.ALL); break;
            }
        }
        this.block.propagateBranchParadigm();
    }

    /**
     * Returns the answer option associated with this question having the input
     * {@link SurveyDatum} identifier.
     *
     * @param oid The input {@link SurveyDatum} identifier.
     * @return The appropriate {@link SurveyDatum} subclass.
     * @throws edu.umass.cs.surveyman.survey.Question.OptionNotFoundException if there is no answer option associated
     * with this question.
     */
    public SurveyDatum getOptById(String oid)
            throws SurveyException
    {
        if (SurveyDatum.isCustomDatum(oid))
            return null;
        if (options.containsKey(oid))
            return options.get(oid);
        throw new OptionNotFoundException(oid, this.id);
    }

    /**
     * Returns a sorted array of the answer options.
     * @return {@link SurveyDatum} array of the answer options, sorted by their relative
     * indices.
     * @throws edu.umass.cs.surveyman.survey.Question.MalformedOptionException if there is an error with the options'
     * indices.
     */
    public SurveyDatum[] getOptListByIndex()
            throws SurveyException
    {
        if (freetext) return new SurveyDatum[0];
        SurveyDatum[] opts = new SurveyDatum[options.size()];
        for (SurveyDatum c : options.values())
            if (c.getIndex() > options.size())
                throw new MalformedOptionException(String.format("Option \r\n{%s}\r\n has an index that exceeds max index %d"
                        , c.toString()
                        , options.size() - 1));
            else if (opts[c.getIndex()] != null)
                throw new MalformedOptionException(String.format("Options \r\n{%s}\r\n and \r\n{%s}\r\n have the same index. " +
                        "(Entries (%d, %d) and (%d, %d) both have index %d)."
                        , opts[c.getIndex()]
                        , c.toString()
                        , opts[c.getIndex()].getSourceRow(), opts[c.getIndex()].getSourceCol()
                        , c.getSourceRow(), c.getSourceCol(), c.getIndex()
                        )
                    );
            else
                opts[c.getIndex()] = c;
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
    public boolean before(
            Question q)
    {
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
    public int getSourceRow()
    {
        return Integer.parseInt(id.split("_")[1]);
    }

    /**
     * Getter for the input source column. This should be the same for every question in a survey.
     * @return {@code int} corresponding to the QUESTION column.
     */
    public int getSourceCol()
    {
        return Integer.parseInt(id.split("_")[2]);
    }

    /**
     * For each question, returns the set of all equivalent questions (including itself).
     * @return The list of equivalent questions. If this question is not part of a set of variants, the function will
     * return a list containing just this question. If this question is a custom question, the function will return an
     * empty list.
     */
    public List<Question> getVariants()
    {
        List<Question> questions = new ArrayList<Question>();
        if (!customQuestion(this.id)) {
            if (this.block.branchParadigm == Block.BranchParadigm.ALL)
                return this.block.questions;
            else {
                questions.add(this);
            }
        }
        return questions;
    }

    private String getFreetextValue()
    {
        if ( this.freetextDefault != null )
            return String.format("\"%s\"", this.freetextDefault);
        else if ( this.freetextPattern != null )
            return String.format("\"#{%s}\"", this.freetextPattern.pattern());
        else return "true";
    }

    protected String jsonizeOrdering()
            throws SurveyException
    {
        List<String> options = new ArrayList<>();
        for (SurveyDatum opt : this.getOptListByIndex())
            options.add(String.format("\"%s\"", opt.getId()));
        return String.format("[ %s ]", StringUtils.join(options, ","));
    }

    protected String jsonize()
            throws SurveyException
    {

        String options = SurveyDatum.jsonize(Arrays.asList(this.getOptListByIndex()));
        String branchMap = this.branchMap.jsonize();
        StringBuilder otherStuff = new StringBuilder();

        if (options.equals(""))
            otherStuff.append(this.freetext ? String.format(", \"freetext\" : %s", this.getFreetextValue()) : "");
        else otherStuff.append(String.format(", \"options\" : %s", options));

        if (!branchMap.equals(""))
            otherStuff.append(String.format(", \"branchMap\" : %s ", branchMap));

        if (this.randomize != CSVParser.defaultValues.get(AbstractParser.RANDOMIZE).booleanValue())
            otherStuff.append(String.format(", \"randomize\" : %s", this.randomize));

        if (this.ordered != CSVParser.defaultValues.get(AbstractParser.ORDERED).booleanValue())
            otherStuff.append(String.format(", \"ordered\" : %s", this.jsonizeOrdering()));

        if (this.exclusive != CSVParser.defaultValues.get(AbstractParser.EXCLUSIVE).booleanValue())
            otherStuff.append(String.format(", \"exclusive\" : %s", this.exclusive));

        if (!this.permitBreakoff)
            otherStuff.append( ", \"breakoff\" : false");

        if (!this.correlation.equals(""))
            otherStuff.append(String.format(", \"correlation\" : \"%s\"", this.correlation));

        if (this.answer != null)
            otherStuff.append(String.format(", \"answer\" : \"%s\"", this.answer.getId()));

        return String.format("{ \"id\" : \"%s\", \"qtext\" : \"%s\" %s}"
                , this.id
                , SurveyDatum.html(this.data)
                , otherStuff.toString());
    }

    protected static String jsonize(List<Question> questionList) throws SurveyException {
        Iterator<Question> qs = questionList.iterator();
        if (!qs.hasNext())
            return "[]";
        StringBuilder s = new StringBuilder(qs.next().jsonize());
        while (qs.hasNext()) {
            Question q = qs.next();
            s.append(String.format(", %s", q.jsonize()));
        }
        return String.format("[ %s ]", s.toString());
    }

    private static void makeQuestions(Question[] questions, String... surfaceStrings)
    {
        assert questions.length == surfaceStrings.length;
        for (int i = 0; i < questions.length; i++) {
            questions[i] = new Question(surfaceStrings[i]);
        }
    }

    /**
     * Convenience method for quickly creating a series of questions with the default settings.
     * @param questions An array that will be populated with new question objects.
     * @param surfaceStrings The strings corresponding to the text the user will see.
     */
    public static void makeUnorderedRadioQuestions(
            Question[] questions,
            String... surfaceStrings)
    {
        makeQuestions(questions, surfaceStrings);
    }

    public static void makeOrderedRadioQuestions
            (Question[] questions, String... surfaceStrings)
    {
        makeQuestions(questions, surfaceStrings);
        for (Question q : questions) {
            q.ordered = true;
        }
    }

    public static void makeUnorderedCheckQuestions(
            Question[] questions, String... surfaceStrings)
    {
        makeQuestions(questions);
        for (Question q: questions) {
            q.exclusive = false;
        }
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
     *         <li>id</li>
     *         <li>data</li>
     *         <li>option map</li>
     *         <li>enclosing block</li>
     *         <li>exclusive flag</li>
     *         <li>ordered flag</li>
     *         <li>randomize flag</li>
     *     </ul>
     * </p>
     * @param o Another option.
     * @return Whether the input object is equal to this Question.
     */
    @Override
    public boolean equals(Object o){
        assert(o instanceof Question);
        Question q = (Question) o;
        return ! this.id.equals(AbstractParser.CUSTOM_ID)
                && this.data.equals(q.data)
                && this.options.equals(q.options)
                && this.block.equals(q.block)
                && this.exclusive.equals(q.exclusive)
                && this.ordered.equals(q.ordered)
                && this.randomize.equals(q.randomize);
    }

    /**
     * Hashcodes are computed from the id.
     * @return hashcode
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    /**
     * Converts a response to a double precision representation, for use in clustering.
     * @param opts The answer set to this question.
     * @param noise Boolean indicating whether we should add noise.
     * @return
     * @throws SurveyException
     */
    public double responseToDouble(List<OptTuple> opts, boolean noise)
            throws SurveyException
    {
        double score = -1;
        if (this.freetext) {
            //TODO(etosch): implement
            return -1;
        } else if (this.exclusive) {
            assert opts.size() == 1 : "An exclusive question (i.e., radio button) question cannot have more than one response";
            SurveyDatum c = opts.get(0).c;
            List<SurveyDatum> surveyData = Arrays.asList(this.getOptListByIndex());
            score = surveyData.indexOf(c);
        } else {
            // Get all of the possible options
            List<SurveyDatum> surveyData = Arrays.asList(this.getOptListByIndex());
            // Hold the answers in a set.
            Set<SurveyDatum> answers = new HashSet<>();
            // Add all of the answers actually chosen to the answer set.
            for (OptTuple optTuple : opts) {
                answers.add(optTuple.c);
            }
            assert answers.size() <= surveyData.size() : "Cannot choose more options than there are available.";
            int numOptions = surveyData.size();
            for (int i = 1; i < Math.ceil(Math.pow(2, numOptions)); i++) {
                char[] selector = new char[numOptions];
                char[] tmp = Integer.toBinaryString(i).toCharArray();
                // front pad
                for (int j = 0 ; j < selector.length - tmp.length; j++) {
                    selector[j] = '0';
                }
                for (int j = 0; j < tmp.length; j++) {
                    selector[j + (selector.length - tmp.length)] = tmp[j];
                }
                assert selector.length <= surveyData.size() : "Width of the selector array cannot be larger than the total possible answers.";
                // lack of padded zeros is equal to those indices set to 0...
                Set<SurveyDatum> possibleAnswerSet = new HashSet<>();
                for (int j = 0; j < selector.length; j++) {
                    if (selector[j] == '1') {
                        possibleAnswerSet.add(surveyData.get(j));
                    }
                }
                if (possibleAnswerSet.equals(answers))
                    score = i;
            }
        }
        if (score == -1)
            throw new RuntimeException("Never set score!");
        if (noise)
            return score + (QCMetrics.rng.nextGaussian() / 3);
        else return score;
    }

}

package edu.umass.cs.surveyman.input;

import edu.umass.cs.surveyman.input.exceptions.MalformedBooleanException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.simple.SimpleLoggerContext;
import org.apache.logging.log4j.spi.ExtendedLogger;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.*;

import static edu.umass.cs.surveyman.input.csv.CSVLexer.falseValues;
import static edu.umass.cs.surveyman.input.csv.CSVLexer.trueValues;

public abstract class AbstractParser {

    /**
     * Holds a map from the column name to its default value.
     */
    public static final HashMap<String, Boolean> defaultValues = new HashMap<String, Boolean>();
    /**
     * The question column header/JSON key.
     */
    public static final String QUESTION = "QUESTION";
    /**
     * The block column header/JSON key.
     */
    public static final String BLOCK = "BLOCK";
    /**
     * The options column header/JSON key.
     */
    public static final String OPTIONS = "OPTIONS";
    /**
     * The resource column header/JSON key.
     */
    @Deprecated
    public static final String RESOURCE = "RESOURCE";
    /**
     * The exclusive column header/JSON key. Used to indicate that answer options are mutually exclusive. The default value is {@code true}.
     */
    public static final String EXCLUSIVE = "EXCLUSIVE";
    /**
     * The ordered column header/JSON key. Used to indicate that answer options' order is semantically meaningful. The default value is {@code false}.
     */
    public static final String ORDERED = "ORDERED";
    /**
     * The randomize column header/JSON key. Used to indicate that the options may be shuffled. The default value is {@code true}.
     */
    public static final String RANDOMIZE = "RANDOMIZE";
    /**
     * The branch column/JSON key.
     */
    public static final String BRANCH = "BRANCH";
    /**
     * The freetext column header/JSON key. The default value is {@code false}.
     */
    public static final String FREETEXT = "FREETEXT";
    /**
     * The correlation column header/JSON key.
     */
    public static final String CORRELATION = "CORRELATION";
    /**
     * The expected response column header/JSON key. If the question is not freetext, this is boolean-valued.
     */
    public static final String ANSWER = "ANSWER";
    /**
     * The schema against which JSON input is validated.
     */
    public static final String INPUT_SCHEMA = "http://surveyman.github.io/Schemata/survey_input.json";
//    public static final String CONDITION = "CONDITION";
    /**
     * An array of semantically meaningful column headers.
     */
    public static final String[] knownHeaders = {QUESTION, BLOCK, OPTIONS, RESOURCE, EXCLUSIVE, ORDERED, RANDOMIZE, BRANCH, FREETEXT, CORRELATION, ANSWER};
    /**
     * The identifier used for ad hoc responses (e.g., timing information).
     */
    public static final String CUSTOM_ID = "q_-1_-1";

    protected final static ExtendedLogger LOGGER = new SimpleLoggerContext().getLogger(AbstractParser.class.getName());

    static {
        defaultValues.put(EXCLUSIVE, true);
        defaultValues.put(ORDERED, false);
        defaultValues.put(RANDOMIZE, true);
        defaultValues.put(FREETEXT, false);
    }

    /**
     * The top-level blocks in the survey.
     */
    protected List<Block> topLevelBlocks = new ArrayList<Block>();
    /**
     * All blocks in the survey (including sub-blocks).
     */
    protected Map<String, Block> allBlockLookUp = new HashMap<String, Block>();
    /**
     * A map from correlation labels to their associated set of questions.
     */
    protected Map<String, List<Question>> correlationMap = new HashMap<String, List<Question>>();

    /**
     * Returns the internal boolean representation of a boolean-valued input to CSV or JSON.
     * @param thing The raw text being parsed.
     * @param col The associated column/key (used for debugging).
     * @return Internal boolean representation of the input data.
     * @throws SurveyException
     */
    protected static boolean boolType(String thing, String col) throws SurveyException {
        if (Arrays.asList(trueValues).contains(thing.toLowerCase()))
            return true;
        else if (Arrays.asList(falseValues).contains(thing.toLowerCase()))
            return false;
        else throw new MalformedBooleanException(thing, col);
    }

    /**
     * Parses the boolean input and ensures that boolean values are consistent across option entries.
     * @param bool The Boolean whose data must be consistent across entries.
     * @param col The column name.
     * @param entry The data in that column.
     * @param lineNo The line number (row) of the data.
     * @param colNo The column position in the input.
     * @return Boxed Boolean representation of the input boolean data.
     * @throws SurveyException
     */
    protected static Boolean parseBool(Boolean bool, String col, String entry, int lineNo, int colNo) throws SurveyException {
        //String thing = stripQuots(entry.contents.trim()).trim();
        String thing = entry;
        if (bool==null)
            return boolType(thing, col);
        else {
            boolean actual = boolType(thing, col);
            if (bool.booleanValue() != actual) {
                throw new MalformedBooleanException(String.format("Inconsistent boolean values; Expected %b in %s. Got %b (%d, %d)."
                        , bool
                        , actual
                        , lineNo
                        , colNo)
                        , col);
            }
        }
        return bool;
    }

    /**
     * Parses Question and Option column data into the internal representation.
     * @param contents The input data.
     * @param row The line number (row) of the data.
     * @param col The column position of the data.
     * @return {@link edu.umass.cs.surveyman.survey.Component} internal representation of the data.
     */
    public static Component parseComponent(String contents, int row, int col) {
        if (HTMLComponent.isHTMLComponent(contents))
            return new HTMLComponent(contents, row, col);
        else return new StringComponent(contents, row, col);
    }

    /**
     * Creates intermediate blocks that may not have been explicitly specified. For example, a CSV following the
     * numbering scheme 1.1, 1.2, 2.1, 2.2, ... will not have explicit top-level blocks. This method will generate
     * blocks 1, 2, ... etc. All parent/child references are validated and/or set here.
     * @param blockLookUp Map of all blocks.
     */
    protected static void addPhantomBlocks(Map<String, Block> blockLookUp) {
        Deque<String> blockIDs = new LinkedList<String>();
        for (String key : blockLookUp.keySet())
            blockIDs.add(key);
        while (!blockIDs.isEmpty()) {
            String nextId = blockIDs.pop();
            Block currentBlock = blockLookUp.get(nextId);
            if (currentBlock.getBlockDepth() > 1) {
                String parentStrId = currentBlock.getParentStrId();
                if (!parentStrId.equals("") && !blockLookUp.containsKey(parentStrId)) {
                    // create parent block and add to iterator and to the map
                    Block b = new Block(parentStrId);
                    currentBlock.parentBlock = b;
                    blockIDs.addLast(b.getStrId());
                    blockLookUp.put(b.getStrId(), b);
                }
            }
        }
    }

    /**
     * Traverses the structure of the block and its sub-blocks to ensure that the block/branch type is properly set.
     * See {@link edu.umass.cs.surveyman.survey.Block.BranchParadigm} for a detailed explanation of the block/branch
     * type lattice.
     * @param b A block whose type needs to be set.
     * @throws SurveyException
     */
    protected void propagateBranchParadigm(Block b) throws SurveyException {
        if (b.subBlocks.isEmpty())
            b.propagateBranchParadigm();
        for (Block bb : b.subBlocks)
            propagateBranchParadigm(bb);
    }

    /**
     * Iterates over the top-level blocks of the survey and calls
     * {@link edu.umass.cs.surveyman.input.AbstractParser#propagateBranchParadigm(edu.umass.cs.surveyman.survey.Block)}.
     * See {@link edu.umass.cs.surveyman.survey.Block.BranchParadigm} for a detailed explanation of the block/branch
     * type lattice.
     * @param survey A block whose type needs to be set.
     * @throws SurveyException
     */
    protected void propagateBranchParadigms(Survey survey) throws SurveyException {
        for (Block b : survey.topLevelBlocks)
            propagateBranchParadigm(b);
    }

    /**
     * Initializes a block-less survey as a survey of one block.
     * @param survey A flat, fully randomizable survey.
     */
    protected void initializeAllOneBlock(Survey survey){
        Block block = new Block("1");
        block.questions = survey.questions;
        topLevelBlocks.add(block);
        survey.blocks.put("1", block);
        for (Question q : survey.questions) {
            q.block = block;
        }
    }

    /**
     * The main entry point.
     * @return The internal representation of the survey.
     * @throws SurveyException
     */
    public abstract Survey parse() throws SurveyException;

}

package edu.umass.cs.surveyman.input;

import edu.umass.cs.surveyman.input.exceptions.MalformedBooleanException;
import org.apache.logging.log4j.simple.SimpleLoggerContext;
import org.apache.logging.log4j.spi.ExtendedLogger;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.*;

import static edu.umass.cs.surveyman.input.csv.CSVLexer.falseValues;
import static edu.umass.cs.surveyman.input.csv.CSVLexer.trueValues;

/**
 *  This class implements general parsing utility methods and contains high-level input to survey abstractions.
 */
public abstract class AbstractParser {

    /**
     * Holds a map from the column name to its default value.
     */
    public static final HashMap<String, Boolean> defaultValues = new HashMap<>();
    /**
     * The schema against which JSON input is validated.
     */
    public static final String INPUT_SCHEMA = "http://surveyman.github.io/Schemata/survey_input.json";
//    public static final String CONDITION = "CONDITION";
    /**
     * An array of semantically meaningful column headers.
     */
    public static final String[] knownHeaders = {InputOutputKeys.QUESTION, InputOutputKeys.BLOCK, InputOutputKeys.OPTIONS, InputOutputKeys.RESOURCE, InputOutputKeys.EXCLUSIVE, InputOutputKeys.ORDERED, InputOutputKeys.RANDOMIZE, InputOutputKeys.BRANCH, InputOutputKeys.FREETEXT, InputOutputKeys.CORRELATION, InputOutputKeys.ANSWER};
    /**
     * The identifier used for ad hoc responses (e.g., timing information).
     */
    public static final String CUSTOM_ID = "q_-1_-1";

    protected final static ExtendedLogger LOGGER = new SimpleLoggerContext().getLogger(AbstractParser.class.getName());

    static {
        defaultValues.put(InputOutputKeys.EXCLUSIVE, true);
        defaultValues.put(InputOutputKeys.ORDERED, false);
        defaultValues.put(InputOutputKeys.RANDOMIZE, true);
        defaultValues.put(InputOutputKeys.FREETEXT, false);
        defaultValues.put(InputOutputKeys.BREAKOFF, true);
    }

    /**
     * The top-level blocks in the survey.
     */
    protected List<Block> topLevelBlocks = new ArrayList<>();
    /**
     * All blocks in the survey (including sub-blocks).
     */
    protected Map<String, Block> allBlockLookUp = new HashMap<>();
    /**
     * A map from correlation labels to their associated set of questions.
     */
    protected Map<String, List<Question>> correlationMap = new HashMap<>();

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
        if (bool==null)
            return boolType(entry, col);
        else {
            boolean actual = boolType(entry, col);
            if (actual != bool) {
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
     * @return {@link SurveyDatum} internal representation of the data.
     */
    public static SurveyDatum parseComponent(String contents, int row, int col, int index) {
        if (HTMLDatum.isHTMLComponent(contents))
            return new HTMLDatum(contents, row, col, index);
        else return new StringDatum(contents, row, col, index);
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
                    blockIDs.addLast(b.getId());
                    blockLookUp.put(b.getId(), b);
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

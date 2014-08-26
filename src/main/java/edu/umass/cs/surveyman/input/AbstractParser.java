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

    public static final HashMap<String, Boolean> defaultValues = new HashMap<String, Boolean>();
    public static final String QUESTION = "QUESTION";
    public static final String BLOCK = "BLOCK";
    public static final String OPTIONS = "OPTIONS";
    public static final String RESOURCE = "RESOURCE";
    public static final String EXCLUSIVE = "EXCLUSIVE";
    public static final String ORDERED = "ORDERED";
    public static final String RANDOMIZE = "RANDOMIZE";
    public static final String BRANCH = "BRANCH";
    public static final String FREETEXT = "FREETEXT";
    public static final String CORRELATION = "CORRELATION";
    public static final String ANSWER = "ANSWER";
    public static final String INPUT_SCHEMA = "http://surveyman.github.io/Schemata/survey_input.json";
//    public static final String CONDITION = "CONDITION";
    public static final String[] knownHeaders = {QUESTION, BLOCK, OPTIONS, RESOURCE, EXCLUSIVE, ORDERED, RANDOMIZE, BRANCH, FREETEXT, CORRELATION, ANSWER};
    public static final String CUSTOM_ID = "q_-1_-1";

    protected final static ExtendedLogger LOGGER = new SimpleLoggerContext().getLogger(AbstractParser.class.getName());

    static {
        defaultValues.put(EXCLUSIVE, true);
        defaultValues.put(ORDERED, false);
        defaultValues.put(RANDOMIZE, true);
        defaultValues.put(FREETEXT, false);
    }

    protected List<Block> topLevelBlocks = new ArrayList<Block>();
    protected Map<String, Block> allBlockLookUp = new HashMap<String, Block>();
    protected Map<String, List<Question>> correlationMap = new HashMap<String, List<Question>>();


    protected static boolean boolType(String thing, String col, AbstractParser parser) throws SurveyException {
        if (Arrays.asList(trueValues).contains(thing.toLowerCase()))
            return true;
        else if (Arrays.asList(falseValues).contains(thing.toLowerCase()))
            return false;
        else {
            SurveyException e = new MalformedBooleanException(thing, col);
            throw e;
        }
    }

    protected static Boolean parseBool(Boolean bool, String col, String entry, int lineNo, int colNo, AbstractParser parser) throws SurveyException {
        //String thing = stripQuots(entry.contents.trim()).trim();
        String thing = entry;
        if (bool==null)
            return boolType(thing, col, parser);
        else {
            boolean actual = boolType(thing, col, parser);
            if (bool.booleanValue() != actual) {
                SurveyException e = new MalformedBooleanException(String.format("Inconsistent boolean values; Expected %b in %s. Got %b (%d, %d)."
                        , bool
                        , actual
                        , lineNo
                        , colNo)
                        , col);
                throw e;
            }
        }
        return bool;
    }

    public static Component parseComponent(String contents, int row, int col) {
        if (HTMLComponent.isHTMLComponent(contents))
            return new HTMLComponent(contents, row, col);
        else return new StringComponent(contents, row, col);
    }


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

    protected void propagateBranchParadigm(Block b) throws SurveyException {
        if (b.subBlocks.isEmpty())
            b.propagateBranchParadigm();
        for (Block bb : b.subBlocks)
            propagateBranchParadigm(bb);
    }

    protected void propagateBranchParadigms(Survey survey) throws SurveyException {
        for (Block b : survey.topLevelBlocks)
            propagateBranchParadigm(b);
    }

    protected void initializeAllOneBlock(Survey survey){
        Block block = new Block("1");
        block.questions = survey.questions;
        topLevelBlocks.add(block);
        survey.blocks.put("1", block);
        for (Question q : survey.questions) {
            q.block = block;
        }
    }

    public abstract Survey parse() throws SurveyException;

}

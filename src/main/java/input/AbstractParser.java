package input;

import input.exceptions.MalformedBooleanException;
import org.apache.log4j.Logger;
import survey.*;

import java.util.*;

import static input.csv.CSVLexer.falseValues;
import static input.csv.CSVLexer.trueValues;

public abstract class AbstractParser {

    public final static HashMap<String, Boolean> defaultValues = new HashMap<String, Boolean>();
    protected final static Logger LOGGER = Logger.getLogger(AbstractParser.class);

    static {
        defaultValues.put(Survey.EXCLUSIVE, true);
        defaultValues.put(Survey.ORDERED, false);
        defaultValues.put(Survey.RANDOMIZE, true);
        defaultValues.put(Survey.FREETEXT, false);
    }

    protected List<Block> topLevelBlocks = new ArrayList<Block>();
    protected Map<String, Block> allBlockLookUp = new HashMap<String, Block>();
    protected Map<String, List<Question>> correlationMap = new HashMap<String, List<Question>>();


    protected static boolean boolType(String thing, String col, AbstractParser parser) throws SurveyException{
        if (Arrays.asList(trueValues).contains(thing.toLowerCase()))
            return true;
        else if (Arrays.asList(falseValues).contains(thing.toLowerCase()))
            return false;
        else {
            SurveyException e = new MalformedBooleanException(thing, col, parser, parser.getClass().getEnclosingMethod());
            LOGGER.fatal(e);
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
                        , col
                        , parser
                        , parser.getClass().getEnclosingMethod());
                LOGGER.fatal(e);
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

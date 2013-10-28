package csv;

import static csv.CSVLexer.*;

import survey.*;
import system.Bug;
import system.Debugger;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import system.Library;

public class CSVParser {

    /** Inner/nested classes*/
    static class MalformedBlockException extends SurveyException implements Bug {
        Object caller;
        Method lastAction;
        public MalformedBlockException(String strId, CSVParser caller, Method lastAction) {
            super(String.format("Malformed block identifier: %s", strId));
            this.caller = caller;
            this.lastAction = lastAction;
            Debugger.addBug(this);
        }
        public Object getCaller() {
            return caller;
        }
        public Method getLastAction() {
            return lastAction;
        }
    }
    static class MalformedOptionException extends SurveyException implements Bug{
        Object caller;
        Method lastAction;
        public MalformedOptionException(String optString, CSVParser caller, Method lastAction) {
            super(String.format("%s has unknown formatting. See documentation for permitted formatting.", optString));
            this.caller = caller;
            this.lastAction = lastAction;
            Debugger.addBug(this);
        }

        @Override
        public Object getCaller() {
            return caller;
        }

        @Override
        public Method getLastAction() {
            return lastAction;
        }
    }
    static class MalformedBooleanException extends SurveyException implements Bug{
        Object caller;
        Method lastAction;
        public MalformedBooleanException(String boolStr, CSVParser caller, Method lastAction) {
            super(String.format("Unrecognized boolean string (%s). See the SurveyMan wiki for accepted strings.", boolStr));
            this.caller = caller;
            this.lastAction = lastAction;
            Debugger.addBug(this);
        }

        @Override
        public Object getCaller() {
            return caller;
        }

        @Override
        public Method getLastAction() {
            return lastAction;
        }
    }
    public static class SyntaxException extends SurveyException implements Bug{
        Object caller;
        Method lastAction;
        public SyntaxException(String msg, Object caller, Method lastAction) {
            super(msg);
            this.caller = caller;
            this.lastAction = lastAction;
            Debugger.addBug(this);
        }

        @Override
        public Object getCaller() {
            return lastAction;
        }

        @Override
        public Method getLastAction() {
            return lastAction;
        }
    }
    public static class BranchException extends SurveyException implements Bug{
        Object caller;
        Method lastAction;
        public BranchException(String fromBlockId, String toBlockId, CSVParser caller, Method lastAction) {
            super(String.format("Cannot jump from block %s to block %s. Must always jump into a block whose outermost numbering is greater."
                    , fromBlockId, toBlockId));
            this.caller = caller;
            this.lastAction = lastAction;
            Debugger.addBug(this);
        }
        public BranchException(String msg, CSVParser caller, Method lastAction) {
            super(msg);
            this.caller = caller;
            this.lastAction = lastAction;
        }

        @Override
        public Object getCaller() {
            return caller;
        }

        @Override
        public Method getLastAction() {
            return lastAction;
        }
    }


    /** static fields */
    public static HashMap<String, Boolean> defaultValues = new HashMap<String, Boolean>();
    static {
        defaultValues.put(Survey.EXCLUSIVE, true);
        defaultValues.put(Survey.ORDERED, false);
        defaultValues.put(Survey.RANDOMIZE, true);
        defaultValues.put(Survey.FREETEXT, false);
    }
    final private static Logger LOGGER = Logger.getLogger(CSVParser.class);

    /** instance fields */
    private HashMap<String, ArrayList<CSVEntry>> lexemes = null;
    private String[] headers;
    private final CSVLexer csvLexer;
    private List<Block> topLevelBlocks = new ArrayList<Block>();
    private Map<String, Block> allBlockLookUp = null;
    private Map<String, List<Question>> correlationMap = new HashMap<String, List<Question>>();

    /** constructors */
    public CSVParser(CSVLexer lexer){
        this.lexemes = lexer.entries;
        this.headers = lexer.headers;
        this.csvLexer = lexer;
    }

    /** static methods */

    private static boolean boolType(String thing, CSVParser parser) throws SurveyException{
        if (Arrays.asList(trueValues).contains(thing.toLowerCase()))
            return true;
        else if (Arrays.asList(falseValues).contains(thing.toLowerCase()))
            return false;
        else {
            SurveyException e = new MalformedBooleanException(thing, parser, parser.getClass().getEnclosingMethod());
            LOGGER.fatal(e);
            throw e;
        }
    }
    
    private static Boolean parseBool(Boolean bool, CSVEntry entry, CSVParser parser) throws SurveyException {
        //String thing = stripQuots(entry.contents.trim()).trim();
        String thing = entry.contents;
        if (bool==null)
            return boolType(thing, parser);
        else {
            boolean actual = boolType(thing, parser);
            if (bool.booleanValue() != actual) {
                SurveyException e = new MalformedBooleanException(String.format("Inconsistent boolean values; Expected %b. Got %b (%d, %d)."
                            , bool.booleanValue()
                            , actual
                            , entry.lineNo
                            , entry.colNo)
                    , parser
                    , parser.getClass().getEnclosingMethod());
                LOGGER.fatal(e);
                throw e;
            }
        }
        return bool;
    }

    private static Boolean assignBool(Boolean bool, String colName, int i, CSVParser parser) throws SurveyException {
        HashMap<String, ArrayList<CSVEntry>> lexemes = parser.lexemes;
        ArrayList<CSVEntry> thisCol = lexemes.get(colName);
        // if this column doesn't exist, set it to be the default value
        if (thisCol==null || thisCol.size()==0)
            return defaultValues.get(colName);
        else {
            CSVEntry entry = thisCol.get(i);
            // if the user skipped this column, set to be the default entry
            if (entry==null || entry.contents==null || entry.contents.equals("")) {
                LOGGER.warn(String.format("Supplying default entry for column %s in cell (%d,%d)"
                        , colName
                        , entry.lineNo
                        , entry.colNo));
                return defaultValues.get(colName);
            } else return parseBool(bool, entry, parser);
        }
    }

    /** instance methods */
    public List<Block> getTopLevelBlocks() {
        return topLevelBlocks;
    }

    public Map<String, Block> getAllBlockLookUp() {
        return allBlockLookUp;
    }

    private void unifyBranching(Survey survey) throws SurveyException {
        // grab the branch column from lexemes
        // find the block with the corresponding blockid
        // put the cid and block into the
        ArrayList<CSVEntry> branches = lexemes.get(Survey.BRANCH);
        if (!(branches==null || branches.isEmpty())) {
            for (CSVEntry entry : branches){
                if (!(entry==null || entry.contents==null || entry.contents.equals(""))) {
                    Question question = survey.getQuestionByLineNo(entry.lineNo);
                    // set this question's block's branchQ equal to this question
                    if (question.block.branchQ!=null) {
                        if (!question.quid.equals(question.block.branchQ.quid)) {
                            SurveyException e = new BranchException(String.format("Only permitted one branch per block. Error in block %s", entry.contents)
                                , this
                                , this.getClass().getEnclosingMethod());
                            LOGGER.warn(e);
                            throw e;
                        }
                    } else question.block.branchQ = question;
                    // get component of the option
                    CSVEntry option = lexemes.get(Survey.OPTIONS).get(branches.indexOf(entry));
                    Component c = question.getOptById(Component.makeComponentId(option.lineNo, option.colNo));
                    Block b = allBlockLookUp.get(entry.contents);
                    if (b==null) {
                        SurveyException e = new SyntaxException(String.format("Branch to block (%s) at line %d matches no known block (to question error)."
                                , entry.contents
                                , entry.lineNo)
                            , this
                            , this.getClass().getEnclosingMethod());
                        LOGGER.warn(e);
                        throw e;
                    }
                    question.branchMap.put(c, b);
                }   
            }
        }
    }

    private boolean newQuestion(CSVEntry question, CSVEntry option, Question tempQ, int i) throws SurveyException{
        // checks for well-formedness and returns true if we should set tempQ to a new question
        if (question.lineNo != option.lineNo) {
            SurveyException e = new SyntaxException("CSV entries not properly aligned.", this, this.getClass().getEnclosingMethod());
            LOGGER.fatal(e);
            throw e;
        }
        if ( tempQ == null && "".equals(question.contents) ){
            SurveyException e = new SyntaxException("No question indicated.", this, this.getClass().getEnclosingMethod());
            LOGGER.fatal(e);
            throw e;
        }
        if (tempQ != null && (question.contents==null || question.contents.equals(""))) {
            // then this line should include only options.
            for (String key: lexemes.keySet()) {
                if (! (key.equals(Survey.OPTIONS) || key.equals(Survey.BRANCH))) {
                    CSVEntry entry = lexemes.get(key).get(i);
                    if (! (entry.contents==null || entry.contents.trim().equals(""))) {
                        SurveyException e = new SyntaxException(String.format("Entry in cell (%d,%d) (column %s) is %s; was expected to be empty"
                                , entry.lineNo
                                , entry.colNo
                                , key
                                , entry.contents)
                            , this
                            , this.getClass().getEnclosingMethod());
                        LOGGER.fatal(e);
                        throw e;
                    }
                }
            }
            // will be using the tempQ from the previous question
            return false;
        }
        else return true;
    }

    private ArrayList<Question> unifyQuestions() throws MalformedURLException, SurveyException {
        
        Question tempQ = null;
        ArrayList<Question> qlist = new ArrayList<Question>();
        ArrayList<CSVEntry> questions = lexemes.get(Survey.QUESTION);
        ArrayList<CSVEntry> options = lexemes.get(Survey.OPTIONS);
        ArrayList<CSVEntry> resources = (lexemes.containsKey(Survey.RESOURCE)) ? lexemes.get(Survey.RESOURCE) : null;
        ArrayList<CSVEntry> correlates = (lexemes.containsKey(Survey.CORRELATION)) ? lexemes.get(Survey.CORRELATION) : null;
        
        int index = 0;
        
        for (int i = 0; i < questions.size() ; i++) {
            
            CSVEntry question = questions.get(i);
            CSVEntry option = options.get(i);
            
            LOGGER.log(Level.INFO, tempQ+"Q:"+question.contents+"O:"+option.contents);
            if (newQuestion(question, option, tempQ, i)) {
                tempQ = new Question(question.lineNo, question.colNo);
                tempQ.data.add(parseComponent(question, tempQ.data.size()));
                tempQ.options =  new HashMap<String, Component>();
                tempQ.index = index;
                qlist.add(tempQ);
                index++;
            }
            if (resources != null && resources.get(i).contents!=null) {
                CSVEntry resource = resources.get(i);
                String potentialURL = resource.contents.trim();
                if (!potentialURL.equals(""))
                    tempQ.data.add(new URLComponent(potentialURL, resource.lineNo, resource.colNo));
            }            // add this line number to the question's lineno list
            if (correlates != null && correlates.get(i).contents!=null) {
                CSVEntry correlation = correlates.get(i);
                if (correlationMap.containsKey(correlation.contents)){
                  List<Question> qs = correlationMap.get(correlation.contents);
                  qs.add(tempQ);
                } else correlationMap.put(correlation.contents, Arrays.asList(new Question[]{ tempQ }));
            }
            tempQ.options.put(Component.makeComponentId(option.lineNo, option.colNo), parseComponent(option, tempQ.options.size()));
            tempQ.sourceLineNos.add(option.lineNo);
            //assign boolean question fields
            tempQ.exclusive = assignBool(tempQ.exclusive, Survey.EXCLUSIVE, i, this);
            tempQ.ordered = assignBool(tempQ.ordered, Survey.ORDERED, i, this);
            tempQ.randomize = assignBool(tempQ.randomize, Survey.RANDOMIZE, i, this);
            tempQ.freetext = assignBool(tempQ.freetext, Survey.FREETEXT, i, this);
            if (tempQ.freetext)
                tempQ.options.put(Survey.FREETEXT, new StringComponent("", option.lineNo, option.colNo));
            if (tempQ.otherValues.isEmpty())
                for (String col : headers) {
                    boolean known = false;
                    for (int j = 0 ; j < Survey.knownHeaders.length ; j++)
                        if (Survey.knownHeaders[j].equals(col)){
                            known = true; break;
                        }
                    if (! known) {
                        String val = lexemes.get(col).get(i).contents;
                        tempQ.otherValues.put(col, val);
                    }
                }
        }
        
        return qlist;
        
    }

    public static Component parseComponent(CSVEntry csvEntry, int index) {
        Component c = parseComponent(csvEntry.contents, csvEntry.lineNo, csvEntry.colNo);
        c.index = index;
        return c;
    }

    public static Component parseComponent(String contents, int row, int col) {
        try {
            return new URLComponent(contents, row, col);
        } catch (MalformedURLException e) {
            return new StringComponent(contents, row, col);
        }
    }

    private static int[] getBlockIdArray (String contents) {
         String[] pieces = new String[1];
         if (contents.contains("."))
            pieces = contents.split("\\.");
         else pieces[0] = contents;
         int[] id = new int[pieces.length];
         for (int i = 0 ; i < pieces.length ; i ++) {
             String piece = pieces[i].replace("_", "");
             id[i] = Integer.parseInt(piece);
         }
         return id;
    }
    
    private void setBlockMaps(Map<String, Block> blockLookUp, List<Block> topLevelBlocks) {
        // first create a flat map of all the blocks;
        // the goal is to unify the list of block ids
        if (lexemes.containsKey(Survey.BLOCK)) {
            Block tempB = null;
            for (CSVEntry entry : lexemes.get(Survey.BLOCK)) {
                if (entry.contents==null || entry.contents.length()==0) {
                    // this line belongs to the last parsed block
                    tempB.sourceLines.add(entry.lineNo);
                } else {
                    if (blockLookUp.containsKey(entry.contents)) {
                        tempB = blockLookUp.get(entry.contents);
                        tempB.sourceLines.add(entry.lineNo);
                    } else {
                        tempB = new Block();
                        tempB.strId = entry.contents;
                        tempB.setRandomizeFlagToTrue();
                        if (entry.contents.startsWith("_")) tempB.setRandomizeFlagToTrue();
                        tempB.sourceLines.add(entry.lineNo);
                        tempB.setIdArray(getBlockIdArray(entry.contents));
                        // if top-level, add to topLevelBlocks
                        if (tempB.isTopLevel()) topLevelBlocks.add(tempB);
                        blockLookUp.put(entry.contents, tempB);
                    }
                }
            }
        }
    }
             
    private ArrayList<Block> initializeBlocks() throws SurveyException{
        Map<String, Block> blockLookUp = new HashMap<String, Block>();
        setBlockMaps(blockLookUp, topLevelBlocks);
        allBlockLookUp = new HashMap<String, Block>(blockLookUp);
        // now create the heirarchical structure of the blocks
        ArrayList<Block> blocks = (ArrayList<Block>) topLevelBlocks;
        int currentDepth = 1;
        while (! blockLookUp.isEmpty()) {
            Iterator<String> itr = blockLookUp.keySet().iterator();
            while(itr.hasNext()) {
                String strId = itr.next();
                if (topLevelBlocks.contains(blockLookUp.get(strId))) {
                    itr.remove();
                    blockLookUp.remove(strId);
                } else {
                    // this is not a top-level block.
                    Block block = blockLookUp.get(strId);
                    LOGGER.debug(block);
                    // if this block is at the current level of interest
                    if (block.getBlockDepth() == currentDepth + 1) {
                        String parentBlockStr = Block.idToString(block.parentBlockID);
                        Block parent = allBlockLookUp.get(parentBlockStr);
                        int thisBlocksIndex = block.index;
                        if (parent==null) {
                            parent = new Block();
                            parent.strId = parentBlockStr;
                            parent.setIdArray(block.parentBlockID);
                        }
                        if (parent.subBlocks.size() < thisBlocksIndex+1)
                            for (int j = parent.subBlocks.size() ; j <= thisBlocksIndex ; j++)
                                parent.subBlocks.add(null);
                        if (parent.subBlocks.get(thisBlocksIndex)!=null) {
                            SurveyException se =  new MalformedBlockException(block.strId, this, this.getClass().getEnclosingMethod());
                            LOGGER.fatal(se);
                            throw se;
                        }
                        parent.subBlocks.set(thisBlocksIndex, block);
                        // now that we've placed this block, remove it from the lookup
                        itr.remove();
                        blockLookUp.remove(strId);
                    } // else, skip for now
                }
            }
            currentDepth++;
        }
        return blocks;
    }
    
    private void unifyBlocks(ArrayList<CSVEntry> blockLexemes, ArrayList<Block> blocks, ArrayList<CSVEntry> qLexemes, ArrayList<Question> questions)
            throws SurveyException{
        // associate questions with the appropriate block
        CSVEntry.sort(blockLexemes);
        CSVEntry.sort(qLexemes);
        // looping this way creates more work, but we can clean it up later.
        for (int i = 0 ; i < blockLexemes.size() ; i++) {
            if (! (qLexemes.get(i).contents==null || qLexemes.get(i).contents.equals(""))) {
                int lineNo = blockLexemes.get(i).lineNo;
                if (lineNo != qLexemes.get(i).lineNo) {
                    SurveyException se = new SyntaxException(String.format("Misaligned linenumbers"), this, this.getClass().getEnclosingMethod());
                    LOGGER.fatal(se);
                    throw se;
                }
                String blockStr = blockLexemes.get(i).contents;
                // get question corresponding to this lineno
                Question question = null;
                for (Question q : questions) 
                    if (q.sourceLineNos.contains(lineNo)) {
                        question = q; break;
                    }
                if (question==null) {
                    SurveyException e = new SyntaxException(String.format("No question found at line %d", lineNo), this, this.getClass().getEnclosingMethod());
                    LOGGER.fatal(e);
                    throw e;
                }
                // get block corresponding to this lineno
                Block block = allBlockLookUp.get(blockStr);
                if (block==null) {
                    SurveyException e = new SyntaxException(String.format("No block found corresponding to %s", blockStr), this, this.getClass().getEnclosingMethod());
                    LOGGER.fatal(e);
                    throw e;
                }
                question.block = block;
                block.questions.add(question);
            }
        }
    }
            
    public Survey parse() throws MalformedURLException, SurveyException {

        Map<String, ArrayList<CSVEntry>> lexemes = csvLexer.entries;

        Survey survey = new Survey();
        survey.encoding = csvLexer.encoding;
        survey.source = csvLexer.filename;
        survey.sourceName = new File(csvLexer.filename).getName().split("\\.")[0];

        // sort each of the table entries, so we're monotonically inew Question[]{ tempQ }ncreasing by lineno
        for (String key : lexemes.keySet())
            CSVEntry.sort(lexemes.get(key));
        
        // add questions to the survey
        ArrayList<Question> questions = unifyQuestions();
        survey.questions = questions;
        
        // add blocks to the survey
        if (lexemes.containsKey(Survey.BLOCK)) {
            ArrayList<Block> blocks = initializeBlocks();
            unifyBlocks(lexemes.get(Survey.BLOCK), blocks, lexemes.get(Survey.QUESTION), questions);
            survey.blocks = blocks;
        } else survey.blocks = new ArrayList<Block>();

        // update branch list
        unifyBranching(survey);
        
        survey.correlationMap = this.correlationMap;
        
        return survey;
    }
}
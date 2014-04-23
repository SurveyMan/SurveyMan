package csv;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import survey.*;
import system.Bug;
import system.Debugger;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.*;
import java.util.regex.Pattern;

import static csv.CSVLexer.falseValues;
import static csv.CSVLexer.trueValues;

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
    static class MalformedBooleanException extends SurveyException implements Bug{
        Object caller;
        Method lastAction;
        public MalformedBooleanException(String boolStr, String column, CSVParser caller, Method lastAction) {
            super(String.format("Unrecognized boolean string (%s) in column %s. See the SurveyMan wiki for accepted strings.", boolStr, column));
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
    public final static HashMap<String, Boolean> defaultValues = new HashMap<String, Boolean>();

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

    private static boolean boolType(String thing, String col, CSVParser parser) throws SurveyException{
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
    
    private static Boolean parseBool(Boolean bool, String col, CSVEntry entry, CSVParser parser) throws SurveyException {
        //String thing = stripQuots(entry.contents.trim()).trim();
        String thing = entry.contents;
        if (bool==null)
            return boolType(thing, col, parser);
        else {
            boolean actual = boolType(thing, col, parser);
            if (bool.booleanValue() != actual) {
                SurveyException e = new MalformedBooleanException(String.format("Inconsistent boolean values; Expected %b in %s. Got %b (%d, %d)."
                            , bool
                            , actual
                            , entry.lineNo
                            , entry.colNo)
                    , col
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
                        , entry.lineNo // TODO: entry.lineNo will throw a nullptr exception given this if statement
                        , entry.colNo));
                return defaultValues.get(colName);
            } else return parseBool(bool, colName, entry, parser);
        }
    }

    private static Boolean assignFreetext(Question q, int i, CSVParser parser) throws SurveyException {
        Boolean b;
        try{
            b = assignBool(q.freetext, Survey.FREETEXT, i, parser);
        } catch (MalformedBooleanException mbe) {
            LOGGER.info(mbe);
            b = true;
            String freetextEntry = parser.lexemes.get(Survey.FREETEXT).get(i).contents;
            Pattern regexPattern = Pattern.compile("\\#\\{.*\\}");
            if ( regexPattern.matcher(freetextEntry).matches() ){
                String regexContents = freetextEntry.substring(2, freetextEntry.length() - 1);
                assert(regexContents.length() == freetextEntry.length() - 3);
                q.freetextPattern = Pattern.compile(regexContents);
            } else {
                q.freetextDefault = freetextEntry;
            }
        }
        return b;
    }

    /** instance methods */

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
                    if (question.block.branchQ==null) {
                        question.block.branchParadigm = Block.BranchParadigm.ONE; //getBranchParadigm(question.branchMap);
                        question.block.branchQ = question;
                    } else if (question.block.branchQ != question) {
                        question.block.branchParadigm = Block.BranchParadigm.SAMPLE;
                    }
                    //question.block.propagateBranchParadigm();
                    // get component of the option
                    CSVEntry option = lexemes.get(Survey.OPTIONS).get(branches.indexOf(entry));
                    Component c = question.getOptById(Component.makeComponentId(option.lineNo, option.colNo));
                    Block b = allBlockLookUp.get(entry.contents);
                    if (b==null && ! entry.contents.equals("NULL")) {
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
//x
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

        if (questions==null || options == null)
            throw new SyntaxException(String.format("Surveys must have at a minimum a QUESTION column and an OPTIONS column. " +
                    "The %s column is missing in survey %s.", questions==null ? Survey.QUESTION : Survey.OPTIONS, this.csvLexer.filename), null, null);

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
                if (correlationMap.containsKey(correlation.contents))
                  correlationMap.get(correlation.contents).add(tempQ);
                else correlationMap.put(correlation.contents, new ArrayList<Question>(Arrays.asList(new Question[]{ tempQ })));
            }
            tempQ.options.put(Component.makeComponentId(option.lineNo, option.colNo), parseComponent(option, tempQ.options.size()));
            tempQ.sourceLineNos.add(option.lineNo);
            //assign boolean question fields
            if (tempQ.exclusive==null)
                tempQ.exclusive = assignBool(tempQ.exclusive, Survey.EXCLUSIVE, i, this);
            if (tempQ.ordered==null)
                tempQ.ordered = assignBool(tempQ.ordered, Survey.ORDERED, i, this);
            if (tempQ.randomize==null)
                tempQ.randomize = assignBool(tempQ.randomize, Survey.RANDOMIZE, i, this);
            if (tempQ.freetext==null)
                tempQ.freetext = assignFreetext(tempQ, i, this);
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
             String piece = pieces[i];
             if (Block.isRandomizable(piece))
                 piece = piece.substring(1);
             id[i] = Integer.parseInt(piece);
         }
         return id;
    }
    
    private void addPhantomBlocks(Map<String, Block> blockLookUp) {
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
                        tempB = new Block(entry.contents);
                        tempB.sourceLines.add(entry.lineNo);
                        // if top-level, add to topLevelBlocks
                        if (tempB.isTopLevel()) topLevelBlocks.add(tempB);
                        blockLookUp.put(entry.contents, tempB);
                    }
                }
            }
        }
        addPhantomBlocks(blockLookUp);
    }

    private Map<String, Block> cleanedStrIds(Map<String, Block> m){
        Map<String, Block> cleanedMap = new HashMap<String, Block>();
        for (Map.Entry<String, Block> entry : m.entrySet())
            cleanedMap.put(cleanStrId(entry.getKey()), entry.getValue());
        return cleanedMap;
    }

    private String cleanStrId(String id){
        return Block.idToString(Block.idToArray(id));
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
                Block block = blockLookUp.get(strId);
                if (block.isTopLevel()) {
                    if (!topLevelBlocks.contains(block)) {
                        ((ArrayList<Block>) topLevelBlocks).add(block);
                    }
                    itr.remove();
                    blockLookUp.remove(strId);
                } else {
                    // this is not a top-level block.
                    //LOGGER.debug(block);
                    // if this block is at the current level of interest
                    if (block.getBlockDepth() == currentDepth + 1) {
                        String parentBlockStr = block.getParentStrId();
                        Block parent = allBlockLookUp.get(parentBlockStr);
                        int thisBlocksIndex = block.index;
                        if (parent==null) {
                            parent = new Block();
                            parent.setStrId(cleanStrId(parentBlockStr));
                            parent.setIdArray(Block.idToArray(parentBlockStr));
                        }
                        if (parent.subBlocks.size() < thisBlocksIndex+1)
                            for (int j = parent.subBlocks.size() ; j <= thisBlocksIndex ; j++)
                                parent.subBlocks.add(null);
//                        if (parent.subBlocks.get(thisBlocksIndex)!=null) {
//                            SurveyException se =  new MalformedBlockException(block.getStrId(), this, this.getClass().getEnclosingMethod());
//                            LOGGER.fatal(se);
//                            throw se;
//                        }
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

    private void propagateBranchParadigm(Block b) throws SurveyException {
        if (b.subBlocks.isEmpty())
            b.propagateBranchParadigm();
        for (Block bb : b.subBlocks)
            propagateBranchParadigm(bb);
    }

    private void propagateBranchParadigms(Survey survey) throws SurveyException {
        for (Block b : survey.topLevelBlocks)
            propagateBranchParadigm(b);
    }

    private String[] extractOtherHeaders() {
        List<String> temp = new ArrayList<String>();
        List<String> knownHeaders = Arrays.asList(Survey.knownHeaders);
        for (String colName : lexemes.keySet()) {
            if (!knownHeaders.contains(colName))
                temp.add(colName);
        }
        return temp.toArray(new String[temp.size()]);
    }

    private void initializeAllOneBlock(Survey survey){
        Block block = new Block("1");
        block.questions = survey.questions;
        topLevelBlocks.add(block);
        survey.blocks.put(cleanStrId("1"), block);
        for (Question q : survey.questions) {
            q.block = block;
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
            survey.blocks = new HashMap<String, Block>();
            for (Block b : blocks)
                survey.blocks.put(cleanStrId(b.getStrId()), b);
        } else survey.blocks = new HashMap<String, Block>();

        // update branch list
        unifyBranching(survey);

        survey.resetQuestionIndices();

        if (this.topLevelBlocks.isEmpty()) {
            initializeAllOneBlock(survey);
        }

        Collections.sort(this.topLevelBlocks);
        survey.topLevelBlocks = this.topLevelBlocks;

        survey.correlationMap = this.correlationMap;
        for (Block b : survey.topLevelBlocks)
            b.setParentPointer();
        propagateBranchParadigms(survey);

        survey.otherHeaders = extractOtherHeaders();

        return survey;
    }
}
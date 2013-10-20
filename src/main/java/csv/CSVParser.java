package csv;

import static csv.CSVLexer.*;
import scalautils.QuotMarks;
import survey.*;
import system.Bug;
import system.Debugger;
import system.mturk.MturkLibrary;
import java.io.*;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class CSVParser {

    /** Inner/nested classes*/
    class MalformedBlockException extends SurveyException implements Bug {
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
        defaultValues.put(EXCLUSIVE, true);
        defaultValues.put(ORDERED, false);
        defaultValues.put(PERTURB, true);
        defaultValues.put(FREETEXT, false);
    }
    final private static Logger LOGGER = Logger.getLogger(CSVParser.class);

    /** instance fields */
    private HashMap<String, ArrayList<CSVEntry>> lexemes = null;
    private String[] headers;
    private final CSVLexer csvLexer;
    private List<Block> topLevelBlocks = new ArrayList<Block>();
    private Map<String, Block> allBlockLookUp = null;

    /** constructors */
    public CSVParser(CSVLexer lexer){
        this.lexemes = lexer.entries;
        this.headers = lexer.headers;
        this.csvLexer = lexer;
    }


    public static String stripQuots(String s) {
        //CSVLexer.stripQuots strips according to the the layered header quotation marks
        //CSVParser.stripQuots only tries to strip one layer of quots
        /*
        if (s.length()>1) {
            String firstChar = s.substring(0,1);
            String secondChar = s.substring(1,2);
            if (QuotMarks.isA(firstChar) && !QuotMarks.isA(secondChar)) {
                for (String quot : QuotMarks.getMatch(firstChar))
                    if (s.endsWith(quot))
                        return s.substring(1,s.length()-1);
            }
        }
        */
        return s;
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
        ArrayList<CSVEntry> branches = lexemes.get(BRANCH);
        if (!(branches==null || branches.isEmpty())) {
            for (CSVEntry entry : branches) {
                if (!(entry==null || entry.contents==null || entry.contents.equals(""))) {
                    CSVEntry matchingOption = lexemes.get(OPTIONS).get(branches.indexOf(entry));
                    Question question = null;
                    for (Question q : survey.questions){
                        //match by lineno to question
                        if (q.sourceLineNos.contains(Integer.valueOf(entry.lineNo))) {
                            question = q; break;
                        }
                    }
                    if (question==null) {
                        SurveyException e = new SyntaxException(String.format("Branch to block (%s) at line %d matches no known options (from answer error)."
                                , entry.contents
                                , entry.lineNo)
                            , this, this.getClass().getEnclosingMethod());
                        LOGGER.warn(e);
                        throw e;
                    }
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
                    Component c = question.getOptByData(stripQuots(matchingOption.contents.trim()).trim());
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
                if (! (key.equals(OPTIONS) || key.equals(BRANCH))) {
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
        ArrayList<Question> qlist = new ArrayList<Question>();
        Question tempQ = null;
        ArrayList<CSVEntry> questions = lexemes.get(QUESTION);
        ArrayList<CSVEntry> options = lexemes.get(OPTIONS);
        ArrayList<CSVEntry> resources = (lexemes.containsKey(RESOURCE)) ? lexemes.get(RESOURCE) : null;
        int index = 0;
        for (int i = 0; i < questions.size() ; i++) {
            CSVEntry question = questions.get(i);
            CSVEntry option = options.get(i);
            LOGGER.log(Level.INFO, tempQ+"Q:"+question.contents+"O:"+option.contents);
            if (newQuestion(question, option, tempQ, i)) {
                tempQ = new Question();
                tempQ.data.add(parseComponent(question.contents));
                tempQ.options =  new HashMap<String, Component>();
                tempQ.index = index;
                qlist.add(tempQ);
                index++;
            }
            if (resources != null && resources.get(i).contents!=null) {
                String potentialURL = stripQuots(resources.get(i).contents.trim()).trim();
                if (!potentialURL.equals(""))
                    tempQ.data.add(new URLComponent(potentialURL));
            }
            parseOptions(tempQ.options, option.contents, this);
            // add this line number to the question's lineno list
            tempQ.sourceLineNos.add(option.lineNo);
            //assign boolean question fields
            tempQ.exclusive = assignBool(tempQ.exclusive, EXCLUSIVE, i, this);
            tempQ.ordered = assignBool(tempQ.ordered, ORDERED, i, this);
            tempQ.perturb = assignBool(tempQ.perturb, PERTURB, i, this);
            tempQ.freetext = assignBool(tempQ.freetext, FREETEXT, i, this);
            if (tempQ.freetext)
                tempQ.options.put("freetext", new StringComponent(""));
            if (tempQ.otherValues.size()==0)
                for (String col : headers) {
                    boolean known = false;
                    for (int j = 0 ; j < knownHeaders.length ; j++)
                        if (knownHeaders[j].equals(col)){
                            known = true; break;
                        }
                    if (! known) {
                        String val = lexemes.get(col).get(i).contents;
                        LOGGER.log(Level.DEBUG, " val: "+val);
                        tempQ.otherValues.put(col, val);
                    }
                }
            LOGGER.log(Level.DEBUG, " numOtherValues: "+tempQ.otherValues.size());
        }
        return qlist;
    }

    private static void parseOptions(Map<String, Component> optMap, String optString, CSVParser parser) throws SurveyException{
        
        int baseIndex = getNextIndex(optMap);
        if (optString==null || optString.length()==0) return;
        optString=stripQuots(optString.trim());

        if (optString.startsWith("[[") && optString.endsWith("]]")) {
            // if a range list
            String[] bounds = optString.substring(2,optString.length()-2).split("--?");
            int upper, lower;
            try {
                upper = Integer.parseInt(bounds[0]);
                lower = Integer.parseInt(bounds[1]);
                if (lower > upper) {
                    int temp = upper;
                    upper = lower;
                    lower = temp;
                }
            } catch (NumberFormatException nfe) {
                SurveyException e = new MalformedOptionException(optString, parser, parser.getClass().getEnclosingMethod());
                LOGGER.fatal(e);
                throw e;
            }
            for (int i = lower ; i < upper ; i++) {
                Component c = new StringComponent(String.valueOf(i));
                c.index = i - lower + baseIndex;
                optMap.put(c.cid, c);
            }
        } else if (optString.startsWith("[") && !optString.startsWith("[[")) {
            // if a single-cell list
            String addendum = "";
            try {
                if (!optString.endsWith("]"))
                    addendum = optString.substring(optString.lastIndexOf("*")+1);
            } catch (IndexOutOfBoundsException e) {
                SurveyException surveyException =  new MalformedOptionException(optString, parser, parser.getClass().getEnclosingMethod());
                LOGGER.fatal(surveyException);
                throw surveyException;
            }
            // temporarily replace the xmlchars
            //for (Map.Entry<String, String> e : CSVLexer.xmlChars.entrySet())
            //    optString = optString.replaceAll(e.getValue(), e.getKey());
            // get the contents of the list
            optString = optString.substring(1, optString.length() - (addendum.length()== 0 ? 1 : (addendum.length()+2)));
            // split the list according to one of two valid delimiters
            String[] opts = optString.split(";|,");
            for (int i = 0 ; i < opts.length ; i++) {
                Component c = parseComponent(String.format("%s%s%s"
                        , opts[i].trim()
                        , (opts[i].trim().equals(""))?"":" "
                        , addendum));
                c.index = i + baseIndex;
                optMap.put(c.cid, c);
            }
        } else if (!optString.startsWith("[")){// we're a single option
            Component c = parseComponent(optString);
            c.index = baseIndex;
            optMap.put(c.cid, c);
        } else {
            SurveyException e = new MalformedOptionException(optString, parser, parser.getClass().getEnclosingMethod());
            LOGGER.fatal(e);
            throw e;
        }
    }
    
    private static int getNextIndex(Map<String, Component> optMap) {
        int maxindex = -1;
        for (Component cc : optMap.values())
            if (cc.index > maxindex)
                maxindex = cc.index;
        return maxindex + 1;
    }

    public static Component parseComponent(String contents) {
        try {
            return new URLComponent(contents);
        } catch (MalformedURLException e) {
            return new StringComponent(contents);
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
        if (lexemes.containsKey(BLOCK)) {
            Block tempB = null;
            for (CSVEntry entry : lexemes.get(BLOCK)) {
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
                        tempB.randomize = entry.contents.startsWith("_");
                        tempB.sourceLines.add(entry.lineNo);
                        tempB.id = getBlockIdArray(entry.contents);
                        // if top-level, add to topLevelBlocks
                        if (tempB.id.length==1)
                            topLevelBlocks.add(tempB);
                        else {
                            boolean topLevelp = true;
                            for (int i = 1 ; i < tempB.id.length ; i++)
                                if (tempB.id[i]!=0) {
                                    topLevelp = false;
                                    break;
                                }
                            if (topLevelp)
                                topLevelBlocks.add(tempB);
                        }
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
                    LOGGER.log(Level.WARN, "heirarchical blocks have not yet been tested.");
                    Block block = blockLookUp.get(strId);
                    // if this block is at the current level of interest
                    if (block.id.length == currentDepth + 1) {
                        // infer my parent block id; java, you are the dumbest
                        List<Integer> sublist = new ArrayList<Integer>();
                        for (int i=0; i<block.id.length-1; i++)
                            sublist.add(block.id[i]);
                        Integer[] parentBlockId = sublist.toArray(new Integer[block.id.length-1]);
                        String parentBlockStr = parentBlockId[0].toString();
                        for (int i=1; i < parentBlockId.length ; i++)
                            parentBlockStr = parentBlockStr + "." + parentBlockId[i].toString();
                        // get parent block
                        Block parent = allBlockLookUp.get(parentBlockStr);
                        int thisBlocksIndex = block.id[block.id.length-1]-1;
                        if (parent==null) {
                            parent = new Block();
                            parent.strId = parentBlockStr;
                            parent.id = getBlockIdArray(parentBlockStr);
                        }
                        if (parent.subBlocks==null)
                            parent.subBlocks = new ArrayList<Block>();
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
                LOGGER.log(Level.DEBUG, " this question: "+question);
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

        Survey survey = new Survey();
        survey.encoding = csvLexer.encoding;

        Map<String, ArrayList<CSVEntry>> lexemes = csvLexer.entries;
        
        // sort each of the table entries, so we're monotonically increasing by lineno
        for (String key : lexemes.keySet())
            CSVEntry.sort(lexemes.get(key));
        
        // add questions to the survey
        ArrayList<Question> questions = unifyQuestions();
        survey.questions = questions;
        
        // add blocks to the survey
        if (lexemes.containsKey(BLOCK)) {
            ArrayList<Block> blocks = initializeBlocks();
            unifyBlocks(lexemes.get(BLOCK), blocks, lexemes.get(QUESTION), questions);
            survey.blocks = blocks;
        } else survey.blocks = new ArrayList<Block>();

        // update branch list
        unifyBranching(survey);
        
        return survey;
    }
}


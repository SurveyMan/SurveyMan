package csv;

import static csv.CSVLexer.*;
import scalautils.QuotMarks;
import survey.*;
import system.mturk.MturkLibrary;
import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class CSVParser {
    

    public static HashMap<String, Boolean> defaultValues = new HashMap<String, Boolean>();
    static {
        defaultValues.put(EXCLUSIVE, true);
        defaultValues.put(ORDERED, false);
        defaultValues.put(PERTURB, true);
        defaultValues.put(FREETEXT, false);
    }
    final public static String[] trueValues = {"yes", "y", "true", "t", "1"};
    final public static String[] falseValues = {"no", "n", "false", "f", "0"};
    final private static Logger LOGGER = Logger.getLogger("csv");
    private static HashMap<String, ArrayList<CSVEntry>> lexemes = null;
    private static List<Block> topLevelBlocks = new ArrayList<Block>();
    private static Map<String, Block> allBlockLookUp = null;

    public static String stripQuots(String s) {
        //CSVLexer.stripQuots strips according to the the layered header quotation marks
        //CSVParser.stripQuots only tries to strip one layer of quots
        if (s.length()>1) {
            String firstChar = s.substring(0,1);
            String secondChar = s.substring(1,2);
            if (QuotMarks.isA(firstChar) && !QuotMarks.isA(secondChar)) {
                for (String quot : QuotMarks.getMatch(firstChar))
                    if (s.endsWith(quot))
                        return s.substring(1,s.length()-1);
            }
        }
        return s;
    }

    private static boolean boolType(String thing) throws SurveyException{
        if (Arrays.asList(trueValues).contains(thing.toLowerCase()))
            return true;
        else if (Arrays.asList(falseValues).contains(thing.toLowerCase()))
            return false;
        else throw new MalformedBooleanException(thing);
    }
    
    private static Boolean parseBool(Boolean bool, CSVEntry entry) throws SurveyException {
        String thing = stripQuots(entry.contents.trim()).trim();
        if (bool==null)
            return boolType(thing);
        else {
            boolean actual = boolType(thing);
            if (bool.booleanValue() != actual)
                throw new MalformedBooleanException(String.format("Inconsistent boolean values; Expected %b. Got %b (%d, %d)."
                        , bool.booleanValue()
                        , actual
                        , entry.lineNo
                        , entry.colNo));
        }
        return bool;
    }

    private static Boolean assignBool(Boolean bool, String colName, int i) throws SurveyException {
        ArrayList<CSVEntry> thisCol = lexemes.get(colName);
        // if this column doesn't exist, set it to be the default value
        if (thisCol==null || thisCol.size()==0)
            return defaultValues.get(colName);
        else {
            CSVEntry entry = thisCol.get(i);
            // if the user skipped this column, set to be the default entry
            if (entry==null || entry.contents.equals("")) {
                LOGGER.warn(String.format("Supplying default entry for column %s in cell (%d,%d)"
                        , colName
                        , entry.lineNo
                        , entry.colNo));
                return defaultValues.get(colName);
            } else return parseBool(bool, entry);
        }
    }

    private static void ensureBranchForward(int[] toBlock, Question q) throws SurveyException {
        int[] fromBlock = q.block.id;
        String toBlockStr = String.valueOf(toBlock[0]);
        for (int i=1; i<toBlock.length; i++)
            toBlockStr = toBlockStr + "." + toBlock[i];
        if (fromBlock[0]>=toBlock[0])
            throw new BranchException(q.block.strId, toBlockStr);
    }

    private static void ensureCompactness() throws SurveyException {
        //first check the top level
        Block[] temp = new Block[topLevelBlocks.size()];
        for (Block b : topLevelBlocks) {
            if (temp[b.id[0]-1]==null)
                temp[b.id[0]-1]=b;
            else throw new SyntaxException(String.format("Block %s is noncontiguous.", b.strId));
        }
        for (Block b : allBlockLookUp.values())
            if (b.subBlocks!=null)
                for (Block bb : b.subBlocks)
                    if (bb==null)
                        throw new SyntaxException(String.format("Detected noncontiguous subblock in parent block %s", b.strId));
    }

    private static void unifyBranching(Survey survey) throws SurveyException {
        // grab the branch column from lexemes
        // find the block with the corresponding blockid
        // put the cid and block into the
        ArrayList<CSVEntry> branches = lexemes.get(BRANCH);
        if (!(branches==null || branches.isEmpty())) {
            for (CSVEntry entry : branches) {
                if (!(entry==null || entry.contents.equals(""))) {
                    CSVEntry matchingOption = lexemes.get(OPTIONS).get(branches.indexOf(entry));
                    Question question = null;
                    for (Question q : survey.questions){
                        //match by lineno to question
                        if (q.sourceLineNos.contains(Integer.valueOf(entry.lineNo))) {
                            question = q; break;
                        }
                    }
                    if (question==null) 
                        throw new SyntaxException(String.format("Branch to block (%s) at line %d matches no known options (from answer error)."
                                , entry.contents
                                , entry.lineNo));
                    // set this question's block's branchQ equal to this question
                    if (question.block.branchQ!=null) {
                        if (!question.quid.equals(question.block.branchQ.quid))
                            throw new BranchException(String.format("Only permitted one branch per block. Error in block %s", entry.contents));
                    } else question.block.branchQ = question;
                    // get component of the option
                    Component c = question.getOptByData(stripQuots(matchingOption.contents.trim()).trim());
                    Block b = allBlockLookUp.get(entry.contents);
                    if (b==null)
                        throw new SyntaxException(String.format("Branch to block (%s) at line %d matches no known block (to question error)."
                                , entry.contents
                                , entry.lineNo));
                    ensureBranchForward(b.id, question);
                    question.branchMap.put(c, b);
                }   
            }
        }
    }

    private static boolean newQuestion(CSVEntry question, CSVEntry option, Question tempQ, int i) throws SurveyException{
        // checks for well-formedness and returns true if we should set tempQ to a new question
        if (question.lineNo != option.lineNo)
            throw new SyntaxException("CSV entries not properly aligned.");
        if ( tempQ == null && "".equals(question.contents) )
            throw new SyntaxException("No question indicated.");
        if (tempQ != null && question.contents.equals("")) {
            // then this line should include only options.
            for (String key: lexemes.keySet()) {
                if (! (key.equals(OPTIONS) || key.equals(BRANCH))) {
                    CSVEntry entry = lexemes.get(key).get(i);
                    if (! entry.contents.trim().equals(""))
                        throw new SyntaxException(String.format("Entry in cell (%d,%d) (column %s) is %s; was expected to be empty"
                                , entry.lineNo
                                , entry.colNo
                                , key
                                , entry.contents));
                }
            }
            // will be using the tempQ from the previous question
            return false;
        } else return true;
    }

    private static ArrayList<Question> unifyQuestions() throws MalformedURLException, SurveyException {
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
            if (resources != null) {
                String potentialURL = stripQuots(resources.get(i).contents.trim()).trim();
                if (!potentialURL.equals(""))
                    tempQ.data.add(new URLComponent(potentialURL));
            }
            parseOptions(tempQ.options, option.contents);
            // add this line number to the question's lineno list
            tempQ.sourceLineNos.add(option.lineNo);
            //assign boolean question fields
            tempQ.exclusive = assignBool(tempQ.exclusive, EXCLUSIVE, i);
            tempQ.ordered = assignBool(tempQ.ordered, ORDERED, i);
            tempQ.perturb = assignBool(tempQ.perturb, PERTURB, i);
            tempQ.freetext = assignBool(tempQ.freetext, FREETEXT, i);
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

    private static void parseOptions(Map<String, Component> optMap, String optString) throws SurveyException{
        
        int baseIndex = getNextIndex(optMap);
        if (optString.length()==0) return;
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
                throw new MalformedOptionException(optString);
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
                throw new MalformedOptionException(optString);
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
        } else throw new MalformedOptionException(optString);
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
         for (int i = 0 ; i < pieces.length ; i ++)
             id[i] = Integer.parseInt(pieces[i]);
         return id;
    }
    
    private static void setBlockMaps(Map<String, Block> blockLookUp, List<Block> topLevelBlocks) {
        // first create a flat map of all the blocks;
        // the goal is to unify the list of block ids
        if (CSVParser.lexemes.containsKey(BLOCK)) {
            Block tempB = null;
            for (CSVEntry entry : CSVParser.lexemes.get(BLOCK)) {
                if (entry.contents.length()==0) {
                    // this line belongs to the last parsed block
                    tempB.sourceLines.add(entry.lineNo);
                } else {
                    if (blockLookUp.containsKey(entry.contents)) {
                        tempB = blockLookUp.get(entry.contents);
                        tempB.sourceLines.add(entry.lineNo);
                    } else {
                        tempB = new Block();
                        tempB.strId = entry.contents;
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
             
    private static ArrayList<Block> initializeBlocks() throws SurveyException{
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
                        if (parent.subBlocks.get(thisBlocksIndex)!=null)
                            throw new MalformedBlockException(block.strId);
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
    
    private static void unifyBlocks(ArrayList<CSVEntry> blockLexemes, ArrayList<Block> blocks, ArrayList<CSVEntry> qLexemes, ArrayList<Question> questions)
            throws SurveyException{
        // associate questions with the appropriate block
        CSVEntry.sort(blockLexemes);
        CSVEntry.sort(qLexemes);
        // looping this way creates more work, but we can clean it up later.
        for (int i = 0 ; i < blockLexemes.size() ; i++) {
            if (! qLexemes.get(i).contents.equals("")) {
                int lineNo = blockLexemes.get(i).lineNo;
                if (lineNo != qLexemes.get(i).lineNo) throw new SyntaxException("ParseError");
                String blockStr = blockLexemes.get(i).contents;
                // get question corresponding to this lineno
                Question question = null;
                for (Question q : questions) 
                    if (q.sourceLineNos.contains(lineNo)) {
                        question = q; break;
                    }
                LOGGER.log(Level.DEBUG, " this question: "+question);
                if (question==null) throw new SyntaxException(String.format("No question found at line %d", lineNo));
                // get block corresponding to this lineno
                Block block = allBlockLookUp.get(blockStr);
                if (block==null)
                    throw new SyntaxException(String.format("No block found corresponding to %s", blockStr));
                question.block = block;
                block.questions.add(question);
            }
        }
        ensureCompactness();
    }
            
    private static Survey parse() throws MalformedURLException, SurveyException {

        Survey survey = new Survey();
        survey.encoding = CSVLexer.encoding;
        
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

    public static Survey parse(HashMap<String, ArrayList<CSVEntry>> inputLexemes)
            throws SurveyException, MalformedURLException{
        CSVParser.lexemes = inputLexemes;
        CSVParser.topLevelBlocks = new ArrayList<Block>();
        CSVParser.allBlockLookUp = null;
        return parse();
    }

    public static Survey parse(String filename, String seperator)
            throws IOException, SurveyException {

        if (seperator.length() > 1)
            CSVLexer.separator = specialChar(seperator);
        else CSVLexer.separator = seperator.codePointAt(0);

        HashMap<String, ArrayList<CSVEntry>> lexemes = CSVLexer.lex(filename);
        Survey survey = parse(lexemes);
        List<String> otherHeaders = new ArrayList<String>();

        for (String header : headers)
            if (! Arrays.asList(knownHeaders).contains(header))
                otherHeaders.add(header);
        survey.otherHeaders = otherHeaders.toArray(new String[otherHeaders.size()]);
        // set the survey name. maybe this would be shorter with a regex?

        survey.source = filename;
        String[] fileNamePieces = filename.split(MturkLibrary.fileSep);
        String name = fileNamePieces[fileNamePieces.length - 1];
        survey.sourceName = name.split("\\.")[0];

        return survey;
    }

    public static Survey parse(String filename)
            throws FileNotFoundException, IOException, SurveyException {
        topLevelBlocks = new ArrayList<Block>();
        allBlockLookUp = null;
        return parse(filename, ",");
    }
}

class MalformedBlockException extends SurveyException {
    public MalformedBlockException(String strId) {
        super(String.format("Malformed block identifier: %s", strId));
    }
}

class MalformedOptionException extends SurveyException {
    public MalformedOptionException(String optString) {
        super(String.format("%s has unknown formatting. See documentation for permitted formatting.", optString));
    }
}

class MalformedBooleanException extends SurveyException {
    public MalformedBooleanException(String boolStr) {
        super(String.format("Unrecognized boolean string (%s). See the SurveyMan wiki for accepted strings.", boolStr));
    }
}

class SyntaxException extends SurveyException {
    public SyntaxException(String msg) {
        super(msg);
    }
}

class BranchException extends SurveyException {
    public BranchException(String fromBlockId, String toBlockId) {
        super(String.format("Cannot jump from block %s to block %s. Must always jump into a block whose outermost numbering is greater."
                       , fromBlockId, toBlockId));
    }
    public BranchException(String msg) {
        super(msg);
    }
}

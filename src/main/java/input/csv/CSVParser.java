package input.csv;

import input.AbstractParser;
import input.exceptions.MalformedBooleanException;
import input.exceptions.SyntaxException;
import org.apache.log4j.Level;
import survey.*;
import survey.exceptions.SurveyException;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class CSVParser extends AbstractParser {

    private HashMap<String, ArrayList<CSVEntry>> lexemes = null;
    private String[] headers;
    private final CSVLexer csvLexer;

    public CSVParser(CSVLexer lexer){
        this.lexemes = lexer.entries;
        this.headers = lexer.headers;
        this.csvLexer = lexer;
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
            } else return parseBool(bool, colName, entry.contents, entry.lineNo, entry.colNo, parser);
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


    public static Component parseComponent(CSVEntry csvEntry, int index) {
        Component c = parseComponent(csvEntry.contents, csvEntry.lineNo, csvEntry.colNo);
        c.index = index;
        return c;
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
                        question.block.branchParadigm = Block.BranchParadigm.ALL;
                    }
                    //question.block.propagateBranchParadigm();
                    // get component of the option
                    CSVEntry option = lexemes.get(Survey.OPTIONS).get(branches.indexOf(entry));
                    Component c = question.getOptById(Component.makeComponentId(option.lineNo, option.colNo));
                    Block b = allBlockLookUp.get(entry.contents);
                    if (b==null && ! entry.contents.equals("NEXT")) {
                        SurveyException e = new SyntaxException(String.format("Branch to block (%s) at line %d matches no known block (to question error)."
                                , entry.contents
                                , entry.lineNo));
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
            SurveyException e = new SyntaxException("CSV entries not properly aligned.");
            LOGGER.fatal(e);
            throw e;
        }
        if ( tempQ == null && "".equals(question.contents) ){
            SurveyException e = new SyntaxException("No question indicated.");
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

    private ArrayList<Question> unifyQuestions() throws SurveyException {
        
        Question tempQ = null;
        ArrayList<Question> qlist = new ArrayList<Question>();
        ArrayList<CSVEntry> questions = lexemes.get(Survey.QUESTION);
        ArrayList<CSVEntry> options = lexemes.get(Survey.OPTIONS);
        ArrayList<CSVEntry> correlates = (lexemes.containsKey(Survey.CORRELATION)) ? lexemes.get(Survey.CORRELATION) : null;

        if (questions==null || options == null)
            throw new SyntaxException(String.format("Surveys must have at a minimum a QUESTION column and an OPTIONS column. " +
                    "The %s column is missing in survey %s.", questions==null ? Survey.QUESTION : Survey.OPTIONS, this.csvLexer.filename));

        int index = 0;
        
        for (int i = 0; i < questions.size() ; i++) {
            
            CSVEntry question = questions.get(i);
            CSVEntry option = options.get(i);
            
            LOGGER.log(Level.INFO, tempQ+"Q:"+question.contents+"O:"+option.contents);

            if (newQuestion(question, option, tempQ, i)) {
                tempQ = new Question(question.lineNo, question.colNo);
                tempQ.data = parseComponent(question, 0);
                tempQ.options =  new HashMap<String, Component>();
                tempQ.index = index;
                qlist.add(tempQ);
                index++;
            }

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

            if (correlates != null && correlates.get(i).contents!=null) {
                CSVEntry correlation = correlates.get(i);
                if (correlationMap.containsKey(correlation.contents))
                  correlationMap.get(correlation.contents).add(tempQ);
                else correlationMap.put(correlation.contents, new ArrayList<Question>(Arrays.asList(new Question[]{ tempQ })));
            }

            if (!tempQ.freetext && option.contents!=null)
                tempQ.options.put(Component.makeComponentId(option.lineNo, option.colNo), parseComponent(option, tempQ.options.size()));

            tempQ.sourceLineNos.add(option.lineNo);

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
    
    private void unifyBlocks(ArrayList<CSVEntry> blockLexemes, ArrayList<CSVEntry> qLexemes, ArrayList<Question> questions)
            throws SurveyException{
        // associate questions with the appropriate block
        CSVEntry.sort(blockLexemes);
        CSVEntry.sort(qLexemes);
        // looping this way creates more work, but we can clean it up later.
        for (int i = 0 ; i < blockLexemes.size() ; i++) {
            if (! (qLexemes.get(i).contents==null || qLexemes.get(i).contents.equals(""))) {
                int lineNo = blockLexemes.get(i).lineNo;
                if (lineNo != qLexemes.get(i).lineNo) {
                    SurveyException se = new SyntaxException(String.format("Misaligned linenumbers"));
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
                    SurveyException e = new SyntaxException(String.format("No question found at line %d in survey %s", lineNo, csvLexer.filename));
                    LOGGER.fatal(e);
                    throw e;
                }
                // get block corresponding to this lineno
                Block block = allBlockLookUp.get(blockStr);
                if (block==null) {
                    SurveyException e = new SyntaxException(String.format("No block found corresponding to %s in %s", blockStr, csvLexer.filename));
                    LOGGER.fatal(e);
                    throw e;
                }
                question.block = block;
                block.questions.add(question);
            }
        }
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

    public Map<String, Block> getAllBlockLookUp() {
        return allBlockLookUp;
    }

    public Survey parse() throws SurveyException {

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
            unifyBlocks(lexemes.get(Survey.BLOCK), lexemes.get(Survey.QUESTION), questions);
            survey.blocks = new HashMap<String, Block>();
            for (Block b : blocks)
                survey.blocks.put(cleanStrId(b.getStrId()), b);
        } else survey.blocks = new HashMap<String, Block>();

        // update branch list
        unifyBranching(survey);

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

        survey.staticAnalysis();

        return survey;
    }
}
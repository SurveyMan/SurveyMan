package csv;

import static csv.CSVLexer.*;
import java.io.*;
import java.net.MalformedURLException;
import survey.*;

import java.util.*;

class CSVParser {
    
    // maybe rewrite this later to be more like a DB
    
    private static void parseOptions(Map<String, Component> options, String option) {
        try {
            options.put(option, new URLComponent(option));
        } catch (MalformedURLException e){
            options.put(option, new StringComponent(option));
        }
    }
    
    private static boolean boolType(String[] trueValues, String[] falseValues, String thing) {
        if (Arrays.asList(trueValues).contains(thing.toLowerCase()))
            return true;
        else if (Arrays.asList(falseValues).contains(thing.toLowerCase()))
            return false;
        else throw new RuntimeException(String.format("Unrecognized boolean string %s. See README for accepted strings.", thing));
    }
    
    private static boolean parseBool(Boolean bool, HashMap<String, ArrayList<CSVEntry>> lexemes, String colName, int index, boolean defaultVal) {
        String[] trueValues = {"yes", "y", "true", "t", "1"};
        String[] falseValues = {"no", "n", "false", "f", "0"};
        CSVEntry entry = (lexemes.containsKey(colName)) ? lexemes.get(colName).get(index) : null;
        if (bool==null) {
            if (entry==null || entry.contents.equals("")) 
                return defaultVal;
            else return boolType(trueValues, falseValues, entry.contents);
        } else { 
            if (! (bool==null || entry==null || entry.contents.equals(""))) {
                boolean actual = boolType(trueValues, falseValues, entry.contents);
                if (bool.booleanValue() != actual)
                    throw new RuntimeException(String.format("Inconsistent boolean values; Expected %b. Got %b (%d, %d)."
                            , bool.booleanValue()
                            , actual
                            , entry.lineNo
                            , entry.colNo));
            }
            return bool.booleanValue();
        }
    }
    
    private static void unifyBranching(){
        
    }
    
    private static ArrayList<Question> unifyQuestions(HashMap<String, ArrayList<CSVEntry>> lexemes){
        ArrayList<Question> qlist = new ArrayList<Question>();
        Question tempQ = null;
        ArrayList<CSVEntry> questions = lexemes.get("QUESTION");
        CSVEntry.sort(questions);
        ArrayList<CSVEntry> options = lexemes.get("OPTIONS");
        CSVEntry.sort(options);
        for (int i = 0; i < questions.size() ; i++) {
            CSVEntry question = questions.get(i);
            CSVEntry option = options.get(i);
            //out.println(tempQ+"Q:"+question.contents+"O:"+option.contents);
            if (question.lineNo != option.lineNo)
                throw new RuntimeException("CSV entries not properly aligned.");
            if ( tempQ == null && "".equals(question.contents) ) 
                throw new RuntimeException("No question indicated.");
            if (tempQ != null && question.contents.equals("")) {
                // then this line should include only options.
                for (String key: lexemes.keySet()) {
                    if (! key.equals("OPTIONS")) {
                        CSVEntry entry = lexemes.get(key).get(i);
                        if (! entry.contents.equals(""))
                            throw new RuntimeException(String.format("Entry in cell (%d,%d) (column %s) is %s; was expected to be empty"
                                    , entry.lineNo
                                    , entry.colNo
                                    , key
                                    , entry.contents));
                    }
                }
                // will be using the tempQ from the previous question
            } else {
                tempQ = new Question();
                try {
                    tempQ.data = new URLComponent(question.contents);
                } catch (MalformedURLException e) {
                    tempQ.data = new StringComponent(question.contents);
                }
                tempQ.options =  new HashMap<String, Component>();
                qlist.add(tempQ);
            }
            parseOptions(tempQ.options, option.contents);
            // add this line number to the question's lineno list
            tempQ.sourceLineNos.add(option.lineNo);
            // maybe box these in the future so we can pass in a reference3
            tempQ.exclusive = parseBool(tempQ.exclusive, lexemes, "EXCLUSIVE", i, true);
            tempQ.ordered = parseBool(tempQ.ordered, lexemes, "ORDERED", i, false);
            tempQ.perturb = parseBool(tempQ.perturb, lexemes, "PERTURB", i, true);
        }
        return qlist;
    }

    private static int[] getBlockIdArray (String contents) {
         String[] pieces = new String[1];
         if (contents.contains("."))
            pieces = contents.split(".");
         else pieces[0] = contents;
         int[] id = new int[pieces.length];
         for (int i = 0 ; i < pieces.length ; i ++) {
             //out.println("x"+pieces[i]+"x"+pieces.length+"x"+contents.contains("."));
             id[i] = Integer.parseInt(pieces[i]);
         }
         return id;
    }
             
    private static Block[] initializeBlocks(ArrayList<CSVEntry> lexemes) {
        Map<String, Block> blockLookUp = new HashMap<String, Block>();
        List<Block> topLevelBlocks = new ArrayList<Block>();
        // first create a flat map of all the blocks;
        // the goal is to unify the list of block ids
        for (CSVEntry entry : lexemes) {
            if (entry.contents.length() != 0) {
                if (blockLookUp.containsKey(entry.contents)) {
                    Block block = blockLookUp.get(entry.contents);
                    block.sourceLines.add(entry.lineNo);
                } else {
                    Block block = new Block();
                    block.strId = entry.contents;
                    block.sourceLines.add(entry.lineNo);
                    block.id = getBlockIdArray(entry.contents);
                    // if top-level, add to topLevelBlocks
                    if (block.id.length==1)
                        topLevelBlocks.add(block);
                    else {
                        boolean topLevelp = true;
                        for (int i = 1 ; i < block.id.length ; i++)
                            if (block.id[i]!=0) {
                                topLevelp = false;
                                break;
                            }
                        if (topLevelp)
                            topLevelBlocks.add(block);
                    }
                    blockLookUp.put(entry.contents, block);
                }
            }
        }
        // now create the heirarchical structure of the blocks
        Block[] blocks = topLevelBlocks.toArray(new Block[topLevelBlocks.size()]);
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
                    if (block.id.length == currentDepth + 1) {
                        // add to the appropriate top-level block
                        Block parent = null;
                        Block[] thisLevelsBlockArr = blocks;
                        for (int level = 0 ; level < currentDepth ; level++) {
                            for (int i = 0 ; i < thisLevelsBlockArr.length ; i++) {
                                if (thisLevelsBlockArr[i].id[level]==block.id[level]) {
                                    // if this block is an ancestor, update
                                    parent = thisLevelsBlockArr[i];
                                    thisLevelsBlockArr = parent.subBlocks;
                                } // else, skip
                            }
                        }
                        if (parent==null) throw new MalformedBlockException(strId);
                        // blocks are indexed at their value
                        int blockSize = block.id[currentDepth] + 1;
                        // put this block where it belongs
                        if (thisLevelsBlockArr==null)
                            thisLevelsBlockArr = new Block[blockSize];
                        else if (thisLevelsBlockArr.length < blockSize) {
                            // extend the array
                            Block[] newBlockArr = new Block[blockSize];
                            for (int i = 0 ; i < newBlockArr.length; i++) {
                                if (i < thisLevelsBlockArr.length)
                                    newBlockArr[i] = thisLevelsBlockArr[i];
                                else if (i==block.id[currentDepth])
                                    newBlockArr[i]=block;
                            }
                            parent.subBlocks = newBlockArr;
                        } else {
                            // add at the appropriate index
                            if (thisLevelsBlockArr[block.id[currentDepth]]!=null)
                                throw new RuntimeException(String.format("Expected block %s to be empty at position $d. Found block %s instead; could not place block %s"
                                        , parent.strId
                                        , block.id[currentDepth]
                                        , thisLevelsBlockArr[block.id[currentDepth]].strId
                                        , block.strId));
                            thisLevelsBlockArr[block.id[currentDepth]] = block;
                        }
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
    
    private static void unifyBlocks(ArrayList<CSVEntry> blockLexemes, Block[] blocks
            , ArrayList<CSVEntry> qLexemes, ArrayList<Question> questions) {
        // associate questions with the appropriate block
        CSVEntry.sort(blockLexemes);
        CSVEntry.sort(qLexemes);
        // looping this way creates more work, but we can clean it up later.
        for (int i = 0 ; i < blockLexemes.size() ; i++) {
            if (! qLexemes.get(i).contents.equals("")) {
                //out.println("ASDFASDFASDF"+i);
                int lineNo = blockLexemes.get(i).lineNo;
                if (lineNo != qLexemes.get(i).lineNo) throw new RuntimeException("ParseError");
                int[] id = getBlockIdArray(blockLexemes.get(i).contents);
                // get question corresponding to this lineno
                Question question = null;
                for (Question q : questions) 
                    if (q.sourceLineNos.contains(lineNo)) {
                        question = q; break;
                    }
                //out.println(question);
                if (question==null) throw new RuntimeException(String.format("No question found at line %d", lineNo));
                // get block corresponding to this lineno
                Block block = null;
                for (int j = 0 ; j < blocks.length ; j++){
                    block = blocks[j];
                    //out.println(Arrays.toString(block.id));
                    for (int k = 0 ; k < id.length ; k++) {
                        if (k >= block.id.length || id[k] != block.id[k]) {
                            block = null; break;
                        }
                    }
                    if (block != null)
                        break;
                }
                //out.println(block);
                if (block==null) throw new RuntimeException(String.format("No block found corresponding to %s", Arrays.toString(id)));
                question.block = block;
                block.questions.add(question);
            }
        }
    }
            
    public static Survey parse(HashMap<String, ArrayList<CSVEntry>> lexemes) {
        // create another hashmap to look up entries by line number
        Survey survey = new Survey();
        for (String key : lexemes.keySet())
            CSVEntry.sort(lexemes.get(key));
        ArrayList<Question> questions = unifyQuestions(lexemes);
        survey.questions = questions;
        if (lexemes.containsKey("BLOCK")) {
            Block[] blocks = initializeBlocks(lexemes.get("BLOCK"));
            unifyBlocks(lexemes.get("BLOCK"), blocks, lexemes.get("QUESTION"), questions);
            survey.blocks = blocks;
        }
        return survey;
    }

    public static void main(String[] args) 
            throws UnsupportedEncodingException, FileNotFoundException, IOException {
        // write test code here.
        CSVLexer.out = new PrintStream(System.out, true, CSVLexer.encoding);
        HashMap<String, ArrayList<CSVEntry>> entries = null;
        int i = 0 ;
        while(i < args.length) {
           if (i+1 < args.length && args[i+1].startsWith("--sep=")) {
               String stemp = args[i+1].substring("--sep=".length());
               if (stemp.length() > 1)
                   seperator = specialChar(stemp);
               else seperator = stemp.codePointAt(0);
               entries = lex(args[i]);
               i++;
           } else entries = lex(args[i]);
           Survey survey = parse(entries);
           out.println(survey.toString());
           i++;
           headers = null;
        }
    }
}

class MalformedBlockException extends RuntimeException {
    public MalformedBlockException(String strId) {
        super(String.format("Malformed block identifier: %s", strId));
    }
}

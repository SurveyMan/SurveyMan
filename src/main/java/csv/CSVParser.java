package csv;

import static csv.CSVLexer.*;
import java.io.*;
import java.net.MalformedURLException;
import survey.*;

import java.util.*;

class CSVParser {
    
    private static void parseOptions(Map<String, Component> options, String contents) {
        try {
            options.put(contents, new URLComponent(contents));
        } catch (MalformedURLException e){
            options.put(contents, new StringComponent(contents));
        }
    }
    
    private static ArrayList<Question> unifyQuestions(HashMap<String, ArrayList<CSVEntry>> lexemes){
        ArrayList<Question> qlist = new ArrayList<Question>();
        ArrayList<CSVEntry> questions = lexemes.get("QUESTION");
        ArrayList<CSVEntry> options = lexemes.get("OPTIONS");
        Question tempQ = null;
        for (int i = 0; i < questions.size() ; i++) {
            String contents = questions.get(i).contents;
            if ( tempQ == null && "".equals(contents) ) 
                throw new RuntimeException("No question indicated.");
            if ("".equals(contents)) {
                parseOptions(tempQ.options, contents);
            } else {
                tempQ = new Question();
                try {
                    tempQ.data = new URLComponent(contents);
                } catch (MalformedURLException e) {
                    tempQ.data = new StringComponent(contents);
                }
                tempQ.options =  new HashMap<String, Component>();
                parseOptions(tempQ.options, contents);
                // add the rest of the fields for question   
            }
        }
        return qlist;
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
                } else {
                    Block block = new Block();
                    block.strId = entry.contents;
                    // parse the block representation
                    String[] pieces = new String[1];
                    if (entry.contents.contains("."))
                        pieces = entry.contents.split(".");
                    else pieces[0] = entry.contents;
                    block.id = new int[pieces.length];
                    for (int i = 0 ; i < pieces.length ; i ++) {
                        out.println("x"+pieces[i]+"x"+pieces.length+"x"+entry.contents.contains("."));
                        block.id[i] = Integer.parseInt(pieces[i]);
                    }
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
    
    public static Survey parse(HashMap<String, ArrayList<CSVEntry>> lexemes) {
        Survey survey = new Survey();
        for (String key : lexemes.keySet())
            CSVEntry.sort(lexemes.get(key));
        ArrayList<Question> questions = unifyQuestions(lexemes);
        survey.questions = questions;
        if (lexemes.containsKey("BLOCK")) {
            Block[] blocks = initializeBlocks(lexemes.get("BLOCK"));
            survey.blocks = blocks;
        }
        return survey;
    }

    public static void main(String[] args) 
            throws UnsupportedEncodingException, FileNotFoundException, IOException {
        // write test code here.
        CSVLexer.out  = new PrintStream(System.out, true, CSVLexer.encoding);
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

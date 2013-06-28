package csv;

import static csv.CSVLexer.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import survey.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class CSVParser {
    
    public static void parseOptions(Map<String, Component> options, String contents) {
        try {
            options.put(contents, new URLComponent(contents));
        } catch (MalformedURLException e){
            options.put(contents, new StringComponent(contents));
        }
    }
    
    public static ArrayList<Question> unifyQuestions(HashMap<String, ArrayList<CSVEntry>> lexemes){
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
    
    public static Survey parse(HashMap<String, ArrayList<CSVEntry>> lexemes) {
        for (String key : lexemes.keySet())
            CSVEntry.sort(lexemes.get(key));
        ArrayList<Question> questions = unifyQuestions(lexemes);
//        Block[] blocks = initializeBlocks(lexemes.get("BLOCK"));
        return new Survey();
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
           i++;
           headers = null;
        }
    }

}
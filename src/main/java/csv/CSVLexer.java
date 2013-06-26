package csv;

import java.io.*;
import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.lang.RuntimeException;
import java.util.ArrayList;
import java.util.List;
import utils.Gensym;
import scala.collection.Seq;
import scalautils.QuotMarks;

public class CSVLexer {

    private static PrintStream out;
    public static String seperator = ",";
    public static final String[] knownHeaders =
            {"QUESTION", "BLOCK", "OPTIONS", "RESOURCE", "EXCLUSIVE", "ORDERED", "PERTURB", "BRANCH"};

    private static boolean inQuot(String line) {
        // searches for quotation marks
        // since there are multiple possibilities for the right qmark,
        // consider the first match the matching one
        // only care about the outer quotation.
        char[] c = line.toCharArray();
        boolean inQ = false;
        String lqmark = null;
        Seq<String> rqmarks = null;
        int i = 0;
        while (i < c.length) {
            String s = String.valueOf(c[i]);
            if (QuotMarks.isA(s)) {
                if (inQ) {
                    assert (rqmarks!=null);
                    if (rqmarks.contains(s)) {
                        if (i+1 < c.length && c[i]==c[i+1]) // valid escape seq
                            i++;
                        else inQ = false; // else close quot
                    }
                } else {
                    // if I'm not already in a quote, check whether this is a 2-char quot.
                    if (i + 1 < c.length && QuotMarks.isA(s + String.valueOf(c[i+1]))) {
                        lqmark = s + String.valueOf(c[i+1]); i++;
                    } else lqmark = s ;
                    inQ=true ; rqmarks = QuotMarks.getMatch(lqmark);
                }
            }
            i++;
            // out.print(i+" ");
        }
        return inQ;
    }

    public static ArrayList<ArrayList<CSVEntry>> lex(String filename) 
            throws FileNotFoundException, IOException, RuntimeException {
        // FileReader uses the system's default encoding.
        // BufferedReader makes 16-bit chars
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String[] headers = null;
        ArrayList<ArrayList<CSVEntry>> entries = null;
        String line = "";
        int lineno = 1;
        while((line = br.readLine()) != null) {
            if (headers==null) {
                Gensym gensym = new Gensym("col");
                headers = line.split(CSVLexer.seperator);
                for (int i = 0; i < headers.length ; i++) {
                    headers[i] = headers[i].trim().toUpperCase();
                    if (headers[i].equals(""))
                        headers[i] = gensym.next();
                    else { // make sure it doesn't contain quotes
                        for (int j = 0; j < headers[i].length() ; j++) {
                            if (QuotMarks.isA(headers[i].substring(j, j+1))
                                || ((j+1 < headers[i].length()) && QuotMarks.isA(headers[i].substring(j, j+2))))
                                throw new RuntimeException("Headers cannot contain quotation marks : "+headers[i]);
                        }
                    }
                }
                for (int i = 0 ; i < headers.length ; i++)
                    out.println(headers[i]);
            } else {
                // check to make sure this isn't a false alarm where we're in a quot
                while (CSVLexer.inQuot(line)) {
                    String newLine = br.readLine();
                    if (newLine != null)
                        line  = line + newLine;
                    else throw new EOFException("Malformed quotation in: " + line + ".");
                }
                //out.println("\t"+lineno+":\t"+line);
                lineno+=1;
            }
        }
        out.println(filename+": "+(lineno-1)+"\t"+CSVLexer.seperator);
        return new ArrayList<ArrayList<CSVEntry>>();
    }

    public static void main(String[] args) 
            throws FileNotFoundException, IOException, UnsupportedEncodingException {
        //write test code here
        CSVLexer.out = new PrintStream(System.out, true, "UTF-8");
        int i = 0 ;
        while(i < args.length) {
           if (i+1 < args.length && args[i+1].startsWith("--sep=")) {
               CSVLexer.seperator = args[i+1].substring("--sep=".length());
               lex(args[i]); i++;
           } else lex(args[i]);
           i++;
        }
    }
}
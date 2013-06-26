package csv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import scala.collection.Seq;
import scalautils.QuotMarks;

public class CSVLexer {

    public final String seperator = ",";

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
                    if (i+1 < c.length && QuotMarks.isA(s + String.valueOf(c[i+1]))) {
                        lqmark = s + String.valueOf(c[i+1]); i++;
                    } else lqmark = s ;
                    inQ=true ; rqmarks = QuotMarks.getMatch(lqmark);
                }
            }
            i++;
        }
        return inQ;
    }

    public static ArrayList<ArrayList<CSVEntry>> lex(String filename) 
            throws FileNotFoundException, IOException {
        // FileReader uses the system's default encoding.
        // BufferedReader makes 16-bit chars
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String[] headers = null;
        String line = "";
        while((line = br.readLine()) != null) {
            // check to make sure this isn't a false alarm where we're in a quot
            while (CSVLexer.inQuot(line))
                line  = line + br.readLine();
        }
        return new ArrayList<ArrayList<CSVEntry>>();
    }

    public static void main(String[] args) 
            throws FileNotFoundException, IOException {
        //write test code here
        ArrayList<ArrayList<CSVEntry>> retval = lex(args[0]);
   }
}
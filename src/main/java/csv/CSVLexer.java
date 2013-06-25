package csv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.String;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import csv.CSVEntry;
import QuotMarks;

public class CSVLexer {

    public final String seperator;

    public static final String[] knownHeaders =
            {"QUESTION", "BLOCK", "OPTIONS", "RESOURCE", "EXCLUSIVE", "ORDERED", "PERTURB", "BRANCH"};

    private boolean inQuot(String line) {
        // searches for quotation marks
        // since there are multiple possibilities for the right qmark,
        // consider the first match the matching one
        // only care about the outer quotation.
        String[] c = line.toCharArray();
        boolean inQ = false;
        String lqmark = null;
        List<String> rqmarks = null;
        int i = 0;
        while (i < c.length) {
            String s = new String(c[i]);
            if (QuotMarks.isA(s)) {
                if (inQ) {
                    if (rqmarks.contains(s)) {
                        if (i+1 < c.length && c[i]==c[i+1]) // valid escape seq
                            i++;
                        else inQ = false; // else close quot
                    }
                } else {
                    // if I'm not already in a quote, check whether this is a 2-char quot.
                    if (i+1 < c.length && QuotMars.isA(s + new String(c[i+1]))) {
                        lqmark = sprime; i++;
                    } else lqmark = s ;
                    inQ=true ; rqmarks = QuotMarks.getMatch(lqmark);
                }
            }
            i++;
        }
        return inQ;
    }

    public static ArrayList<List<CSVEntry>> lex(String filename) {
        // FileReader uses the system's default encoding.
        // BufferedReader makes 16-bit chars
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String[] headers = null;
        String line = "";
        while((line = br.readLine()) != null) {
            // check to make sure this isn't a false alarm where we're in a quot
            while (inQuot(line))
                line  = line + br.readLine();
        }
        return new ArrayList<List<CSVEntry>>();
    }

    public static void main(String[] args) {
        //write test code here
        System.out.println("Hello, World!");
    }
}
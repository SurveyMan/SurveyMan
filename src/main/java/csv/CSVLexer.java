package csv;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import utils.Gensym;
import scala.collection.Seq;
import scalautils.QuotMarks;

public class CSVLexer {

    protected static PrintStream out;
    public static final String encoding = "UTF-8";
    public static int seperator = ",".codePointAt(0);
    public static final String[] knownHeaders =
            {"QUESTION", "BLOCK", "OPTIONS", "RESOURCE", "EXCLUSIVE", "ORDERED", "PERTURB", "BRANCH"};
    public static String[] headers = null;
    

    private static String sep2string() {
        return Character.toString((char) seperator);
    }

    private static boolean inQuot(String line) {
        // searches for quotation marks
        // since there are multiple possibilities for the right qmark,
        // consider the first match the matching one
        // only care about the outer quotation.
        char[] c = line.toCharArray();
        boolean inQ = false;
        String lqmark = "";
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

    private static String[] getHeaders (String line) {
        Gensym gensym = new Gensym("GENCOLHEAD");
        String[] headers = line.split(sep2string());
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
        for (int i = 0 ; i < headers.length ; i++ ) {
            boolean in = false;
            for (int j = 0 ; j < knownHeaders.length ; j++)
                if (headers[i].equals(knownHeaders[j])) {
                    in = true; break;
                }
            if (!in) out.println(String.format("WARNING: Column header %s has no known semantics.", headers[i]));
        }
        return headers;
    }


    private static void clean (HashMap<String, ArrayList<CSVEntry>> entries) {
        for (String key : entries.keySet()){
            // all entries need to have the beginning/trailing seperator and whitespace removed
            for (CSVEntry entry : entries.get(key)) {
                if (entry.contents.endsWith(sep2string()))
                    entry.contents = entry.contents.substring(0, entry.contents.length()-sep2string().length());
                if (entry.contents.startsWith(sep2string()))
                    entry.contents = entry.contents.substring(sep2string().length());
                entry.contents.trim();
            }
        }
    }

    public static HashMap<String, ArrayList<CSVEntry>> lex(String filename)
            throws FileNotFoundException, IOException, RuntimeException {
        // FileReader uses the system's default encoding.
        // BufferedReader makes 16-bit chars
        BufferedReader br = new BufferedReader(new FileReader(filename));
        HashMap<String, ArrayList<CSVEntry>> entries = null;
        String line = "";
        int lineno = 0;
        while((line = br.readLine()) != null) {
            lineno+=1;
            if (headers==null) {
                headers = getHeaders(line);
                entries = new HashMap<String, ArrayList<CSVEntry>>(headers.length);
                for (int i = 0 ; i < headers.length ; i++)
                    entries.put(headers[i], new ArrayList<CSVEntry>());
            } else {
                // check to make sure this isn't a false alarm where we're in a quot
                // this is super inefficient, but whatever, we'll make it better later or maybe we won't notice.
                while (inQuot(line)) {
                    String newLine = br.readLine();
                    lineno += 1;
                    if (newLine != null)
                        line  = line + newLine;
                    else throw new MalformedQuotationException(line);
                }
                // for each header, read an entry.
                String entry = null;
                String restOfLine = line;
                for (int i = 0 ; i < headers.length ; i ++) {
                    if (i == headers.length - 1) {
                        if (inQuot(restOfLine)) throw new MalformedQuotationException(restOfLine);
                        entries.get(headers[i]).add(new CSVEntry(restOfLine, lineno, i));
                    } else {
                        int a = restOfLine.indexOf(Character.toString((char) seperator));
                        int b = 1;
                        if (a == -1) {
                            out.println(String.format("WARNING: seperator '%s'(unicode:%s) not found in line %d:\n\t (%s)."
                                    , Character.toString((char) seperator)
                                    , String.format("\\u%04x", seperator)
                                    , lineno
                                    , line));
                        }
                        entry = restOfLine.substring(0, a + b);
                        restOfLine = restOfLine.substring(entry.length());
                        while (inQuot(entry)) {
                            if (restOfLine.equals("")) throw new MalformedQuotationException(entry);
                            a = restOfLine.indexOf(Character.toString((char) seperator));
                            entry = entry + restOfLine.substring(0, a + b);
                            restOfLine = restOfLine.substring(a + b);
                        }
                        entries.get(headers[i]).add(new CSVEntry(entry, lineno, i));
                    }
                }
            }
        }
        out.println(filename+": "+(lineno-1)+" "+Character.toString((char) seperator)+" ");
        clean(entries);
        return entries;
    }

    protected static int specialChar(String stemp) {
        if (stemp.codePointAt(0)!=0x5C)
            throw new FieldSeperatorException(stemp);
        switch (stemp.charAt(1)) {
            case 't': return 0x9;
            case 'b' : return 0x8;
            case 'n' : return 0xA;
            case 'r' : return 0xD;
            case 'f' : return 0xC;
            case 'u' : return Integer.decode(stemp.substring(2, 5));
            default: throw new FieldSeperatorException(stemp);
        }
    }

    public static void main(String[] args) 
            throws FileNotFoundException, IOException, UnsupportedEncodingException, RuntimeException {
        //write test code here
        out = new PrintStream(System.out, true, encoding);
        int i = 0 ;
        HashMap<String, ArrayList<CSVEntry>> entries;
        while(i < args.length) {
           if (i+1 < args.length && args[i+1].startsWith("--sep=")) {
               String stemp = args[i+1].substring("--sep=".length());
               if (stemp.length() > 1)
                   seperator = specialChar(stemp);
               else seperator = stemp.codePointAt(0);
               entries = lex(args[i]);
               i++;
           } else entries = lex(args[i]);
           out.println("headers: " + entries.keySet());
           for (int j = 0 ; j < headers.length ; j++ ) {
               ArrayList<CSVEntry> thisCol = entries.get(headers[j]);
               int sps = 15 - headers[j].length();
               String ssps = ""; while(sps>0) { ssps+=" "; sps--; }
               out.println(headers[j]+ssps+thisCol.get(0)+"..."+thisCol.get(thisCol.size()-1));
           }
           i++;
           headers = null;
        }
    }
}

class MalformedQuotationException extends RuntimeException {
    public MalformedQuotationException(String line) {
        super("Malformed quotation in :"+line);
    }
}
class FieldSeperatorException extends RuntimeException {
    public FieldSeperatorException(String seperator) {
        super(seperator.startsWith("\\")?
                "Illegal sep: " + seperator
                        + " is " + seperator.length()
                        + " chars and " + seperator.getBytes().length
                        + " bytes."
                : "Illegal escape char (" + seperator.charAt(0)
                + ") in sep " + seperator );
    }
}
package csv;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import survey.SurveyException;
import system.Bug;
import system.Debugger;
import utils.Gensym;
import utils.Out;
import scala.collection.Seq;
import scalautils.QuotMarks;

public class CSVLexer {


    static class MalformedQuotationException extends SurveyException implements Bug {
        public Object caller;
        public Method lastAction;
        public MalformedQuotationException(int row, int column, String msg, CSVLexer lexer, Method method) {
            super(String.format("Malformed quotation in cell (%d,%d) : %s."
                    , row
                    , column
                    , msg));
            Debugger.addBug(this);
        }
        public MalformedQuotationException(String msg, CSVLexer lexer, Method method){
            super(msg);
            Debugger.addBug(this);
        }
        public Object getCaller(){
            return caller;
        }
        public Method getLastAction(){
            return lastAction;
        }
    }
    static class FieldSeparatorException extends SurveyException implements Bug {
        public Object caller;
        public Method lastAction;
        public FieldSeparatorException(String separator, CSVLexer lexer, Method method) {
            super(separator.startsWith("\\")?
                    "Illegal sep: " + separator
                            + " is " + separator.length()
                            + " chars and " + separator.getBytes().length
                            + " bytes."
                    : "Illegal escape char (" + separator.charAt(0)
                    + ") in sep " + separator);
            caller = lexer;
            lastAction = method;
            Debugger.addBug(this);
        }
        public Object getCaller(){
            return caller;
        }
        public Method getLastAction(){
            return lastAction;
        }
    }
    class CSVColumnException extends SurveyException implements Bug{
        Object caller;
        Method lastAction;
        public CSVColumnException(String colName, CSVLexer lexer, Method method) {
            super(String.format("CSVs column headers must contain a %s column. You may have chosen an incorrect field delimiter."
                    , colName.toUpperCase()));
            caller = lexer;
            lastAction = method;
            Debugger.addBug(this);
        }
        public Object getCaller(){
            return caller;
        }
        public Method getLastAction(){
            return lastAction;
        }
    }
    class HeaderException extends SurveyException implements Bug {
        Object caller;
        Method lastAction;
        public HeaderException(String msg, CSVLexer lexer, Method method) {
            super(msg);
            caller = lexer;
            lastAction = method;
            Debugger.addBug(this);
        }
        public Object getCaller(){
            return caller;
        }
        public Method getLastAction(){
            return lastAction;
        }
    }

    private static final Logger LOGGER = Logger.getLogger("csv");
    public static final String QUESTION = "QUESTION";
    public static final String BLOCK = "BLOCK";
    public static final String OPTIONS = "OPTIONS";
    public static final String RESOURCE = "RESOURCE";
    public static final String EXCLUSIVE = "EXCLUSIVE";
    public static final String ORDERED = "ORDERED";
    public static final String PERTURB = "PERTURB";
    public static final String RANDOMIZE = "RANDOMIZE";
    public static final String BRANCH = "BRANCH";
    public static final String FREETEXT = "FREETEXT";
    public static final String[] knownHeaders = {QUESTION, BLOCK, OPTIONS, RESOURCE, EXCLUSIVE, ORDERED, PERTURB, RANDOMIZE, BRANCH, FREETEXT};
    public static HashMap<String, String> xmlChars = new HashMap<String, String>();
    static {
        xmlChars.put("<", "&lt;");
        xmlChars.put(">", "&gt;");
        xmlChars.put("&", "&amp;");
        QuotMarks.addQuots(xmlChars);
    }

    private int quots2strip = 0;
    public String encoding;
    public String sep;
    public String filename;
    public String[] headers;
    public HashMap<String, ArrayList<CSVEntry>> entries;
    private int startingLine = 0;

    public CSVLexer(String sep, String filename, String encoding) throws IOException, SurveyException {
        this.sep = sep;
        this.filename = filename;
        this.encoding = encoding;
        this.headers = getHeaders();
        this.entries = lex(filename);
    }

    public CSVLexer(String sep, String filename) throws IOException, SurveyException {
        this(sep, filename, "UTF-8");
    }

    private static String sep2string(int sep) {
        return Character.toString((char) sep);
    }

    private static boolean inQuot(String line) {
        // searches for quotation marks
        // since there are multiple possibilities for the right qmark,
        // consider the first match the matching one
        // only care about the outer quotation.
        char[] c = line.toCharArray();
        boolean inQ = false;
        String lqmark = "";
        ArrayList<String> rqmarks = null;
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

    private String stripQuots(String text, boolean header) throws SurveyException {
        String txt = text;
        if (header) {
            int qs = 0;
            while (QuotMarks.isA(txt.substring(0,1))) {
                boolean matchFound = false;
                for (String quot : QuotMarks.getMatch(txt.substring(0,1))) {
                    if (txt.endsWith(quot)) {
                        txt = txt.substring(1, txt.length() - 1);
                        qs++; matchFound = true; break;
                    }
                }
                if (!matchFound) throw new HeaderException("Matching wrapped quotation marks not found : " + text, this, this.getClass().getEnclosingMethod());
            }
            if (qs > quots2strip)
                quots2strip = qs;
        } else {
            // will try to strip up to quots2strip
            for (int i = 0 ; i < quots2strip ; i ++) {
                boolean matchFound = false;
                String maybeQuot = txt.substring(0,1);
                if (QuotMarks.isA(maybeQuot)) {
                    for (String quot : QuotMarks.getMatch(maybeQuot)) {
                        if (txt.endsWith(quot)) {
                            txt = txt.substring(1, txt.length() - 1);
                            matchFound = true; break;
                        }
                    }
                } else continue;
                if (!matchFound) throw new MalformedQuotationException("Matching wrapped quotation marks not found : "+ text, this, this.getClass().getEnclosingMethod());
            }
        }
        return txt.trim();
    }

    private String[] getHeaders() throws SurveyException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(this.filename));
        String line;
        do {
            line = br.readLine();
            this.startingLine++;
        } while (line==null || line.equals(""));
        br.close();
        Gensym gensym = new Gensym("GENCOLHEAD");
        String[] headers = line.split(sep2string(specialChar(this.sep, this)));
        for (int i = 0; i < headers.length ; i++) {
            headers[i] = headers[i].trim().toUpperCase();
            if (headers[i].equals(""))
                headers[i] = gensym.next();
            else {
                // strip quotes
                headers[i] = stripQuots(headers[i], true);
                // make sure it doesn't contain quotes
                for (int j = 0; j < headers[i].length() ; j++) {
                    if (QuotMarks.isA(headers[i].substring(j, j+1))
                            || ((j+1 < headers[i].length()) && QuotMarks.isA(headers[i].substring(j, j+2))))
                        throw new HeaderException("Headers cannot contain quotation marks : "+headers[i], this, this.getClass().getEnclosingMethod());
                }
            }
        }
        return headers;
    }

    public static String xmlChars2HTML(String s) {
        s = s.replaceAll("&", xmlChars.get("&"));
        for (Map.Entry<String, String> e : xmlChars.entrySet())
            if (! e.getKey().equals("&"))
                s = s.replaceAll(e.getKey(), e.getValue());
        return s;
    }

    public static String htmlChars2XML(String s) {
        for (Map.Entry<String, String> e : xmlChars.entrySet())
            s = s.replaceAll(e.getValue(), e.getKey());
        return s;
    }

    private static void clean (HashMap<String, ArrayList<CSVEntry>> entries, CSVLexer lexer) throws SurveyException {
        for (String key : entries.keySet()){
            // all entries need to have the beginning/trailing separator and whitespace removed
            for (CSVEntry entry : entries.get(key)) {
                if (entry.contents.endsWith(sep2string(specialChar(lexer.sep, lexer))))
                    entry.contents = entry.contents.substring(0, entry.contents.length()-sep2string(specialChar(lexer.sep, lexer)).length());
                if (entry.contents.startsWith(sep2string(specialChar(lexer.sep, lexer))))
                    entry.contents = entry.contents.substring(sep2string(specialChar(lexer.sep, lexer)).length());
                entry.contents = entry.contents.trim();
                // remove beginning/trailing quotation marks
                if (entry.contents.length() > 0 ) {
                    for (int i = 0 ; i < lexer.quots2strip ; i ++) {
                        int len = entry.contents.length();
                        String lquot = entry.contents.substring(0,1);
                        String rquot = entry.contents.substring(len-1, len);
                        boolean foundMatch = false;
                        if (! QuotMarks.isA(lquot)) {
                            SurveyException e = new MalformedQuotationException(entry.lineNo
                                    , entry.colNo
                                    , String.format("entry (%s) does not begin with a known quotation mark", entry.contents)
                                    , lexer
                                    , lexer.getClass().getEnclosingMethod()
                            );
                            LOGGER.warn(e);
                            break;
                        }
                        for (String quot : QuotMarks.getMatch(lquot)){
                            if (entry.contents.endsWith(quot)) {
                                foundMatch = true; break;
                            }
                        }
                        if (! foundMatch) {
                            SurveyException e = new MalformedQuotationException(entry.lineNo
                                    , entry.colNo
                                    , String.format("entry (%s) does not have matching quotation marks.", entry.contents)
                                    , lexer
                                    , lexer.getClass().getEnclosingMethod()
                            );
                            LOGGER.fatal(e);
                            throw e;
                        }
                        entry.contents = entry.contents.substring(1, len-1);
                    }
                }
            }
        }
    }

    private static HashMap<String, ArrayList<CSVEntry>> initializeEntries(String[] headers) {
        HashMap<String, ArrayList<CSVEntry>> entries = new HashMap<String, ArrayList<CSVEntry>>();
        for (int i = 0 ; i < headers.length ; i++)
            entries.put(headers[i], new ArrayList<CSVEntry>());
        return entries;
    }

    public HashMap<String, ArrayList<CSVEntry>> lex(String filename)
            throws FileNotFoundException, IOException, RuntimeException, SurveyException {
        // FileReader uses the system's default encoding.
        // BufferedReader makes 16-bit chars
        BufferedReader br = new BufferedReader(new FileReader(filename));
        HashMap<String, ArrayList<CSVEntry>> entries = initializeEntries(this.headers);
        String line = br.readLine();
        for(int lineno = 0 ; line != null ; line = br.readLine(), lineno++) {
            // check to make sure this isn't a false alarm where we're in a quot
            // this isn't super inefficient, but whatever, we'll make it better later or maybe we won't notice.
            if (lineno < this.startingLine)
                continue;
            while (inQuot(line)) {
                String newLine = br.readLine();
                lineno += 1;
                if (newLine != null)
                    line  = line + newLine;
                else {
                    SurveyException e = new MalformedQuotationException(String.format("Malformed quotation at line %d : %s", lineno, line)
                        , this
                        , this.getClass().getEnclosingMethod());
                    LOGGER.fatal(e);
                    throw e;
                }
            }
            // for each header, read an entry.
            String entry = null;
            String restOfLine = line;
            for (int i = 0 ; i < headers.length ; i ++) {
                if (i == headers.length - 1) {
                    if (inQuot(restOfLine)) {
                        SurveyException e = new MalformedQuotationException(String.format("Malformed quotation at line %d : %s", lineno, restOfLine)
                                , this
                                , this.getClass().getEnclosingMethod());
                        LOGGER.fatal(e);
                        throw e;
                    }
                    entries.get(headers[i]).add(new CSVEntry(restOfLine, lineno, i));
                } else {
                    int a = restOfLine.indexOf(Character.toString((char) specialChar(this.sep, this)));
                    int b = 1;
                    if (a == -1) {
                        LOGGER.warn(String.format("separator '%s'(unicode:%s) not found in line %d:\n\t (%s)."
                                    , Character.toString((char) specialChar(this.sep, this))
                                    , String.format("\\u%04x", specialChar(this.sep, this))
                                    , lineno
                                    , line));
                        }
                        entry = restOfLine.substring(0, a + b);
                        restOfLine = restOfLine.substring(entry.length());
                        while (inQuot(entry)) {
                            if (restOfLine.equals("")) {
                                SurveyException e = new MalformedQuotationException(String.format("Malformed quotation at line %d : %s", lineno, entry)
                                        , this
                                        , this.getClass().getEnclosingMethod());
                                LOGGER.fatal(e);
                                throw e;
                            }
                            a = restOfLine.indexOf(Character.toString((char) specialChar(this.sep, this)));
                            entry = entry + restOfLine.substring(0, a + b);
                            restOfLine = restOfLine.substring(a + b);
                        }
                        try{
                            entries.get(headers[i]).add(new CSVEntry(entry, lineno, i));
                        } catch (NullPointerException e) {
                            LOGGER.warn(String.format("NPE for header [%s] and entry [%s], csv source lineno %d"
                                    ,headers[i]
                                    , entry
                                    , lineno));
                        }
                    }
                }
            }
        clean(entries, this);
        if (! entries.keySet().contains(QUESTION)) {
            SurveyException e = new CSVColumnException(QUESTION, this, this.getClass().getEnclosingMethod());
            LOGGER.fatal(e);
            throw e;
        }
        if (! entries.keySet().contains(OPTIONS)) {
            SurveyException e = new CSVColumnException(OPTIONS, this, this.getClass().getEnclosingMethod());
            LOGGER.fatal(e);
            throw e;
        }
        return entries;
    }

    protected static int specialChar(String stemp, CSVLexer caller) throws SurveyException{
        if (stemp.codePointAt(0)!=0x5C) throw new FieldSeparatorException(stemp, caller, caller.getClass().getEnclosingMethod());
        switch (stemp.charAt(1)) {
            case 't': return 0x9;
            case 'b' : return 0x8;
            case 'n' : return 0xA;
            case 'r' : return 0xD;
            case 'f' : return 0xC;
            case 'u' : return Integer.decode(stemp.substring(2, 5));
            default: throw new FieldSeparatorException(stemp, caller, caller.getClass().getEnclosingMethod());
        }
    }
}

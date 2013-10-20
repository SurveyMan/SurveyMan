package csv;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.IsIncludedIn;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import survey.SurveyException;
import system.Bug;
import system.Debugger;
import utils.Gensym;
import scalautils.QuotMarks;
import org.supercsv.io.CsvListReader;

public class CSVLexer {

    /** Inner/nested classes */

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

    /** static fields */
    private static final Logger LOGGER = Logger.getLogger(CSVLexer.class);
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
    final public static String[] trueValues = {"yes", "y", "true", "t", "1"};
    final public static String[] falseValues = {"no", "n", "false", "f", "0"};

    public static HashMap<String, String> xmlChars = new HashMap<String, String>();
    static {
        xmlChars.put("<", "&lt;");
        xmlChars.put(">", "&gt;");
        xmlChars.put("&", "&amp;");
        QuotMarks.addQuots(xmlChars);
    }

    /** instance fields */
    private int quots2strip = 0;
    private String fieldQuot = "\"";
    public String encoding;
    public String sep;
    public String filename;
    public String[] headers;
    public HashMap<String, ArrayList<CSVEntry>> entries;

    /** constructors */
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

    /** static methods */
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

    private static String sep2string(int sep) {
        return Character.toString((char) sep);
    }

    private static HashMap<String, ArrayList<CSVEntry>> initializeEntries(String[] headers) {
        HashMap<String, ArrayList<CSVEntry>> entries = new HashMap<String, ArrayList<CSVEntry>>();
        for (int i = 0 ; i < headers.length ; i++)
            entries.put(headers[i], new ArrayList<CSVEntry>());
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

    /** instance methods */
    private String[] getHeaders() throws SurveyException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(this.filename));
        String line;
        do {
            line = br.readLine();
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
                //headers[i] = stripQuots(headers[i], true);
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

    private CellProcessor[] makeProcessors() {
        // returns a list of processors for the appropriate column type
        CellProcessor[] cellProcessors = new CellProcessor[headers.length];
        String[] truthValues = new String[trueValues.length+falseValues.length];
        System.arraycopy(trueValues, 0, truthValues, 0, trueValues.length);
        System.arraycopy(falseValues, 0, truthValues, trueValues.length, falseValues.length);

        for (int i = 0 ; i < headers.length ; i++){
            String header = headers[i];
            if (header.equals(CSVLexer.OPTIONS))
                cellProcessors[i] = new NotNull();

            else if (header.equals(CSVLexer.BLOCK)
                    || header.equals(CSVLexer.BRANCH))
                cellProcessors[i] = new Optional(new StrRegEx("[1-9][0-9]*(\\.[1-9][0-9]*)*"));

            else if (header.equals(CSVLexer.EXCLUSIVE)
                    || headers.equals(CSVLexer.FREETEXT)
                    || headers.equals(CSVLexer.ORDERED)
                    || headers.equals(CSVLexer.PERTURB)
                    || headers.equals(CSVLexer.RANDOMIZE))
                cellProcessors[i] = new Optional(new IsIncludedIn(truthValues));

            else
                cellProcessors[i] = new Optional();
        }

        return cellProcessors;
    }

    private HashMap<String, ArrayList<CSVEntry>> lex(String filename)
            throws FileNotFoundException, IOException, RuntimeException, SurveyException {

        final CsvPreference pref = new CsvPreference.Builder((char) specialChar(fieldQuot, this), specialChar(sep, this), "\n").build();
        ICsvListReader csvReader = new CsvListReader(new FileReader(filename), pref);
        csvReader.getHeader(true); // skips the header column
        final CellProcessor[] processors = makeProcessors();

        HashMap<String, ArrayList<CSVEntry>> entries = initializeEntries(this.headers);

        List<Object> line;
        while ((line = csvReader.read(processors))!=null) {
            // for each header, read an entry.
            int lineNo = csvReader.getLineNumber();
            String entry = null;
            for (int colNo = 0 ; colNo < line.size() ; colNo++) {
                CSVEntry csvEntry = new CSVEntry((String) line.get(colNo), lineNo, colNo+1);
                ArrayList<CSVEntry> csvEntries = entries.get(headers[colNo]);
                csvEntries.add(csvReader.getRowNumber()-1, csvEntry);
                entries.put(headers[colNo], csvEntries);
            }
        }
        return entries;
    }

}

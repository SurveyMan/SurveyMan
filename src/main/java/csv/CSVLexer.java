package csv;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.IsIncludedIn;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.io.CsvListReader;
import survey.Survey;
import survey.SurveyException;
import system.Bug;
import system.Debugger;
import system.Gensym;
import scalautils.QuotMarks;

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
            this.caller = lexer;
            this.lastAction = method;
            Debugger.addBug(this);
        }
        public MalformedQuotationException(String msg, CSVLexer lexer, Method method){
            super(msg);
            this.caller = lexer;
            this.lastAction = method;
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
    static class CSVColumnException extends SurveyException implements Bug{
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
    static class HeaderException extends SurveyException implements Bug {
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
    public CSVLexer(String filename, String sep, String encoding)
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this.sep = sep;
        this.filename = filename;
        this.encoding = encoding;
        this.headers = getHeaders();
        this.entries = lex(filename);
    }

    public CSVLexer(String filename, String sep)
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this(filename, sep, "UTF-8");
    }

    /** static methods */
    public static String xmlChars2HTML(String s) {
        LOGGER.debug(String.format("Replace XML chars with HTML (%s)", s));
        if (s==null)
            return "";
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

    private static HashMap<String, ArrayList<CSVEntry>> initializeEntries(String[] headers) {
        HashMap<String, ArrayList<CSVEntry>> entries = new HashMap<String, ArrayList<CSVEntry>>();
        for (int i = 0 ; i < headers.length ; i++)
            entries.put(headers[i], new ArrayList<CSVEntry>());
        return entries;
    }

    /** instance methods */
    private String stripHeaderQuots(String text) throws SurveyException {
        String txt = text;
        int qs = 0;
        while (txt.length()>0 && QuotMarks.isA(txt.substring(0,1))) {
            boolean matchFound = false;
            for (String quot : QuotMarks.getMatch(txt.substring(0,1))) {
                if (txt.endsWith(quot)) {
                    txt = txt.substring(1, txt.length() - 1);
                    qs++; matchFound = true; break;
                }
            }
            if (!matchFound) {
                SurveyException e = new HeaderException("Matching wrapped quotation marks not found : " + text, this, this.getClass().getEnclosingMethod());
                LOGGER.fatal(e);
                throw e;
            }
        }
        if (qs > quots2strip)
            quots2strip = qs;
        return txt.trim();
    }

    private String[] getHeaders() throws SurveyException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(this.filename));
        String line;
        do {
            line = br.readLine();
        } while (line==null || line.equals(""));
        br.close();
        Gensym gensym = new Gensym("GENCOLHEAD");
        String[] headers = line.split(this.sep);
        for (int i = 0; i < headers.length ; i++) {
            headers[i] = stripHeaderQuots(headers[i]).trim().toUpperCase();
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

    private String[] mapStringOp(String[] input, Method m) throws InvocationTargetException, IllegalAccessException {
        String[] retval = new String[input.length];
        for (int i = 0 ; i < retval.length ; i++)
            retval[i] = (String) m.invoke(input[i]);
        return retval;
    }

    private CellProcessor[] makeProcessors() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // returns a list of processors for the appropriate column type
        CellProcessor[] cellProcessors = new CellProcessor[headers.length];
        String[] truthValues = new String[(trueValues.length+falseValues.length)*2];
        Method upperCase = String.class.getMethod("toUpperCase");
        System.arraycopy(trueValues, 0, truthValues, 0, trueValues.length);
        System.arraycopy(falseValues, 0, truthValues, trueValues.length, falseValues.length);
        System.arraycopy(mapStringOp(trueValues, upperCase), 0, truthValues, trueValues.length+falseValues.length, trueValues.length);
        System.arraycopy(mapStringOp(falseValues, upperCase), 0, truthValues, (trueValues.length*2)+falseValues.length, falseValues.length);

        for (int i = 0 ; i < headers.length ; i++){
            String header = headers[i];

            if (header.equals(Survey.BLOCK)
                    || header.equals(Survey.BRANCH))
                cellProcessors[i] = new Optional(new StrRegEx("_?[1-9][0-9]*(\\._?[1-9][0-9]*)*"));

            else if (header.equals(Survey.EXCLUSIVE)
                    || headers.equals(Survey.FREETEXT)
                    || headers.equals(Survey.ORDERED)
                    || headers.equals(Survey.RANDOMIZE))
                cellProcessors[i] = new Optional(new IsIncludedIn(truthValues));

            else
                cellProcessors[i] = new Optional();
        }

        return cellProcessors;
    }

    private HashMap<String, ArrayList<CSVEntry>> lex(String filename)
            throws IOException, RuntimeException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        final CsvPreference pref = new CsvPreference.Builder(fieldQuot.toCharArray()[0], sep.codePointAt(0), "\n").build();
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
                csvEntries.add(csvEntries.size(), csvEntry);
                entries.put(headers[colNo], csvEntries);
            }
        }
        return entries;
    }

}

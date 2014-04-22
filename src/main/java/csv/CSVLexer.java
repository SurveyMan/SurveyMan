package csv;

import org.apache.log4j.Logger;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.IsIncludedIn;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import survey.Survey;
import survey.SurveyException;
import system.Bug;
import system.Debugger;
import system.Gensym;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CSVLexer {

    /** Inner/nested classes */
    static class HeaderException extends SurveyException implements Bug {
        Object caller;
        Method lastAction;
        public HeaderException(String msg, CSVLexer lexer, Method method) {
            super(msg);
            caller = lexer;
            lastAction = method;
            Debugger.addBug(this);
        }
        @Override
        public Object getCaller(){
            return caller;
        }
        @Override
        public Method getLastAction(){
            return lastAction;
        }
    }

    /** static fields */
    private static final Logger LOGGER = Logger.getLogger(CSVLexer.class);
    final public static String[] trueValues = {"yes", "y", "true", "t", "1"};
    final public static String[] falseValues = {"no", "n", "false", "f", "0"};

    public final static HashMap<Character, String> xmlChars = new HashMap<Character, String>();
    public final static HashMap<Character, Character> quotMatches = new HashMap<Character, Character>();
    static {
        xmlChars.put('<', "&lt;");
        xmlChars.put('>', "&gt;");
        xmlChars.put('&', "&amp;");
        xmlChars.put('"', "&quot;");
        quotMatches.put('"', '"');
        xmlChars.put((char) 0x2018, "&lsquo;");
        xmlChars.put((char) 0x2019, "&rsquo;");
        quotMatches.put((char) 0x2018, (char) 0x2019);
        quotMatches.put((char) 0x2019, (char) 0x2018);
        xmlChars.put((char) 0x201A, "&sbquo;");
        xmlChars.put((char) 0x2018, "&lsquo;");
        quotMatches.put((char) 0x201A, (char) 0x2018);
        quotMatches.put((char) 0x2018, (char) 0x201A);
        xmlChars.put((char) 0x201C, "&ldquo;");
        xmlChars.put((char) 0x201D, "&rdquo;");
        quotMatches.put((char) 0x201C, (char) 0x201D);
        quotMatches.put((char) 0x201D, (char) 0x201C);
        xmlChars.put((char) 0x201E, "&bdquo;");
        xmlChars.put((char) 0x201C, "&ldquo;");
        quotMatches.put((char) 0x201E, (char) 0x201C);
        quotMatches.put((char) 0x201C, (char) 0x201E);
        xmlChars.put((char) 0x201E, "&bdquo;");
        xmlChars.put((char) 0x201D, "&rdquo;");
        quotMatches.put((char) 0x201D, (char) 0x201E);
        quotMatches.put((char) 0x201E, (char) 0x201D);
        xmlChars.put((char) 0x2039, "&lsaquo;");
        xmlChars.put((char) 0x203A, "&rsaquo;");
        quotMatches.put((char) 0x2039, (char) 0x203A);
        quotMatches.put((char) 0x203A, (char) 0x2039);
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
        if (s==null)
            return "";
        s = s.replaceAll("&", xmlChars.get('&'));
        for (Map.Entry<Character, String> e : xmlChars.entrySet())
            if (! e.getKey().equals('&'))
                s = s.replaceAll(String.valueOf(e.getKey()), e.getValue());
        return s;
    }

    public static String htmlChars2XML(String s) {
        for (Map.Entry<Character, String> e : xmlChars.entrySet())
            s = s.replaceAll(e.getValue(), String.valueOf(e.getKey()));
        return s;
    }

    private static HashMap<String, ArrayList<CSVEntry>> initializeEntries(String[] headers) {
        HashMap<String, ArrayList<CSVEntry>> entries = new HashMap<String, ArrayList<CSVEntry>>();
        for (int i = 0 ; i < headers.length ; i++)
            entries.put(headers[i], new ArrayList<CSVEntry>());
        return entries;
    }

    private static boolean isA(char possibleQuot) {
        return quotMatches.containsKey(possibleQuot);
    }

    /** instance methods */
    private String stripHeaderQuots(String text) throws SurveyException {
        String txt = text;
        int qs = 0;
        while (txt.length()>0 && isA(txt.charAt(0))){
            boolean matchFound = false;
                if (txt.endsWith(String.valueOf(quotMatches.get(txt.charAt(0))))) {
                    txt = txt.substring(1, txt.length() - 1);
                    qs++; matchFound = true; break;
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
        LOGGER.info(Arrays.toString(headers));
        boolean hasQuestion = false;
        boolean hasOption = false;
        for (int i = 0; i < headers.length ; i++) {
            headers[i] = stripHeaderQuots(headers[i]).trim().toUpperCase();
            if (headers[i].equals(Survey.QUESTION))
                hasQuestion = true;
            if (headers[i].equals(Survey.OPTIONS))
                hasOption = true;
            if (headers[i].equals(""))
                headers[i] = gensym.next();
            else {
                //   strip quotes
                //headers[i] = stripQuots(headers[i], true);
                // make sure it doesn't contain quotes
                for (int j = 0; j < headers[i].length() ; j++) {
                    if (isA(headers[i].charAt(j)))
                        throw new HeaderException("Headers cannot contain quotation marks : "+headers[i], this, this.getClass().getEnclosingMethod());
                }
            }
        }
        if (!hasQuestion || !hasOption)
            throw new HeaderException(String.format("Missing header %s", hasQuestion?Survey.OPTIONS:Survey.QUESTION), this, null);
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

            if (header.equals(Survey.BLOCK))
                cellProcessors[i] = new Optional(new StrRegEx("_?[1-9][0-9]*(\\._?[1-9][0-9]*)*"));

            if (header.equals(Survey.BRANCH))
                cellProcessors[i] = new Optional(new StrRegEx("(NULL)|(null)|(_?[1-9][0-9]*)"));


            else if (header.equals(Survey.EXCLUSIVE)
                    || headers[i].equals(Survey.ORDERED)
                    || headers[i].equals(Survey.RANDOMIZE))
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

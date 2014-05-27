package input.csv;

import input.AbstractLexer;
import input.exceptions.HeaderException;
import input.exceptions.SyntaxException;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.IsIncludedIn;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import survey.Survey;
import survey.exceptions.SurveyException;
import util.Gensym;
import util.Slurpie;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class CSVLexer extends AbstractLexer {


    /** instance fields */
    private String fieldQuot = "\"";
    public String encoding;
    public String sep;
    public String filename;
    public String[] headers;
    public HashMap<String, ArrayList<CSVEntry>> entries;

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

    private static HashMap<String, ArrayList<CSVEntry>> initializeEntries(String[] headers) {
        HashMap<String, ArrayList<CSVEntry>> entries = new HashMap<String, ArrayList<CSVEntry>>();
        for (int i = 0 ; i < headers.length ; i++)
            entries.put(headers[i], new ArrayList<CSVEntry>());
        return entries;
    }


    private String[] mapStringOp(String[] input, Method m) throws InvocationTargetException, IllegalAccessException {
        String[] retval = new String[input.length];
        for (int i = 0 ; i < retval.length ; i++)
            retval[i] = (String) m.invoke(input[i]);
        return retval;
    }

    private String[] getHeaders() throws SurveyException, IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Gensym gensym = new Gensym("GENCOLHEAD");
        boolean hasQuestion = false, hasOption = false;

        CsvPreference pref;
        if (this.sep.equals(","))
            pref = CsvPreference.EXCEL_PREFERENCE;
        else if (this.sep.equals("\t") || this.sep.equals("\\t"))
            pref = CsvPreference.TAB_PREFERENCE;
        else throw new SyntaxException("Unknown delimiter: " + this.sep);

        System.out.println(this.filename);
        String stuff = Slurpie.slurp(this.filename);

        CsvListReader reader = new CsvListReader(new StringReader(stuff), pref);
        List<String> line = reader.read();
        String[] headers = mapStringOp(line.toArray(new String[line.size()]),  String.class.getMethod("toUpperCase"));
        headers = mapStringOp(headers, String.class.getMethod("trim"));
        for (int i = 0 ; i < headers.length ; i++) {
            if (headers[i].equals(Survey.QUESTION))
                hasQuestion = true;
            if (headers[i].equals(Survey.OPTIONS))
                hasOption = true;
            if (headers[i].equals(""))
                headers[i] = gensym.next();
        }
        if (!hasQuestion || !hasOption)
            throw new HeaderException(String.format("Missing header %s for survey %s with separator %s"
                    , hasQuestion?Survey.OPTIONS:Survey.QUESTION, this.filename, sep));
        return headers;
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
                cellProcessors[i] = new Optional(new StrRegEx("(NEXT)|(next)|((_|[a-z])?[1-9][0-9]*)"));


            else if (header.equals(Survey.EXCLUSIVE)
                    || headers[i].equals(Survey.ORDERED)
                    || headers[i].equals(Survey.RANDOMIZE))
                cellProcessors[i] = new Optional(new IsIncludedIn(truthValues));

            else
                cellProcessors[i] = new Optional();
        }

        return cellProcessors;
    }

    public HashMap<String, ArrayList<CSVEntry>> lex(String filename)
            throws IOException, RuntimeException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        char fieldSep;

        if (sep.equals(","))
            fieldSep = '\u002c';
        else if (sep.equals("\t") || sep.equals("\\t"))
            fieldSep = '\u0009';
        else throw new SyntaxException("Unknown separator: " + sep);

        final CsvPreference pref = new CsvPreference.Builder(fieldQuot.toCharArray()[0], fieldSep, "\n").build();
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

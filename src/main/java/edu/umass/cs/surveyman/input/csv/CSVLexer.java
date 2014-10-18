package edu.umass.cs.surveyman.input.csv;

import edu.umass.cs.surveyman.input.AbstractLexer;
import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.input.exceptions.HeaderException;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.IsIncludedIn;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;
import edu.umass.cs.surveyman.utils.Slurpie;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Class to lex SurveyMan CSV input.
 */
public class CSVLexer extends AbstractLexer {


    /** instance fields */
    /**
     * String used to quote fields in the input file. Default is U+0022 ("\"").
     */
    private String fieldQuot = "\"";
    /**
     * The file's encoding. Default is UTF-8.
     */
    public String encoding;
    /**
     * The file's field separator. The default is U+002C (",").
     */
    public String sep;
    /**
     * The input file name.
     */
    public String filename;
    /**
     * The list of headers actually encountered in the file.
     */
    public String[] headers;
    /**
     * The map from header label to csv entries.
     */
    public HashMap<String, ArrayList<CSVEntry>> entries;

    public CSVLexer(String filename, String sep, String encoding)
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this.sep = sep;
        this.filename = filename;
        this.encoding = encoding;
        this.headers = getHeaders();
        this.entries = lex(new FileReader(filename));
    }

    public CSVLexer(String filename, String sep)
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this(filename, sep, "UTF-8");
    }

    public CSVLexer(String filename)
            throws IOException, SurveyException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        this(filename, ",");
    }

    public CSVLexer(Reader reader, String sep, String encoding) throws InvocationTargetException, SurveyException,
            IllegalAccessException, NoSuchMethodException, IOException {
        this.sep = sep;
        this.encoding = encoding;
        this.headers = getHeaders(reader);
        this.entries = lex(reader);
    }

    public CSVLexer(Reader reader, String sep) throws SurveyException, NoSuchMethodException, IOException,
            IllegalAccessException, InvocationTargetException {
        this(reader, sep, "UTF-8");
    }

    public CSVLexer(Reader reader) throws SurveyException, NoSuchMethodException, IOException, IllegalAccessException,
            InvocationTargetException {
        this(reader, ",", "UTF-8");
    }

    private static HashMap<String, ArrayList<CSVEntry>> initializeEntries(String[] headers) {
        HashMap<String, ArrayList<CSVEntry>> entries = new HashMap<String, ArrayList<CSVEntry>>();
        for (String header : headers) entries.put(header, new ArrayList<CSVEntry>());
        return entries;
    }


    private String[] mapStringOp(String[] input, Method m) throws InvocationTargetException, IllegalAccessException {
        String[] retval = new String[input.length];
        for (int i = 0 ; i < retval.length ; i++)
            retval[i] = (String) m.invoke(input[i]);
        return retval;
    }

    private String[] getHeaders() throws IOException, InvocationTargetException, SurveyException, IllegalAccessException,
            NoSuchMethodException {
        return getHeaders(new FileReader(this.filename));
    }

    private String[] getHeaders(Reader reader) throws SurveyException, IOException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {

        Gensym gensym = new Gensym("GENCOLHEAD");
        boolean hasQuestion = false, hasOption = false;

        CsvPreference pref;
        if (this.sep.equals(","))
            pref = CsvPreference.EXCEL_PREFERENCE;
        else if (this.sep.equals("\t") || this.sep.equals("\\t") || this.sep.equals("t")) //hack that handles any weird string issues
            pref = CsvPreference.TAB_PREFERENCE;
        else throw new SyntaxException("Unknown delimiter: " + this.sep);


        CsvListReader csvListReader = new CsvListReader(reader, pref);
        List<String> line = csvListReader.read();
        String[] headers = mapStringOp(line.toArray(new String[line.size()]),  String.class.getMethod("toUpperCase"));
        headers = mapStringOp(headers, String.class.getMethod("trim"));
        for (int i = 0 ; i < headers.length ; i++) {
            if (headers[i].equals(AbstractParser.QUESTION))
                hasQuestion = true;
            if (headers[i].equals(AbstractParser.OPTIONS))
                hasOption = true;
            if (headers[i].equals(""))
                headers[i] = gensym.next();
        }
        if (!hasQuestion || !hasOption)
            throw new HeaderException(String.format("Missing header %s for edu.umass.cs.surveyman.survey %s with separator %s"
                    , hasQuestion? AbstractParser.OPTIONS: AbstractParser.QUESTION, this.filename, sep));
        return headers;
    }


    private CellProcessor[] makeProcessors() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
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

            if (header.equals(AbstractParser.BLOCK))
                cellProcessors[i] = new Optional(new StrRegEx("(_|[a-z])?[1-9][0-9]*(\\.(_|[a-z])?[1-9][0-9]*)*"));

            else if (header.equals(AbstractParser.BRANCH))
                cellProcessors[i] = new Optional(new StrRegEx("(NEXT)|(next)|([1-9][0-9]*)"));

//            else if (header.equals(AbstractParser.CONDITION))
//                cellProcessors[i] = new Optional(new StrRegEx("([0-9]+)|(0?\\.[0-9]+)|([0-9][0-9]?[0-9]?\\%)"));


            else if (header.equals(AbstractParser.EXCLUSIVE)
                    || headers[i].equals(AbstractParser.ORDERED)
                    || headers[i].equals(AbstractParser.RANDOMIZE))
                cellProcessors[i] = new Optional(new IsIncludedIn(truthValues));

            else
                cellProcessors[i] = new Optional();
        }

        return cellProcessors;
    }

    public HashMap<String, ArrayList<CSVEntry>> lex(Reader reader)
            throws IOException, RuntimeException, SurveyException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {

        char fieldSep;

        if (sep.equals(","))
            fieldSep = '\u002c';
        else if (sep.equals("\t") || sep.equals("\\t"))
            fieldSep = '\u0009';
        else throw new SyntaxException("Unknown separator: " + sep);

        final CsvPreference pref = new CsvPreference.Builder(fieldQuot.toCharArray()[0], fieldSep, "\n").build();
        ICsvListReader csvReader = new CsvListReader(reader, pref);
        csvReader.getHeader(true); // skips the header column
        final CellProcessor[] processors = makeProcessors();

        HashMap<String, ArrayList<CSVEntry>> entries = initializeEntries(this.headers);

        List<Object> line;
        while ((line = csvReader.read(processors))!=null) {
            // for each header, read an entry.
            int lineNo = csvReader.getLineNumber();
            for (int colNo = 0 ; colNo < line.size() ; colNo++) {
                CSVEntry csvEntry = new CSVEntry((String) line.get(colNo), lineNo, colNo+1);
                ArrayList<CSVEntry> csvEntries = entries.get(headers[colNo]);
                csvEntries.add(csvEntries.size(), csvEntry);
                entries.put(headers[colNo], csvEntries);
            }
        }
        assert entries.get(AbstractParser.QUESTION).size() > 0 : "A survey must have at least one question";
        return entries;
    }

}

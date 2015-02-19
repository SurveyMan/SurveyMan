package edu.umass.cs.surveyman.input;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements general lexing utility methods.
 */
public abstract class AbstractLexer {

    protected final static Logger LOGGER = LogManager.getLogger();
    /**
     * Entries that convert to a boolean true.
     */
    public final static String[] trueValues = {"yes", "y", "true", "t", "1"};
    /**
     * Entries that convert to a boolean false.
     */
    public final static String[] falseValues = {"no", "n", "false", "f", "0"};

    /**
     * XML/HTML conversion characters.
     */
    public static HashMap<Character, String> xmlChars;

    /**
     * Recognized quotation marks.
     */
    public static HashMap<Character, Character> quotMatches;

    /**
     * Initializes xmlChars and quotMatches.
     */
    public static void init(){
        xmlChars = new HashMap<Character, String>();
        quotMatches  = new HashMap<Character, Character>();
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

    /**
     * Utility class for quotation marks.
     *
     * @param possibleQuot Character that might be a quotation mark.
     * @return Boolean indicating whether the input character is a quotation mark.
     */
    public static boolean isA(char possibleQuot) {

        if (quotMatches==null)
            init();

        return quotMatches.containsKey(possibleQuot);
    }

    /**
     * Converts matched xml characters in s to html characters.
     * @param s Input string containing XML characters.
     * @return The converted string.
     */
    public static String xmlChars2HTML(String s) {

        if (xmlChars==null)
            init();

        if (s==null)
            return "";
        // this is a hack and will probably break later
        //if (s.startsWith("<"))
        //    return s;
        s = s.replaceAll("&", xmlChars.get('&'));
        for (Map.Entry<Character, String> e : xmlChars.entrySet())
            if (! e.getKey().equals('&'))
                s = s.replaceAll(String.valueOf(e.getKey()), e.getValue());
        return s;
    }

    /**
     * Converts matched html strings to xml characters.
     * @param s Input string containing HTML characters.
     * @return The converted string.
     */
    public static String htmlChars2XML(String s) {

        if (xmlChars==null)
            init();

        for (Map.Entry<Character, String> e : xmlChars.entrySet())
            s = s.replaceAll(e.getValue(), String.valueOf(e.getKey()));
        return s;
    }

    /**
     * Abstract lexing method.
     *
     * @param reader The source reader, containing the data to be lexed.
     * @return A Map from column names to entries.
     * @throws Exception
     */
    public abstract Map lex(Reader reader) throws Exception;
}

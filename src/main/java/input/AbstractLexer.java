package input;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractLexer {
    protected final static Logger LOGGER = Logger.getLogger(AbstractLexer.class);
    public final static String[] trueValues = {"yes", "y", "true", "t", "1"};
    public final static String[] falseValues = {"no", "n", "false", "f", "0"};

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

    public static String xmlChars2HTML(String s) {
        if (s==null)
            return "";
        // this is a hack and will probably break later
        if (s.startsWith("<"))
            return s;
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

    public abstract Map lex(String filename) throws Exception;
}

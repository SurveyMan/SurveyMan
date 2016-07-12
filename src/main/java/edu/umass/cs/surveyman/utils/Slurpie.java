package edu.umass.cs.surveyman.utils;

import java.io.*;
import java.net.URL;
import java.lang.String;

/**
 * Slurpie.slurp reads an entire file into a string.
 */
public class Slurpie {
    // convenience class to slurp in a whole file

    /**
     * Read the contents of a file.
     * @param filename file or URL.
     * @return contents as a String.
     * @throws IOException
     */
    public static String slurp(String filename) throws IOException {
        return slurp(filename, Integer.MAX_VALUE);
    }

    /**
     * Get a reader for a file or URL.
     * @param filename file or URL.
     * @return a BufferedReader, make sure to close it.
     * @throws IOException
     */
    public static BufferedReader getReader(String filename) throws IOException {
        URL resource = Slurpie.class.getClassLoader().getResource(filename);
        BufferedReader br = null;
        if (resource == null) {
            try {
                br = new BufferedReader(new FileReader(filename));
            } catch (FileNotFoundException fe) {
                br = new BufferedReader(new InputStreamReader(new URL(filename).openStream()));
            }
        } else br = new BufferedReader(new InputStreamReader(resource.openStream()));
        return br;
    }

    /**
     * Slurp in a filename up to a maximimum number of characters.
     * @param filename file name or url.
     * @param numChars maximum size in characters.
     * @return a string of up to numChars.
     * @throws IOException
     */
    public static String slurp(String filename, int numChars) throws IOException {
        try (BufferedReader br = getReader(filename)) {
            return slurp(br, numChars);
        }
    }

    /**
     * Slurp from a BufferedReader up to roughly numChars characters of data.
     * @param br the buffered reader.
     * @param numChars the number of characters to read.
     * @return the data as a string.
     * @throws IOException
     */
    public static String slurp(BufferedReader br, int numChars) throws IOException {
        StringBuilder s = new StringBuilder();
        char[] buf = new char[1024 * 1024];
        for (int totalCharsRead = 0; totalCharsRead < numChars; ) {
            int charsRead = br.read(buf);
            if (charsRead == -1)
                break;
            s.append(buf, 0, charsRead);
            totalCharsRead += buf.length;
        }
        return s.toString();
    }

    public static String slurp(String filename, boolean ignoreErr) throws IOException{
        String retval = "";
        try {
            retval = slurp(filename);
        } catch (IOException io) {
            if (!ignoreErr) throw io;
        }
        return retval;
    }
}

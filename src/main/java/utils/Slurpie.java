package utils;

import system.mturk.MturkLibrary;

import java.io.*;
import java.io.PrintStream;

/**
 * Slurpie.slurp reads an entire file into a string.
 */
public class Slurpie {
    // convenience class to slurp in a whole file

    public static String slurp(String filename) throws IOException {
        return slurp(filename, Integer.MAX_VALUE);
    }

    public static String slurp(String filename, int numChars) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder s = new StringBuilder();
        char[] buf = new char[1024*1024];
        for (int totalCharsRead = 0 ; totalCharsRead < numChars ; ) {
            int charsRead = br.read(buf);
            if (charsRead == -1)
                break;
            s.append(buf, 0, charsRead);
            totalCharsRead += charsRead;
        }
        return s.toString();
    }
}

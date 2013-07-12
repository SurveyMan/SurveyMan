package utils;

import system.mturk.MturkLibrary;

import java.io.*;
import java.io.PrintStream;

/**
 * Slurpie.slurp reads an entire file into a string.
 */
public class Slurpie {
    // convenience class to slurp in a whole file

    public static String slurp(String filename) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder s = new StringBuilder();
        char[] buf = new char[1024*1024];
        while(true) {
            int charsRead = br.read(buf);
            if (charsRead == -1)
                break;
            s.append(buf, 0, charsRead);
        }
        return s.toString();
    }
    
    public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException, IOException {
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        out.println(slurp(MturkLibrary.XMLSKELETON));
        out.println(slurp(MturkLibrary.HTMLSKELETON));
    }
    
}

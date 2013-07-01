package utils;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class Out {
    public static PrintStream out;
    public Out(String encoding) {
        try {
            out = new PrintStream(System.out, true, encoding);
        } catch (UnsupportedEncodingException e) {
            System.out.println("This system does not support "+encoding);
            System.exit(-1);
        }
    }
}
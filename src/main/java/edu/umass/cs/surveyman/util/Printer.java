package edu.umass.cs.surveyman.util;

public class Printer {

    private static boolean VERBOSE = true;

    public static void updateVerbosity(boolean b){
        VERBOSE = b;
    }

    public static void println(String s){
        if (VERBOSE)
            System.out.println(s);
    }

}

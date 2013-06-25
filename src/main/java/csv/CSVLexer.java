package csv;

import java.io.BufferedReader;
import java.lang.String;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import csv.CSVEntry;

public class CSVLexer {

    public static final String[] knownHeaders =
            {"QUESTION", "BLOCK", "OPTIONS", "RESOURCE", "EXCLUSIVE", "ORDERED", "PERTURB", "BRANCH"};

    public static ArrayList<List<CSVEntry>> lex(String filename) {
        return new ArrayList<List<CSVEntry>>();
    }

    public static void main(String[] args) {
        //write test code here
        System.out.println("Hello, World!");
    }
}
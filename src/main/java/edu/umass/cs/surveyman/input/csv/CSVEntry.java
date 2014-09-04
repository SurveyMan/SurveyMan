package edu.umass.cs.surveyman.input.csv;

import java.util.ArrayList;

/**
 * The base class for CSV entries. This corresponds to a cell in a spreadsheet.
 * @author etosch
 */
public class CSVEntry {

    /**
     * The contents of a cell
     */
    public String contents;
    /**
     * The cell's column number
     */
    public int colNo;
    /**
     * The cells's row number.
     */
    public int lineNo;

    /**
     * Creates a new CSVEntry and sets the source and column numbers to -1. This should be used when creating "dummy"
     * data that is not derived from the input, such as timing information.
     * @param contents The string contents of the cell, which may not have been in the source.
     */
    public CSVEntry(String contents) {
        this.contents = contents;
        this.colNo=-1;
        this.lineNo=-1;
    }

    /**
     * Main constructor for a CSVEntry, used by {@link edu.umass.cs.surveyman.input.csv.CSVLexer}.
     * @param contents The string contents of the cell in the source.
     * @param lineNo The source row number.
     * @param colNo The source column number.
     */
    public CSVEntry(String contents, int lineNo, int colNo) {
        this.contents=contents;
        this.colNo=colNo;
        this.lineNo=lineNo;

    }

    /**
     * Sorts a list of CSVEntries according first to their line numbers, then their column numbers.
     * @param entries
     */
    public static void sort(ArrayList<CSVEntry> entries) {
        // since we're probably mostly sorted, just do bubble sort or whatever
        for (int i = 1; i < entries.size() ; i ++) {
            CSVEntry a = entries.get(i-1);
            CSVEntry b = entries.get(i);
            if (a.lineNo > b.lineNo) {
                entries.set(i-1, b);
                entries.set(i, a);
                if (i>1) i-=2; 
            }
        }
    }

    /**
     * Returns a string corresponding to the data and the source cell.
     * @return String representation of this cell.
     */
    @Override
    public String toString() {
        return String.format("[(%d, %d) %s]", lineNo, colNo, contents);
    }

}
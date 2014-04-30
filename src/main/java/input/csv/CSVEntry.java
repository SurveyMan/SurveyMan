package input.csv;

import java.util.ArrayList;

/**
 * The base class for CSV entries.
 * @author etosch
 */
public class CSVEntry {

    public String contents;
    public int colNo;
    public int lineNo;

    public CSVEntry(String contents) {
        this.contents = contents;
        this.colNo=-1;
        this.lineNo=-1;
    }

    public CSVEntry(String contents, int lineNo, int colNo) {
        this.contents=contents;
        this.colNo=colNo;
        this.lineNo=lineNo;

    }
    
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

    @Override
    public String toString() {
        return String.format("[(%d, %d) %s]", lineNo, colNo, contents);
    }

}
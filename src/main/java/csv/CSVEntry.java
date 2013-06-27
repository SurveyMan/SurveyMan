package csv;

import java.lang.String;

public class CSVEntry {

    public String contents;
    public int colNo;
    public int lineNo;

    CSVEntry(String contents) {
        this.contents = contents;
        this.colNo=-1;
        this.lineNo=-1;
    }

    CSVEntry(String contents, int lineNo, int colNo) {
        this.contents=contents;
        this.colNo=colNo;
        this.lineNo=lineNo;

    }

    public String toString() {
        return String.format("(%d, %d) %s", lineNo, colNo, contents);
    }

    public static void main(String[] args) {
        // write test code here
    }
}
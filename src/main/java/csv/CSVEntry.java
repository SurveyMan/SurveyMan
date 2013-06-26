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

    CSVEntry(String contents, int colNo, int lineNo) {
        this.contents=contents;
        this.colNo=colNo;
        this.lineNo=lineNo;

    }

    public static void main(String[] args) {
        // write test code here
    }
}
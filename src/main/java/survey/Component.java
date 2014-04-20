package survey;

public abstract class Component {

    // A component is one of the elements of a survey.
    // Components get unique ids
    // Components may be text or a url
    // Their layout may be controlled by an input css file

    private final String cid;
    private final int row;
    private final int col;
    public int index;


    public static String makeComponentId(int row, int col) {
        return String.format("comp_%d_%d", row, col);
    }


    public Component(int row, int col){
        this.cid = makeComponentId(row, col);
        this.row = row;
        this.col = col;
    }

    public String getCid(){
        return this.cid;
    }

    public int getSourceRow() {
        return row;
    }

    public int getSourceCol() {
        return col;
    }

    public abstract boolean equals(Object c);

    public int hashCode() {
        return cid.hashCode();
    }

    @Override
    public String toString() {
        return "cid:" + cid + " index:" + index;
    }
}





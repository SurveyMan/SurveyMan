package survey;

public class HTMLComponent extends Component {

    public final String data;

    public HTMLComponent(String html, int row, int col) {
        super(row, col);
        this.data = html;
    }

    public boolean isEmpty(){
        return this.data==null || this.getCid()==null;
    }

    public static boolean isHTMLComponent(String data){
        //HACK
        return data.startsWith("<") && data.endsWith(">");
    }

    @Override
    public boolean equals(Object c) {
        if (c instanceof HTMLComponent)
            return this.data.equals(((HTMLComponent) c).data)
                    && this.getCid().equals(((HTMLComponent) c).getCid());
        else return false; 
    }

    @Override
    public String toString() {
        return data.toString();
    }

    public String toString(boolean dataOnly) {
        if (dataOnly)
            return this.data.toString();
        else return this.toString();
    }
}
package survey;

import java.net.MalformedURLException;
import java.net.URL;

// TODO: change URL to URI for less internet during .equals();
public class HTMLComponent extends Component {

    public final URL data;

    public HTMLComponent(String url, int row, int col) throws MalformedURLException {
        super(row, col);
        this.data = new URL(url);
    }

    public boolean isEmpty(){
        return this.data==null || this.getCid()==null;
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
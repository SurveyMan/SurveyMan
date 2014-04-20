package survey;

import java.net.MalformedURLException;
import java.net.URL;

// TODO: change URL to URI for less internet during .equals();
public class URLComponent extends Component {

    public final URL data;

    public URLComponent(String url, int row, int col) throws MalformedURLException {
        super(row, col);
        this.data = new URL(url);
    }
    
    @Override
    public boolean equals(Object c) {
        if (c instanceof URLComponent)
            return this.data.equals(((URLComponent) c).data)
                    && this.getCid().equals(((URLComponent) c).getCid());
        else return false; 
    }

    @Override
    public String toString() {
        return super.toString() + " data:" + data.toString();
    }

    public String toString(boolean dataOnly) {
        if (dataOnly)
            return this.data.toString();
        else return this.toString();
    }
}
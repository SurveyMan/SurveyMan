package survey;

import java.net.MalformedURLException;
import java.net.URL;

public class URLComponent extends Component {

    public final URL data;

    public URLComponent(String url) throws MalformedURLException {
        this.data = new URL(url);
    }
    
    @Override
    public boolean equals(Component c) {
        if (c instanceof URLComponent)
            return this.data.equals(((URLComponent) c).data);
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
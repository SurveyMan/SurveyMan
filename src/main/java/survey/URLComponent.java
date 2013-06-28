package survey;

import java.net.MalformedURLException;
import java.net.URL;
import survey.Component;

public class URLComponent extends Component {

    public final URL data;

    public URLComponent(String url) throws MalformedURLException {
        this.data = new URL(url);
    }

    public String toString() {
        return data.toString();
    }
}
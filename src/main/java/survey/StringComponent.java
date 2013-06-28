package survey;

import survey.Component;

public class StringComponent extends Component {

    public final String data;

    public StringComponent(String data) {
        this.data = data;
    }

    public String toString() {
        return data;
    }
}
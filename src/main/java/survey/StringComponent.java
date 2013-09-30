package survey;

public class StringComponent extends Component {

    public final String data;

    public StringComponent(String data) {
        this.data = data;
    }
    
    @Override
    public boolean equals(Component c) {
        if (c instanceof StringComponent)
            return this.data.equals(((StringComponent) c).data);
        else return false;
    }

    @Override
    public String toString() {
        return super.toString() + " data:" + data;
    }

    public String toString(boolean dataOnly) {
        if (dataOnly)
            return data;
        else return this.toString();
    }
}
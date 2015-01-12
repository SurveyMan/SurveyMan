package edu.umass.cs.surveyman.survey;

/**
 * Component subtype representing String data.
 */
public class StringComponent extends Component {

    /**
     * The string representing this component.
     */
    public final String data;

    /**
     * Creates a string component with row and column data.
     *
     * @param data The string representing this component.
     * @param row The input row.
     * @param col The input column.
     */
    public StringComponent(String data, int row, int col) {
        super(row, col);
        this.data = data;
    }

    /**
     * A StringComponent is empty if either its data is empty or it has no component id.
     * @return
     */
    public boolean isEmpty(){
        return this.data==null || this.getCid()==null;
    }

    @Override
    protected String jsonize() {
        if(data.isEmpty()) {
            throw new RuntimeException("AGAA");
        }
        return String.format("{ \"id\" : \"%s\", \"otext\" : \"%s\" }"
                , this.getCid()
                , data);
    }

    /**
     * Two StringComponents are equal if they have equal component ids.
     * @param c Another StringComponent.
     * @return boolean indicating whether this and the input object have equivalent component ids.
     */

    @Override
    public boolean equals(Object c) {
        if (c instanceof StringComponent)
            return this.data.equals(((StringComponent) c).data)
                    && this.getCid().equals(((StringComponent) c).getCid());
        else return false;
    }

    @Override
    public boolean dataEquals(String data) {
        return this.data.equals(data);
    }

    @Override
    public String toString() {
        return data;
    }
}
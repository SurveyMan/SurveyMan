package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.input.AbstractLexer;

/**
 * SurveyDatum subtype representing String data.
 */
public class StringDatum extends SurveyDatum {

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
    public StringDatum(String data, int row, int col) {
        super(row, col);
        this.data = data;
    }

    public StringDatum(String data) {
        super(SurveyDatum.SYSTEM_DEFINED, SurveyDatum.DEFAULT_SOURCE_COL);
        this.data = data;
    }

    /**
     * A StringDatum is empty if either its data is empty or it has no component id.
     * @return
     */
    public boolean isEmpty()
    {
        return this.data==null || this.getId()==null;
    }

    @Override
    protected String jsonize() {
        if(data.isEmpty()) {
            throw new RuntimeException(String.format("Data field in component %s located at (%d, %d) is empty.",
                    this.data, this.getSourceRow(), this.getSourceCol()));
        }
        return String.format("{ \"id\" : \"%s\", \"otext\" : \"%s\" }"
                , this.getId()
                , AbstractLexer.htmlChars2XML(data));
    }

    /**
     * Two StringComponents are equal if they have equal component ids.
     * @param c Another StringDatum.
     * @return boolean indicating whether this and the input object have equivalent component ids.
     */
    @Override
    public boolean equals(Object c) {
        return c instanceof StringDatum && this.data.equals(((StringDatum) c).data) && this.getId().equals(((StringDatum) c).getId());
    }

    @Override
    public boolean dataEquals(
            String data)
    {
        return this.data.equals(data);
    }

    @Override
    public String toString()
    {
        return data;
    }

}
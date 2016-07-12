package edu.umass.cs.surveyman.survey;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Whitelist;

/**
 * SurveyDatum subtype representing arbitrary HTML. Questions used to be a mix of HTMLDatums and
 * {@link StringDatum}s when the RESOURCE column was in use. Now a question is one of
 * an HTMLDatum or {@link StringDatum}.
 */
public class HTMLDatum extends SurveyDatum {

    /**
     * The normalized HTML String associated with this SurveyDatum.
     */
    public final String data;

    /**
     * Takes in a string of purported HTML, parses and normalizes it, and returns an HTMLDatum. The row and column
     * data is used for debugging and sorting.
     *
     * @param html Arbitrary HTML fragment.
     * @param row Source row.
     * @param col Source column.
     */
    public HTMLDatum(String html, int row, int col, int index) {
        super(row, col, index);
        assert !html.isEmpty() : "Should not be calling the HTMLDatum constructor on an empty data string.";
        Document doc = Jsoup.parseBodyFragment(html).normalise();
        this.data = doc.body().html();
    }

    public HTMLDatum(String html){
        this(html, SurveyDatum.SYSTEM_DEFINED, SurveyDatum.DEFAULT_SOURCE_COL, -1);
    }

    public boolean isEmpty(){
        return this.data==null || this.getId()==null;
    }

    @Override
    public String jsonize() {
        if(data.isEmpty()) {
            throw new RuntimeException(String.format("Data field in component %s located at (%d, %d) is empty.",
                    this.data, this.getSourceRow(), this.getSourceCol()));
        }
        return String.format("{ \"id\" : \"%s\", \"otext\" : \"%s\" }"
                , this.getId()
                , data.replace("\"", "\\\""));
    }

    /**
     * Tests whether the input data is valid HTML using Jsoup.
     * @param data Potentially HTML data.
     * @return boolean indicating whether the input is valid HTML5.
     */
    public static boolean isHTMLComponent(String data){
        return !data.isEmpty() && Jsoup.isValid(data, Whitelist.basicWithImages());
    }

    /**
     * Two HTMLDatums are equal if they have equal ids.
     * @param c Another HTMLDatum.
     * @return boolean indicating whether this and the input object have equivalent component ids.
     */
    @Override
    public boolean equals(Object c) {
        if (c instanceof HTMLDatum)
            return this.data.equals(((HTMLDatum) c).data)
                    && this.getId().equals(((HTMLDatum) c).getId());
        else return false; 
    }

    @Override
    public boolean dataEquals(String data) {
        return isHTMLComponent(data) && this.data.equals(data);
    }

    @Override
    public String toString() {
        return data;
    }

    /**
     * Returns the data contents of the HTMLDatum.
     * @param dataOnly boolean indicating whether to return data contents only.
     * @return String representation of the HTMLDatum.
     */
    public String toString(boolean dataOnly) {
        if (dataOnly)
            return this.data;
        else return this.toString();
    }

    public SurveyDatum copy() {
        return new HTMLDatum(this.data);
    }
}
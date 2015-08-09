package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.input.csv.CSVLexer;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * The abstract base class for things that are laid out on a page. This class encapsulates the main data of the survey.
 * It is used to represent question and option data that is displayed to the respondent. Their layout may be controlled
 * by a custom css file.
 */
public abstract class SurveyDatum implements Comparable, Serializable {

    protected static int DEFAULT_SOURCE_COL = 0;
    protected static int TOTAL_COMPONENTS = 0;
    protected static int SYSTEM_DEFINED = Integer.MIN_VALUE;
    protected static String CUSTOM_DATUM = "data_-1_-1";

    /**
     * Internal unique identifier.
     */
    private String id;
    /**
     * Source line number.
     */
    private int row;
    /**
     * Source column number.
     */
    private int col;
    /**
     * Relative index of this datum in its containing set.
     */
    private int index;

    /**
     * Creates a SurveyMan SurveyDatum internal identifier for the source location.
     * @param row The source line number.
     * @param col The source column (or character index in the row)
     * @return A String of the internal datum identifier.
     */
    public static String makeSurveyDatumId(int row, int col) {
        return String.format("data_%d_%d", row, col);
    }

    /**
     * Creates a new survey datum.
     * @param row The source line number.
     * @param col The source column (or character index in the row).
     */
    public SurveyDatum(int row, int col, int index) {
        SurveyDatum.TOTAL_COMPONENTS++;
        this.id = makeSurveyDatumId(row, col);
        this.row = row == SYSTEM_DEFINED ? SurveyDatum.TOTAL_COMPONENTS : row;
        this.col = col;
        this.index = index;
    }

    /**
     * Returns the internal data identifier.
     * @return A String of the internal component identifier.
     */
    public String getId(){
        return this.id;
    }

    protected void resetId(int row, int col) {
        this.id = makeSurveyDatumId(row, col);
        this.row = row;
        this.col = col;
    }

    /**
     * Returns the source line number.
     * @return The source line number.
     */
    public int getSourceRow() {
        return row;
    }

    /**
     * Returns the source column number (or character index in the row).
     * @return The source column (or character index in the row).
     */
    public int getSourceCol() {
        return col;
    }

    /**
     * Returns the current index of this datum, relative to its containing object.
     * @return 0-based index.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Sets the current index of this datum, realtive to its containing object.
     * @param index The new index at which this datum can be found.
     */
    public void setIndex(int index) {
        this.index = index;
    }

    public static boolean isCustomDatum(String oid)
    {
        return oid.equals(CUSTOM_DATUM);
    }

    public boolean isCustomDatum()
    {
        return SurveyDatum.isCustomDatum(this.getId());
    }

    @Override
    public abstract boolean equals(Object c);

    /**
     * Tests whether this component is empty.
     * @return boolean
     */
    public abstract boolean isEmpty();

    public abstract boolean dataEquals(String data);

    protected abstract String jsonize();

    protected static String jsonize(List<SurveyDatum> options) {
        Iterator<SurveyDatum> opts = options.iterator();
        if (!opts.hasNext())
            return "";
        StringBuilder s = new StringBuilder(opts.next().jsonize());
        while (opts.hasNext()) {
            SurveyDatum o = opts.next();
            s.append(String.format(", %s", o.jsonize()));
        }
        return String.format("[ %s ]", s.toString());
    }

    public static String html(SurveyDatum c) {
        if (c instanceof StringDatum)
            return CSVLexer.xmlChars2HTML(((StringDatum) c).data).replace("\"", "&quot;");
        else {
            String data = ((HTMLDatum) c).data;
            return data.replace("\"", "\\\"")
                    .replace("\n", "<br/>");
        }
    }

    /**
     * Components are hashed on their identifiers.
     * @return int
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * A string representation comprised of the identifier and the relative index, for use in debugging and logging.
     * @return String representation.
     */
    @Override
    public String toString() {
        return "id:" + id + " index:" + index;
    }

    @Override
    public int compareTo(Object object)
    {
        SurveyDatum that = (SurveyDatum) object;
        if (this.row > that.row)
            return 1;
        if (this.row < that.row)
            return -1;
        return 0;
    }

}





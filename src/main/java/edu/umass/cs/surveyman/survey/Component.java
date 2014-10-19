package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.input.csv.CSVLexer;

import java.util.Iterator;
import java.util.List;

/**
 * The abstract base class for things that are laid out on a page. This class encapsulates the main data of the survey.
 * It is used to represent question and option data that is displayed to the respondent.
 */
public abstract class Component {

    // A component is one of the elements of a edu.umass.cs.surveyman.survey.
    // Components get unique ids
    // Components may be text or a url
    // Their layout may be controlled by an edu.umass.cs.surveyman.input css file

    /**
     * Internal unique identifier.
     */
    private final String cid;
    /**
     * Source line number.
     */
    private final int row;
    /**
     * Source column number.
     */
    private final int col;
    /**
     * Relative index of this component in its containing set.
     */
    public int index;

    /**
     * Creates a SurveyMan component internal identifier for the source location.
     * @param row The source line number.
     * @param col The source column (or character index in the row)
     * @return A String of the internal component identifier.
     */
    public static String makeComponentId(int row, int col) {
        return String.format("comp_%d_%d", row, col);
    }

    /**
     * Creates a new component.
     * @param row The source line number.
     * @param col The source column (or character index in the row).
     */
    public Component(int row, int col){
        this.cid = makeComponentId(row, col);
        this.row = row;
        this.col = col;
    }

    /**
     * Returns the internal component identifier.
     * @return A String of the internal component identifier.
     */
    public String getCid(){
        return this.cid;
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

    @Override
    public abstract boolean equals(Object c);

    /**
     * Tests whether this component is empty.
     * @return boolean
     */
    public abstract boolean isEmpty();

    protected String jsonize() {
        return String.format("{ \"id\" : \"%s\", \"otext\" : \"%s\" }"
                , this.getCid()
                , this.toString());
    }

    protected static String jsonize(List<Component> options) {
        Iterator<Component> opts = options.iterator();
        if (!opts.hasNext())
            return "";
        StringBuilder s = new StringBuilder(opts.next().jsonize());
        while (opts.hasNext()) {
            Component o = opts.next();
            s.append(String.format(", %s", o.jsonize()));
        }
        return String.format("[ %s ]", s.toString());
    }

    public static String html(Component c) {
        if (c instanceof StringComponent)
            return CSVLexer.xmlChars2HTML(((StringComponent) c).data).replace("\"", "&quot;");
        else {
            String data = ((HTMLComponent) c).data;
            return data.replace("\"", "&quot;");
        }
    }

    /**
     * Components are hashed on their identifiers.
     * @return int
     */
    @Override
    public int hashCode() {
        return cid.hashCode();
    }

    /**
     * A string representation comprised of the identifier and the relative index.
     * @return
     */
    @Override
    public String toString() {
        return "cid:" + cid + " index:" + index;
    }
}





package edu.umass.cs.surveyman.utils.jsonify;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;

import javax.annotation.Nonnull;
import java.lang.String;

class Boolean implements Jsonable, Comparable {

    boolean b;

    Boolean(@Nonnull boolean b) {
        this.b = b;
    }


    @Override
    public String jsonize() throws SurveyException
    {
        return java.lang.Boolean.toString(this.b);
    }

    @Override
    public int compareTo(@Nonnull Object o)
    {
        boolean that;
        if (o instanceof Boolean) {
            that = ((Boolean) o).b;
        } else if (o instanceof java.lang.Boolean) {
            that = (java.lang.Boolean) o;
        } else throw new RuntimeException("Can only compare booleans");

        if (this.b && !that) return 1;
        else if (!this.b && that) return -1;
        else return 0;
    }
}

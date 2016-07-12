package edu.umass.cs.surveyman.utils.jsonify;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;

import javax.annotation.Nonnull;

class String implements Jsonable {

    public final java.lang.String s;

    public String(java.lang.String s) {
        this.s = s;
    }

    @Override
    public java.lang.String jsonize() throws SurveyException
    {
        return "\"" + this.s + "\"";
    }

    @Override
    public boolean equals(@Nonnull Object o) {
        return this.s.equals(o.toString());
    }

    @Override
    public int hashCode() {
        return this.s.hashCode();
    }

    @Override
    public java.lang.String toString() {
        return this.s;
    }

}

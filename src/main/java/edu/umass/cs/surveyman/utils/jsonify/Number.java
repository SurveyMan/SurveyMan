package edu.umass.cs.surveyman.utils.jsonify;

import edu.umass.cs.surveyman.utils.Jsonable;

import javax.annotation.Nonnull;

class Number implements Jsonable, Comparable {

    enum type { INTEGER, DOUBLE }

    int n;
    double d;
    type t;

    Number(int n) {
        this.n = n;
        this.t = type.INTEGER;
    }

    Number(double d) {
        this.d = d;
        this.t = type.DOUBLE;
    }

    Number inc () {
        switch (t) {
            case INTEGER: this.n++; return this;
            case DOUBLE: this.d += 1; return this;
            default: return this;
        }
    }

    Number add (int n) {
        this.n += n;
        return this;
    }

    Number add (double d) {
        this.d += d;
        return this;
    }


    @Override
    public java.lang.String jsonize()
    {
        switch (t) {
            case INTEGER: return Integer.toString(this.n);
            case DOUBLE: return Double.toString(this.d);
            default: return "";
        }
    }

    @Override
    public boolean equals (Object o) {
        if (o instanceof Number) {
            Number that = (Number) o;
            switch (this.t) {
                case INTEGER: return this.n == that.n;
                case DOUBLE: return this.d == that.d;
                default: return false;
            }
        } else if (o instanceof Integer) {
            Integer that = (Integer) o;
            return this.n == that;
        } else if (o instanceof Double) {
            Double that = (Double) o;
            return this.d == that;
        } else return false;
    }

    @Override
    public int hashCode() {
        switch (t) {
            case INTEGER: return new Integer(this.n).hashCode();
            case DOUBLE: return new Double(this.d).hashCode();
            default: return -1;
        }
    }

    @Override
    public java.lang.String toString() {
        return this.jsonize();
    }


    @Override
    public int compareTo(@Nonnull Object o)
    {
        RuntimeException badComparison = new RuntimeException("Cannot compare between numbers of differing base types.");
        if (o instanceof Number) {
            Number that = (Number) o;
            if (this.t == that.t) {
                switch (this.t) {
                    case INTEGER: return new Integer(this.n).compareTo(that.n);
                    case DOUBLE: return new Double(this.d).compareTo(that.d);
                    default: throw new RuntimeException("Unknown numeric enum.");
                }
            } else throw badComparison;
        } else if (o instanceof Integer) {
            Integer that = (Integer) o;
            if (this.t.equals(type.INTEGER)) {
                return new Integer(this.n).compareTo(that);
            } else throw badComparison;
        } else if (o instanceof Double) {
            Double that = (Double) o;
            if (this.t.equals(type.DOUBLE)) {
                return new Double(this.d).compareTo(that);
            } else throw badComparison;
        } else throw badComparison;
    }


}

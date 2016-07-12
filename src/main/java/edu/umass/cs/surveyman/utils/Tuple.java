package edu.umass.cs.surveyman.utils;

import javax.annotation.Nonnull;

public class Tuple<A extends Comparable, B extends Comparable> implements Comparable {

    public final A fst;
    public final B snd;

    public Tuple(A fst, B snd) {
        this.fst = fst;
        this.snd = snd;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Tuple) {
            Tuple that = (Tuple) o;
            return fst.equals(that.fst) && snd.equals(that.snd);
        } else return false;
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", fst.toString(), snd.toString());
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public int compareTo(@Nonnull Object o) {
        if (o instanceof Tuple) {
            Tuple that = (Tuple) o;
            int cmp = this.fst.compareTo(that.fst);
            if (cmp != 0) {
                return cmp;
            } else return this.snd.compareTo(that.snd);
        } else throw new RuntimeException(String.format("Don't know how to compare between tuple and %s",
                o.getClass().getName()));
    }
}

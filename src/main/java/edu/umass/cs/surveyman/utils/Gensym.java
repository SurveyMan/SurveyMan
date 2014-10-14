package edu.umass.cs.surveyman.utils;

/**
 * Internal identifier generator.
 */
public class Gensym {

    private static int counter = 0;
    private final String prefix;

    public Gensym (String prefix) {
        this.prefix = prefix;
    }

    public Gensym () {
        this.prefix = "";
    }

    public String next() {
        counter += 1;
        return prefix + counter;
    }
}
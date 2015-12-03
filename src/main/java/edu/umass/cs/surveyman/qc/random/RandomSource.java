package edu.umass.cs.surveyman.qc.random;

public interface RandomSource {

    boolean nextBoolean();

    int nextInt(int bound);

    void shuffle(Object[] coll);
}
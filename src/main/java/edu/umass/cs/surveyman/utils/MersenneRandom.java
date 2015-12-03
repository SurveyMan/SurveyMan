package edu.umass.cs.surveyman.utils;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomAdaptor;

import java.util.List;

/**
 * Wrapper for MersenneTwister that will allow us to
 */
public class MersenneRandom extends RandomAdaptor {
    public MersenneRandom() {
        super(new MersenneTwister(System.currentTimeMillis()));
    }

    public MersenneRandom(long seed) {
        super(new MersenneTwister(seed));
    }

    public void shuffle(Object[] coll) {
        // Implement FYShuffle
        for (int i = coll.length - 1; i > 0; i--) {
            int j = nextInt(i);
            Object tmp = coll[i];
            coll[i] = coll[j];
            coll[j] = tmp;
        }
    }

    public void shuffle(List coll) {
        for (int i = coll.size() - 1; i > 0 ; i--) {
            int j = nextInt(i);
            Object tmp = coll.get(i);
            coll.set(i, coll.get(j));
            coll.set(j, tmp);
        }
    }

}

package edu.umass.cs.surveyman.utils;

import org.apache.commons.math3.random.MersenneTwister;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * Wrapper for MersenneTwister that will allow us to
 */
public class MersenneRandom extends Random {

    private final MersenneTwister rng;

    public MersenneRandom() {
        rng = new MersenneTwister(System.currentTimeMillis());
    }

    public MersenneRandom(long seed) {
        rng = new MersenneTwister(seed);
    }

    @Override
    public boolean nextBoolean() {
        return rng.nextBoolean();
    }

    @Override
    public int nextInt() {
        return rng.nextInt();
    }

    @Override
    protected int next(int bits) {
        return rng.nextInt(bits);
    }

    @Override
    public void nextBytes(byte[] bytes) {
        rng.nextBytes(bytes);
    }

    @Override
    public float nextFloat() {
        return rng.nextFloat();
    }

    @Override
    public double nextGaussian() {
        return rng.nextGaussian();
    }

    @Override
    public long nextLong() {
        return rng.nextLong();
    }

    @Override
    public int nextInt(int n) {
        return rng.nextInt(n);
    }

    public void shuffle(Object[] coll) {
        // Implement FYShuffle
        for (int i = coll.length - 1; i > 0; i--) {
            int j = rng.nextInt(i);
            Object tmp = coll[i];
            coll[i] = coll[j];
            coll[j] = tmp;
        }
    }

    public void shuffle(List coll) {
        for (int i = coll.size() - 1; i > 0 ; i--) {
            int j = rng.nextInt(i);
            Object tmp = coll.get(i);
            coll.set(i, coll.get(j));
            coll.set(j, tmp);
        }
    }

}

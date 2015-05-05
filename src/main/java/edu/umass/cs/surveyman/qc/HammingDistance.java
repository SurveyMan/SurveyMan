package edu.umass.cs.surveyman.qc;

import org.apache.commons.math3.ml.distance.DistanceMeasure;

public class HammingDistance implements DistanceMeasure {

    /**
     * Computes the Hamming distance between two vectors. May cause a loss of precision, if the doubles are "really"
     * doubles (in that case, please don't use).
     * @param a n-dimensional vector of values.
     * @param b n-dimensional vector of values
     * @return Hamming distance between a and b.
     */
    @Override
    public double compute(double[] a, double[] b)
    {
        assert a.length == b.length : "Vectors must be of equal length.";
        double differences = 0;
        for (int i = 0; i < a.length; i++) {
            int aa =  (int) Math.round(a[i]);
            int bb = (int) Math.round(b[i]);
            if (aa != bb) differences++;
        }
        return differences;
    }
}

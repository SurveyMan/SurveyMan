package edu.umass.cs.surveyman.qc;

public class CorrelationStruct {

    public final CoefficentsAndTests coefficientType;
    public final double coefficientValue;
    public final Object thingA;
    public final Object thingB;
    public final int numSamplesA;
    public final int numSamplesB;

    public CorrelationStruct(CoefficentsAndTests coefficientType,
                             double coefficientValue,
                             Object thingA,
                             Object thingB,
                             int numSamplesA,
                             int numSamplesB) {
        this.coefficientType = coefficientType;
        this.coefficientValue = coefficientValue;
        this.thingA = thingA;
        this.thingB = thingB;
        this.numSamplesA = numSamplesA;
        this.numSamplesB = numSamplesB;
    }

}

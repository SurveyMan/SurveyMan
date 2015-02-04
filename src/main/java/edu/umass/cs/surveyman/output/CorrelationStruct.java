package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.qc.CoefficentsAndTests;

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

    public String jsonize(){
        return String.format(
                "{ " +
                        "\"coefficientType\" : \"%s\"," +
                        "\"coefficientValue\" : %f," +
                        "\"numSamplesA\" : %d," +
                        "\"numSamplesB\" : %s" +
                "}",
                this.coefficientType,
                this.coefficientValue,
                this.numSamplesA,
                this.numSamplesB
        );
    }

    @Override
    public String toString(){
        return String.format(
                "Coefficient type:\t%s\nCoefficient Value:\t%f\nNumber of Samples %s:\t%d\nNumber of Samples %s:\t%d",
                this.coefficientType,
                this.coefficientValue,
                this.thingA.getClass().getName(),
                this.numSamplesA,
                this.thingB.getClass().getName(),
                this.numSamplesB);
    }
}

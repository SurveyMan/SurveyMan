package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.qc.CoefficentsAndTests;
import edu.umass.cs.surveyman.survey.Question;

public class CorrelationStruct {

    public final CoefficentsAndTests coefficientType;
    public final double coefficientValue;
    public final Question thingA;
    public final Question thingB;
    public final int numSamplesA;
    public final int numSamplesB;

    public CorrelationStruct(CoefficentsAndTests coefficientType,
                             double coefficientValue,
                             Question thingA,
                             Question thingB,
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
                        "\"thingA\" : \"%s\"," +
                        "\"numSamplesA\" : %d," +
                        "\"thingB\" : \"%s\"," +
                        "\"numSamplesB\" : %d" +
                "}",
                this.coefficientType,
                this.coefficientValue,
                this.thingA.quid,
                this.numSamplesA,
                this.thingB.quid,
                this.numSamplesB
        );
    }

    @Override
    public String toString(){
        return String.format(
                //"Coefficient type:\t%s\nCoefficient Value:\t%f\nNumber of Samples %s:\t%d\nNumber of Samples %s:\t%d",
                "%s\t%f\t%s\t%d\t%s\t%d",
                this.coefficientType,
                this.coefficientValue,
                this.thingA.getClass().getName(),
                this.numSamplesA,
                this.thingB.getClass().getName(),
                this.numSamplesB);
    }
}

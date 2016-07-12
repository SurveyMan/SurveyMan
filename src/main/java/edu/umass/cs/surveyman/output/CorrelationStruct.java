package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.qc.CoefficentsAndTests;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.InputOutputKeys;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;
import edu.umass.cs.surveyman.utils.jsonify.Jsonify;
import edu.umass.cs.surveyman.utils.Tabularable;
import edu.umass.cs.surveyman.utils.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CorrelationStruct implements Jsonable, Tabularable {

    public final CoefficentsAndTests coefficientType;
    public final double coefficientValue;
    public final double coefficientPValue;
    public final Question thingA;
    public final Question thingB;
    public final int numSamplesA;
    public final int numSamplesB;

    protected boolean empty = true;

    public CorrelationStruct() {
        this.coefficientType = null;
        this.coefficientValue = -1;
        this.coefficientPValue = -1;
        this.thingA = null;
        this.thingB = null;
        this.numSamplesA = -1;
        this.numSamplesB = -1;
    }


    public CorrelationStruct(
            CoefficentsAndTests coefficientType,
            double coefficientValue,
            double coefficientPValue,
            Question thingA,
            Question thingB,
            int numSamplesA,
            int numSamplesB)
    {
        this.coefficientType = coefficientType;
        this.coefficientValue = coefficientValue;
        this.coefficientPValue = coefficientPValue;
        this.thingA = thingA;
        this.thingB = thingB;
        this.numSamplesA = numSamplesA;
        this.numSamplesB = numSamplesB;
        this.empty = false;
    }

    public static CorrelationStruct makeStruct(
            Question q1,
            Question q2,
            Map<java.lang.String, IQuestionResponse> q1responses,
            Map<java.lang.String, IQuestionResponse> q2responses)
            throws SurveyException
    {
        if (QCMetrics.isAnalyzable(q1) && QCMetrics.isAnalyzable(q2)
                && q1.exclusive && q2.exclusive) {
            if (q1.ordered && q2.ordered) {
                return new CorrelationStruct(
                        CoefficentsAndTests.RHO,
                        QCMetrics.spearmansRho(q1responses, q2responses),
                        -1.,
                        q1,
                        q2,
                        q1responses.size(),
                        q2responses.size());
            } else {
                List<Tuple<SurveyDatum, SurveyDatum>> tuples = new ArrayList<>();
                List<Tuple<SurveyDatum, SurveyDatum>> dummy = new ArrayList<>();
                for (Map.Entry<String, IQuestionResponse> e : q1responses.entrySet()) {
                    String srid = e.getKey();
                    SurveyDatum fst = e.getValue().getAnswer();
                    if (q2responses.containsKey(srid)) {
                        SurveyDatum snd = q2responses.get(srid).getAnswer();
                        tuples.add(new Tuple<>(fst, snd));
                    }
                }
                Tuple<Double, Double> test = QCMetrics.cramersV(q1.options.values(), q2.options.values(), tuples, dummy);
                return new CorrelationStruct(
                        CoefficentsAndTests.V,
                        test.fst,
                        test.snd,
                        q1,
                        q2,
                        q1responses.size(),
                        q2responses.size()
                );
            }
        } else return null;
    }

    public java.lang.String jsonize() throws SurveyException
    {
        if (this.empty) return "\"\"";
        return Jsonify.jsonify(Jsonify.mapify(
                InputOutputKeys.COEFFICIENTTYPE, this.coefficientType.toString(),
                InputOutputKeys.COEFFICIENTVALUE, this.coefficientValue,
                InputOutputKeys.COEFFICIENTPVALUE, this.coefficientPValue,
                InputOutputKeys.THINGA, this.thingA.getId(),
                this.numSamplesA, this.numSamplesA,
                InputOutputKeys.THINGB, this.thingB.getId(),
                InputOutputKeys.NUMSAMPLESB, this.numSamplesB
        ));
    }

    @Override
    public java.lang.String tabularize()
    {
        if (this.empty) return "\"\"";
        return java.lang.String.format(
                "%s\t%f\t%f\t%s\t%d\t%s\t%d",
                this.coefficientType,
                this.coefficientValue,
                this.coefficientPValue,
                this.thingA.getClass().getName(),
                this.numSamplesA,
                this.thingB.getClass().getName(),
                this.numSamplesB);
    }

    @Override
    public java.lang.String toString()
    {
        return this.tabularize();
    }
}

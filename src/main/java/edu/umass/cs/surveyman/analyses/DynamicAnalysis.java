package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.qc.CorrelationStruct;
import edu.umass.cs.surveyman.qc.BreakoffStruct;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;

import java.util.List;
import java.util.Map;

public class DynamicAnalysis {

    public static class Report {

        public final String surveyName;
        public final String sid;
        public final Map<Question, Map<Question, CorrelationStruct>> orderBiases;
        public final List<Map<Question, Map<Question, CorrelationStruct>>> wordingBiases;
        public final List<BreakoffStruct> breakoff;
        public final List<BreakoffStruct> abandonment;

        public Report(
                String surveyName,
                String sid,
                Map<Question, Map<Question, CorrelationStruct>> orderBiases,
                List<Map<Question, Map<Question, CorrelationStruct>>> wordingBiases,
                List<BreakoffStruct> breakoff,
                List<BreakoffStruct> abandonment) {
            this.surveyName = surveyName;
            this.sid = sid;
            this.orderBiases = orderBiases;
            this.wordingBiases = wordingBiases;
            this.breakoff = breakoff;
            this.abandonment = abandonment;
        }
    }


    public static Report dynamicAnalysis(
            Survey survey,
            List<ISurveyResponse> responses,
            Classifier classifier,
            double alpha) {
        return new Report(
                survey.sourceName,
                survey.sid,
                QCMetrics.calculateOrderBiases(survey, responses),
                QCMetrics.calculateWordingBiases(survey, responses),
                QCMetrics.calculateBreakoff(survey, responses),
                QCMetrics.calculateAbandonment(survey, responses)
        );
    }
}
package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.AbstractSurveyResponse;
import edu.umass.cs.surveyman.qc.Classifier;

public class ClassificationStruct {

    public final AbstractSurveyResponse surveyResponse;
    public final Classifier classifier;
    public final int numanswered;
    public final double score;
    public final double threshold;
    public final boolean valid;

    public ClassificationStruct(
            AbstractSurveyResponse surveyResponse,
            Classifier classifier,
            int numanswered,
            double score,
            double threshold,
            boolean valid){
        this.surveyResponse = surveyResponse;
        this.classifier = classifier;
        this.numanswered = numanswered;
        this.score = score;
        this.threshold = threshold;
        this.valid = valid;
    }

    @Override
    public String toString()
    {
        return String.format("%s, %s, %d, %f, %f, %b",
                this.surveyResponse.getSrid(),
                this.classifier.name(),
                this.numanswered,
                this.score,
                this.threshold,
                this.valid);
    }

    public String jsonize()
    {
        return String.format(
                "{\"responseid\" : \"%s\", " +
                        "\"classifier\" : \"%s\", " +
                        "\"numanswered\" : %d," +
                        "\"score\" : %f, " +
                        "\"threshold\" : %f, " +
                        "\"valid\" : %b}",
                this.surveyResponse.getSrid(),
                this.classifier.name(),
                this.numanswered,
                this.score,
                this.threshold,
                this.valid);
    }
}

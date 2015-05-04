package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.Classifier;

public class ClassificationStruct {

    public final SurveyResponse surveyResponse;
    public final Classifier classifier;
    public final int numanswered;
    public final double score;
    public final double threshold;
    public final boolean valid;
    protected final String RESPONSEID = "responseid";
    protected final String CLASSIFIER = "classifier";
    protected final String NUMANSWERED = "numanswered";
    protected final String SCORE = "score";
    protected final String THRESHOLD = "threshold";
    protected final String VALID = "valid";

    public ClassificationStruct(
            SurveyResponse surveyResponse,
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
                "{\"%s\" : \"%s\", " +
                        "\"%s\" : \"%s\", " +
                        "\"%s\" : %d," +
                        "\"%s\" : %f, " +
                        "\"%s\" : %f, " +
                        "\"%s\" : %b}",
                this.RESPONSEID, this.surveyResponse.getSrid(),
                this.CLASSIFIER, this.classifier.name(),
                this.NUMANSWERED, this.numanswered,
                this.SCORE, this.score,
                this.THRESHOLD, this.threshold,
                this.VALID, this.valid);
    }
}

package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.ISurveyResponse;
import edu.umass.cs.surveyman.qc.Classifier;

public class ClassificationStruct {

    public final ISurveyResponse surveyResponse;
    public final Classifier classifier;
    public final double score;
    public final double threshold;
    public final boolean valid;

    public ClassificationStruct(
            ISurveyResponse surveyResponse,
            Classifier classifier,
            double score,
            double threshold,
            boolean valid){
        this.surveyResponse = surveyResponse;
        this.classifier = classifier;
        this.score = score;
        this.threshold = threshold;
        this.valid = valid;
    }

    @Override
    public String toString(){
        return String.format("%s, %s, %f, %f, %b",
                this.surveyResponse.getSrid(),
                this.classifier.name(),
                this.score,
                this.threshold,
                this.valid);
    }

    public String jsonize(){
        return String.format(
                "{\"responseid\" : \"%s\", \"classifier\" : \"%s\", \"score\" : %f, \"threshold\" : %f, \"valid\" : %b}",
                this.surveyResponse.getSrid(),
                this.classifier.name(),
                this.score,
                this.threshold,
                this.valid);
    }
}

package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.classifiers.AbstractClassifier;
import edu.umass.cs.surveyman.survey.InputOutputKeys;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;
import edu.umass.cs.surveyman.utils.jsonify.Jsonify;
import edu.umass.cs.surveyman.utils.Tabularable;

public class ClassificationStruct implements Jsonable, Tabularable {

    public final SurveyResponse surveyResponse;
    public final AbstractClassifier classifier;
    public final int numanswered;
    public final double score;
    public final double threshold;
    private final boolean valid;

    public ClassificationStruct(SurveyResponse surveyResponse, AbstractClassifier classifier) throws SurveyException {
        this.surveyResponse = surveyResponse;
        this.classifier = classifier;
        this.numanswered = surveyResponse.getNonCustomResponses().size();
        this.score = surveyResponse.getScore();
        this.threshold = surveyResponse.getThreshold();
        this.valid = classifier.classifyResponse(surveyResponse);
    }

    public boolean isValid() {
        return this.valid;
    }

    @Override
    public java.lang.String toString () {
        return this.tabularize();
    }

    @Override
    public java.lang.String tabularize ()
    {
        return java.lang.String.format("%s, %s, %d, %f, %f, %b",
                this.surveyResponse.getSrid(),
                this.classifier.getClass().getName(),
                this.numanswered,
                this.score,
                this.threshold,
                this.valid);
    }

    public java.lang.String jsonize() throws SurveyException
    {
        return Jsonify.jsonify(Jsonify.mapify(
                InputOutputKeys.RESPONSEID, this.surveyResponse.getSrid(),
                InputOutputKeys.CLASSIFIER, this.classifier.getClass().getName(),
                InputOutputKeys.NUMANSWERED, this.numanswered,
                InputOutputKeys.SCORE, this.score,
                InputOutputKeys.THRESHOLD, this.threshold,
                InputOutputKeys.VALID, this.valid));
    }
}

package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractSurveyResponse {

    private List<IQuestionResponse> responses = new ArrayList<IQuestionResponse>();
    private boolean recorded = false;
    private String srid = "";
    private String workerid = "";
    private double score;
    private double threshold;
    private KnownValidityStatus status;

    public AbstractSurveyResponse(){}

    public AbstractSurveyResponse(AbstractSurveyResponse sr) {
        this.recorded = sr.isRecorded();
        this.srid = sr.getSrid();
        this.workerid = sr.getWorkerId();
        this.score = sr.getScore();
        this.threshold = sr.getThreshold();
        this.status = sr.getKnownValidityStatus();
    }

    public List<IQuestionResponse> getAllResponses(){
        return this.responses;
    }

    /**
     * Returns a filtered copy of the resposnes, with the custom identifiers removed.
     * @return A List of IQuestionResponses, containing only questions in the original survey.
     */
    public List<IQuestionResponse> getNonCustomResponses() {
        List<IQuestionResponse> retval = new ArrayList<IQuestionResponse>();
        for (IQuestionResponse iqr : this.getAllResponses())
            if (!Question.customQuestion(iqr.getQuestion().quid))
                retval.add(iqr);
        return retval;
    }

    public void setResponses(List<IQuestionResponse> responses) {
        this.responses = responses;
    }

    public void addResponse(IQuestionResponse questionResponse) {
        this.responses.add(questionResponse);
    }

    public boolean isRecorded(){
        return recorded;
    }

    public void setRecorded(boolean recorded){
        this.recorded = recorded;
    }

    public String getSrid() {
        return srid;
    }

    public void setSrid(String srid) {
        this.srid = srid;
    }

    public void setWorkerid(String workerid) {
        this.workerid = workerid;
    }

    public String getWorkerId() {
        return workerid;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getScore(){
        return this.score;
    }

    public void setThreshold(double pval){
        this.threshold = pval;
    }

    public double getThreshold(){
        return this.threshold;
    }

    public KnownValidityStatus getKnownValidityStatus(){
        return this.status;
    }

    public void setKnownValidityStatus(KnownValidityStatus validityStatus){
        this.status = validityStatus;
    }

    public boolean hasResponseForQuestion(Question q) {
        for (IQuestionResponse qr : this.getNonCustomResponses())
            if (qr.getQuestion().equals(q))
                return true;
        return false;
    }

    public IQuestionResponse getResponseForQuestion(Question q) {
        for (IQuestionResponse qr : this.getNonCustomResponses()) {
            if (qr.getQuestion().equals(q))
                return qr;
        }
        throw new RuntimeException("Could not find question %s" + q){};
    }

    public abstract Map<String,IQuestionResponse> resultsAsMap();
    public abstract boolean surveyResponseContainsAnswer(List<Component> variants);

    @Override
    public boolean equals(Object foo) {
        if (foo instanceof AbstractSurveyResponse) {
            AbstractSurveyResponse ar = (AbstractSurveyResponse) foo;
            return this.getSrid().equals(ar.getSrid()) &&
                    this.getNonCustomResponses().equals(ar.getNonCustomResponses());
        } else return false;
    }

    public abstract AbstractSurveyResponse copy();
}
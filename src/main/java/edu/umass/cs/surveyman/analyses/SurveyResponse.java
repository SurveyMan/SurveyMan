package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.utils.Gensym;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SurveyResponse {

    public static final Gensym gensym = new Gensym("asr");
    private List<IQuestionResponse> responses;
    private boolean recorded = false;
    private String srid;
    private double score;
    private double threshold;
    private KnownValidityStatus status;

    public SurveyResponse() {
        this.responses = new ArrayList<IQuestionResponse>();
        this.srid = gensym.next();
    }

    public SurveyResponse(
            List<IQuestionResponse> responses,
            String srid,
            double score,
            double threshold,
            KnownValidityStatus knownValidityStatus)
    {
        this.responses = responses;
        this.srid = srid;
        this.score = score;
        this.threshold = threshold;
        this.status = knownValidityStatus;
    }

    private SurveyResponse(SurveyResponse surveyResponse) {
        this.responses = new ArrayList<IQuestionResponse>(surveyResponse.getAllResponses());
        this.srid = gensym.next();
        this.status = surveyResponse.getKnownValidityStatus();
    }

    public List<IQuestionResponse> getAllResponses() {
        return this.responses;
    }

    public void setResponses(List<IQuestionResponse> responses) {
        this.responses = responses;
    }

    public void addResponse(IQuestionResponse questionResponse)
    {
        this.responses.add(questionResponse);
    }

    public boolean isRecorded()
    {
        return recorded;
    }

    public void setRecorded(boolean recorded)
    {
        this.recorded = recorded;
    }

    public String getSrid()
    {
        return this.srid;
    }

    public void setSrid(String srid)
    {
        this.srid = srid;
    }

    public void setScore(double score)
    {
        this.score = score;
    }

    public double getScore()
    {
        return score;
    }

    public void setThreshold(double pval)
    {
        this.threshold = pval;
    }

    public double getThreshold()
    {
        return threshold;
    }

    public KnownValidityStatus getKnownValidityStatus()
    {
        return status;
    }

    public void setKnownValidityStatus(KnownValidityStatus validityStatus)
    {
        this.status = validityStatus;
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

    public Map<String, IQuestionResponse> resultsAsMap() {
        Map<String, IQuestionResponse> retval = new HashMap<String, IQuestionResponse>();
        for (IQuestionResponse iQuestionResponse : this.getAllResponses()) {
            retval.put(iQuestionResponse.getQuestion().quid, iQuestionResponse);
        }
        return retval;
    }

    public boolean surveyResponseContainsAnswer(List<Component> variants) {
        for (IQuestionResponse qr : this.getNonCustomResponses()) {
            for (OptTuple tupe : qr.getOpts()) {
                if (variants.contains(tupe.c))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object foo) {
        if (foo instanceof SurveyResponse) {
            SurveyResponse ar = (SurveyResponse) foo;
            return this.getSrid().equals(ar.getSrid()) &&
                    this.getNonCustomResponses().equals(ar.getNonCustomResponses());
        } else return false;
    }

    public SurveyResponse copy() {
        return new SurveyResponse(this);
    }

}
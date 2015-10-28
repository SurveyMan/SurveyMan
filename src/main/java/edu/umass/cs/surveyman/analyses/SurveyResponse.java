package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;
import org.apache.commons.math3.ml.clustering.Clusterable;

import java.util.*;

public class SurveyResponse implements Clusterable {

    public static final Gensym gensym = new Gensym("asr");
    private List<IQuestionResponse> responses;
    private boolean recorded = false;
    private String srid;
    private double score;
    private double threshold;
    private KnownValidityStatus status;
    private KnownValidityStatus label;
    private final Survey survey;
    public boolean noise = false;
    public String clusterLabel = "";
    public Clusterable center;

    public SurveyResponse(Survey survey) {
        this.responses = new ArrayList<>();
        this.srid = gensym.next();
        this.survey = survey;
    }

    public SurveyResponse(
            Survey survey,
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
        this.survey = survey;
    }

    private SurveyResponse(SurveyResponse surveyResponse) {
        this.responses = new ArrayList<>(surveyResponse.getAllResponses());
        this.srid = gensym.next();
        this.status = surveyResponse.getKnownValidityStatus();
        this.survey = surveyResponse.survey;
    }

    public Survey getSurvey()
    {
        return this.survey;
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

    public KnownValidityStatus getComputedValidityStatus()
    {
        return this.label;
    }

    public void setComputedValidityStatus(KnownValidityStatus status)
    {
        this.label = status;
    }

    public IQuestionResponse getLastQuestionAnswered() {
        int lastIndexSeen = -1;
        IQuestionResponse retval = null;
        for (IQuestionResponse questionResponse: this.getAllResponses()) {
            int indexSeen = questionResponse.getIndexSeen();
            if (indexSeen > lastIndexSeen) {
                lastIndexSeen = indexSeen;
                retval = questionResponse;
            }
        }
        return retval;
    }

    /**
     * Returns a filtered copy of the resposnes, with the custom identifiers removed.
     * @return A List of IQuestionResponses, containing only questions in the original survey.
     */
    public List<IQuestionResponse> getNonCustomResponses() {
        List<IQuestionResponse> retval = new ArrayList<IQuestionResponse>();
        for (IQuestionResponse iqr : this.getAllResponses())
            if (!Question.customQuestion(iqr.getQuestion().id))
                retval.add(iqr);
        return retval;
    }

    /**
     * Checks whether the respondent saw and answered the input question.
     * @param q The question of interest.
     * @return boolean indicating whether the respondent answered the input question.
     */
    public boolean hasResponseForQuestion(Question q) {
        for (IQuestionResponse qr : this.getNonCustomResponses())
            if (qr.getQuestion().equals(q) && !q.isInstructional())
                return true;
        return false;
    }

    /**
     * Gets the response to the input quesiton. Use with hasResponseForQuestion.
     * @param q The question whose response is needed.
     * @return Respondent's response to this question.
     * @throws java.lang.RuntimeException if the question is not in the SurveyResponse.
     */
    public IQuestionResponse getResponseForQuestion(Question q) {
        for (IQuestionResponse qr : this.getNonCustomResponses()) {
            if (qr.getQuestion().equals(q))
                return qr;
        }
        throw new RuntimeException(String.format("Could not find question %s", q.toString()));
    }

    public Map<String, IQuestionResponse> resultsAsMap() {
        Map<String, IQuestionResponse> retval = new HashMap<String, IQuestionResponse>();
        for (IQuestionResponse iQuestionResponse : this.getAllResponses()) {
            retval.put(iQuestionResponse.getQuestion().id, iQuestionResponse);
        }
        return retval;
    }

    public boolean surveyResponseContainsAnswer(List<SurveyDatum> variants) {
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

    /**
     * Required by the Clusterable interface. This function returns an array the size of the total number of survey
     * questions. Each value is
     * @return
     */
    @Override
    public double[] getPoint()
    {
        Question[] questions = this.survey.getQuestionListByIndex();
        double[] retval = new double[questions.length];
        Arrays.fill(retval, 0.0);
        for (int i = 0; i < questions.length; i++) {
            Question q = questions[i];
            if (this.hasResponseForQuestion(q)) {
                IQuestionResponse questionResponse = this.getResponseForQuestion(q);
                try {
                    retval[i] = q.responseToDouble(questionResponse.getOpts(), noise);
                } catch (SurveyException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
        return retval;
    }
}
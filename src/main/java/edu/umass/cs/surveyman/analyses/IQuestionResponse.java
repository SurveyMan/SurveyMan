package edu.umass.cs.surveyman.analyses;

import java.util.List;

import edu.umass.cs.surveyman.survey.Component;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

/**
 * This is the data structure that holds a respondent's answer to a question.
 */
public interface IQuestionResponse extends Comparable {

    /**
     * Gets the question associated with this IQuestionResponse
     * @return A Question object
     */
    public Question getQuestion();

    /**
     * Gets the list of answers the respondent provided for this question. For radio button questions, this will hold
     * exactly one OptTuple. For checkbox questions, it will hold more than one.
     * @return A list of {@link edu.umass.cs.surveyman.analyses.OptTuple}s, corresponding to answer data.
     */
    public List<OptTuple> getOpts();

    /**
     * Gets the index at which the question in this question response was seen.
     * @return A 0-based integer index.
     */
    public int getIndexSeen();

    /**
     * Convenience method for getting a single answer, if this question is marked exclusive (i.e., radio button).
     * @return The component corresponding to the respondent's answer.
     * @throws edu.umass.cs.surveyman.survey.exceptions.SurveyException if this question is not marked exclusive.
     */
    public Component getAnswer() throws SurveyException;

    /**
     * Convenience method for getting a list of answers, if the question is marked not exclusive (i.e., checkbox).
     * @return A list of Components corresponding to the respondent's answers.
     * @throws SurveyException if this question is marked exclusive.
     */
    public List<Component> getAnswers() throws SurveyException;


}

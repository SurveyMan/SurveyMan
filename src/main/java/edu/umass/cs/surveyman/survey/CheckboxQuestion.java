package edu.umass.cs.surveyman.survey;

/**
 * Convenience class for representing check box questions.
 */
public class CheckboxQuestion extends Question {

    public CheckboxQuestion(SurveyDatum surveyDatum) {
        super(surveyDatum);
    }

    /**
     * Creates a new question object using the string data.
     * @param data The data associated with this question.
     * @param ordered True if the question's responses are ordered.
     */
    public CheckboxQuestion(String data, boolean ordered)
    {
        super(data, Question.nextRow, Question.QUESTION_COL);
        this.ordered = ordered;
        this.exclusive = false;
        this.freetext = false;
        Question.nextRow++;
    }

}

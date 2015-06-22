package edu.umass.cs.surveyman.survey;

/**
 * Convenience class for representing radio button questions.
 */
public class RadioButtonQuestion extends Question {

    public RadioButtonQuestion(SurveyDatum surveyDatum) {
        super(surveyDatum);
    }

    /**
     * Creates a new question object using the string data.
     * @param data The data associated with this question.
     * @param ordered True if the question's responses are ordered.
     */
    public RadioButtonQuestion(String data, boolean ordered)
    {
        super(data, Question.nextRow, Question.QUESTION_COL);
        this.ordered = ordered;
        this.exclusive = true;
        this.freetext = false;
        Question.nextRow++;
    }

}

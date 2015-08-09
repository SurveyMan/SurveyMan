package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class QuestionTest extends TestLog {

    public QuestionTest()
            throws IOException,
            SyntaxException
    {
        super.init(this.getClass());
    }

    @Test
    public void testAddQuestion()
            throws SurveyException
    {
        Survey survey = new Survey();
        Assert.assertEquals(0, survey.getAllBlocks().size());
        Assert.assertEquals(0, survey.questions.size());

        Question q1 = new Question("asdf", 1, 1);
        survey.addQuestion(q1);
        Assert.assertTrue(q1.branchMap.isEmpty());
        Assert.assertTrue(q1.exclusive);
        Assert.assertFalse(q1.ordered);
        Assert.assertFalse(q1.freetext);
        Assert.assertFalse(survey.questions.isEmpty());

        Question q2 = new Question(new StringDatum("fdsa", 2, 2, -1), 2, 1);
        survey.addQuestion(q2);
        Assert.assertTrue(q2.branchMap.isEmpty());
        Assert.assertTrue(q2.exclusive);
        Assert.assertFalse(q2.ordered);
        Assert.assertFalse(q2.freetext);
        Assert.assertEquals(2, survey.questions.size());

        Question q3 = new RadioButtonQuestion("foo", true);
        survey.addQuestion(q3);
        Assert.assertTrue(q2.branchMap.isEmpty());
        Assert.assertTrue(q3.exclusive);
        Assert.assertTrue(q3.ordered);
        Assert.assertFalse(q3.freetext);
        Assert.assertEquals(3, survey.questions.size());

        Question q4 = new Question("aaa");
        Question q5 = new Question("bbb");

        survey.addQuestions(q4, q5);
    }
}
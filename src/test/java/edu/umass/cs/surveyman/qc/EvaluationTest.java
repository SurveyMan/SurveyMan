package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.qc.classifiers.AbstractClassifier;
import edu.umass.cs.surveyman.qc.classifiers.EntropyClassifier;
import edu.umass.cs.surveyman.qc.classifiers.LogLikelihoodClassifier;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(JUnit4.class)
public class EvaluationTest extends TestLog {

    Gensym id = new Gensym("f");

    public EvaluationTest()
            throws IOException,
            SyntaxException
    {
        super.init(this.getClass());
    }

    private void writeData(
            AbstractClassifier classifier,
            String comment,
            StaticAnalysis.Report report)
    {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(classifier + "_" + id.next());
            LOGGER.debug(comment);
            report.print(fos);
            fos.close();
        } catch (Exception e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
    }

    @Test
    public void testEvaluation()
            throws SurveyException
    {
        // Destination to print to
        double granularity = 0.01;
        double alpha = 0.05;
        StaticAnalysis.Report report;

        // Test each classifier for a series of surveys having increasing entropy
        Question[] questions = new Question[5];
        Question.makeUnorderedRadioQuestions(questions, "1", "2", "3", "4", "5");
        questions[0].addOptions("A", "B");
        questions[1].addOptions("C", "D");
        questions[2].addOptions("E", "F");
        questions[3].addOptions("G", "H");
        questions[4].addOptions("I", "J");
        Survey survey = new Survey(questions);
        AbstractClassifier llClassifier = new LogLikelihoodClassifier(survey);
        AbstractClassifier entClassifier = new EntropyClassifier(survey);
        // Test LL classifier
        report = StaticAnalysis.staticAnalysis(survey, llClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(llClassifier, "Printing log likelihood scores for 5 questions with two options", report);
        // Now test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey, entClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(entClassifier, "Printing entropy classifier for 5 questions with two options.", report);
        // Now increase entropy by adding another option
        questions[0].addOption("K");
        questions[1].addOption("L");
        questions[2].addOption("M");
        questions[3].addOption("N");
        questions[4].addOption("O");
        // Test LL classifier
        report = StaticAnalysis.staticAnalysis(survey, llClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(llClassifier, "Printing log likelihood classifier for 5 questions with 3 options.", report);
        // Test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey, entClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(entClassifier, "Printing entropy classifier for 5 questions with 3 options.", report);
        // Now increase entropy by adding another question
        Question q6 = new Question("6");
        q6.addOptions("P", "Q", "R");
        survey.addQuestion(q6);
        // Test LL classifier
        report = StaticAnalysis.staticAnalysis(survey, llClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(entClassifier, "Printing log likelihood classifier for 6 questions with 3 options.", report);
        // Test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey, entClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(entClassifier, "Printing entropy classifier for 6 questions with 3 options.", report);
        // Now increase entropy by adding another option
        questions[0].addOption("S");
        questions[1].addOption("T");
        questions[2].addOption("U");
        questions[3].addOption("V");
        questions[4].addOption("W");
        q6.addOption("X");
        // Test LL classifier
        report = StaticAnalysis.staticAnalysis(survey, llClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(llClassifier, "Printing log likelihood classifier for 6 questions with 4 options.", report);
        // Test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey, entClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(entClassifier, "Printing entropy classifier for 6 questions with 4 options.", report);
        // Now increase entropy by adding another question
        Question q7 = new Question("7");
        q7.addOptions("Y", "Z", "a", "b");
        survey.addQuestion(q7);
        // Test LL classifier
        report = StaticAnalysis.staticAnalysis(survey, llClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(llClassifier, "Printing log likelihood classifier for 7 questions with 4 options.", report);
        // Test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey,entClassifier, granularity, RandomRespondent.AdversaryType.UNIFORM);
        writeData(entClassifier, "Printing entropy classifier for 7 questions with 4 options.", report);
    }
}

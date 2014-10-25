package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.StaticAnalysis;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

@RunWith(JUnit4.class)
public class EvaluationTest extends TestLog {

    public EvaluationTest() throws IOException, SyntaxException {
        super.init(this.getClass());
    }

    @Test
    public void testEvaluation() throws SurveyException{
        // Destination to print to
        String filename = "static_analysis_5";
        FileOutputStream pw = null;
        try {
            pw = new FileOutputStream(filename, true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        // Test each classifier for a series of surveys having increasing entropy
        Survey survey = new Survey();
        Block mainBlock = new Block("1");
        Question q1 = new Question("1");
        // TODO(etosch): if questions are all created first and options added later, the default row ordering will be wrong
        q1.addOption(new StringComponent("A", q1.getSourceRow(), Component.DEFAULT_SOURCE_COL));
        q1.addOption(new StringComponent("B", q1.getSourceRow() + 1, Component.DEFAULT_SOURCE_COL));
        Question q2 = new Question("2");
        q2.addOption(new StringComponent("C", q2.getSourceRow(), Component.DEFAULT_SOURCE_COL));
        q2.addOption(new StringComponent("D", q2.getSourceRow() + 1, Component.DEFAULT_SOURCE_COL));
        Question q3 = new Question("3");
        q3.addOption(new StringComponent("E", q3.getSourceRow(), Component.DEFAULT_SOURCE_COL));
        q3.addOption(new StringComponent("F", q3.getSourceRow() + 1, Component.DEFAULT_SOURCE_COL));
        Question q4 = new Question("4");
        q4.addOption(new StringComponent("G", q4.getSourceRow(), Component.DEFAULT_SOURCE_COL));
        q4.addOption(new StringComponent("H", q4.getSourceRow() + 1, Component.DEFAULT_SOURCE_COL));
        Question q5 = new Question("5");
        q5.addOption(new StringComponent("I", q5.getSourceRow(), Component.DEFAULT_SOURCE_COL));
        q5.addOption(new StringComponent("J", q5.getSourceRow() + 1, Component.DEFAULT_SOURCE_COL));
        mainBlock.addQuestions(q1, q2, q3, q4, q5);
        survey.addBlock(mainBlock);
        // Test LL classifier
        StaticAnalysis.Report report = StaticAnalysis.staticAnalysis(survey, Classifier.LOG_LIKELIHOOD);
        try {
            LOGGER.debug("Printing log likelihood scores for 5 questions with two options");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        // Now test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey, Classifier.ENTROPY);
        try {
            LOGGER.debug("Printing entropy classifier for 5 questions with two options.");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        // Now increase entropy by adding another option
        q1.addOption(new StringComponent("K", q1.getSourceRow() + 2, Component.DEFAULT_SOURCE_COL));
        q2.addOption(new StringComponent("L", q2.getSourceRow() + 2, Component.DEFAULT_SOURCE_COL));
        q3.addOption(new StringComponent("M", q3.getSourceRow() + 2, Component.DEFAULT_SOURCE_COL));
        q4.addOption(new StringComponent("N", q4.getSourceRow() + 2, Component.DEFAULT_SOURCE_COL));
        q5.addOption(new StringComponent("O", q5.getSourceRow() + 2, Component.DEFAULT_SOURCE_COL));
        // Test LL classifier
        report = StaticAnalysis.staticAnalysis(survey, Classifier.LOG_LIKELIHOOD);
        try {
            LOGGER.debug("Printing log likelihood classifier for 5 questions with 3 options.");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        // Test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey, Classifier.ENTROPY);
        try {
            LOGGER.debug("Printing entropy classifier for 5 questions with 3 options.");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        // Now increase entropy by adding another question
        Question q6 = new Question("6");
        q6.addOption(new StringComponent("P", q6.getSourceRow(), Component.DEFAULT_SOURCE_COL));
        q6.addOption(new StringComponent("Q", q6.getSourceRow() + 1, Component.DEFAULT_SOURCE_COL));
        q6.addOption(new StringComponent("R", q6.getSourceRow() + 2, Component.DEFAULT_SOURCE_COL));
        mainBlock.addQuestion(q6);
        survey.questions.add(q6);
        // Test LL classifier
        report = StaticAnalysis.staticAnalysis(survey, Classifier.LOG_LIKELIHOOD);
        try {
            LOGGER.debug("Printing log likelihood classifier for 6 questions with 3 options.");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        // Test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey, Classifier.ENTROPY);
        try {
            LOGGER.debug("Printing entropy classifier for 6 questions with 3 options.");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        // Now increase entropy by adding another option
        q1.addOption(new StringComponent("S", q1.getSourceRow() + 3, Component.DEFAULT_SOURCE_COL));
        q2.addOption(new StringComponent("T", q2.getSourceRow() + 3, Component.DEFAULT_SOURCE_COL));
        q3.addOption(new StringComponent("U", q3.getSourceRow() + 3, Component.DEFAULT_SOURCE_COL));
        q4.addOption(new StringComponent("V", q4.getSourceRow() + 3, Component.DEFAULT_SOURCE_COL));
        q5.addOption(new StringComponent("W", q5.getSourceRow() + 3, Component.DEFAULT_SOURCE_COL));
        q6.addOption(new StringComponent("X", q6.getSourceRow() + 3, Component.DEFAULT_SOURCE_COL));
        // Test LL classifier
        report = StaticAnalysis.staticAnalysis(survey, Classifier.LOG_LIKELIHOOD);
        try {
            LOGGER.debug("Printing log likelihood classifier for 6 questions with 4 options.");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        // Test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey, Classifier.ENTROPY);
        try {
            LOGGER.debug("Printing entropy classifier for 6 questions with 4 options.");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        // Now increase entropy by adding another question
        Question q7 = new Question("7");
        q7.addOption(new StringComponent("Y", q7.getSourceRow(), Component.DEFAULT_SOURCE_COL));
        q7.addOption(new StringComponent("Z", q7.getSourceRow() + 1, Component.DEFAULT_SOURCE_COL));
        q7.addOption(new StringComponent("a", q7.getSourceRow() + 2, Component.DEFAULT_SOURCE_COL));
        q7.addOption(new StringComponent("b", q7.getSourceRow() + 3, Component.DEFAULT_SOURCE_COL));
        mainBlock.addQuestion(q7);
        survey.questions.add(q7);
        // Test LL classifier
        report = StaticAnalysis.staticAnalysis(survey, Classifier.LOG_LIKELIHOOD);
        try {
            LOGGER.debug("Printing log likelihood classifier for 7 questions with 4 options.");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        // Test entropy classifier
        report = StaticAnalysis.staticAnalysis(survey, Classifier.ENTROPY);
        try {
            LOGGER.debug("Printing entropy classifier for 7 questions with 4 options.");
            report.print(pw);
        } catch (RuntimeException e) {
            LOGGER.fatal(e);
            System.exit(1);
        }
        
        try {
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

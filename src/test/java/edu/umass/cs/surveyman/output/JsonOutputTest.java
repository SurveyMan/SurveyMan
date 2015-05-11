package edu.umass.cs.surveyman.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.fge.jackson.JsonLoader;
import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.qc.Classifier;
import edu.umass.cs.surveyman.qc.CoefficentsAndTests;
import edu.umass.cs.surveyman.qc.RandomRespondent;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Slurpie;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;

@RunWith(JUnit4.class)
public class JsonOutputTest extends TestLog {

    public JsonOutputTest()
            throws IOException,
            SyntaxException
    {
        super.init(this.getClass());
    }

    @Test
    public void testSurvey()
            throws InvocationTargetException,
            SurveyException,
            IllegalAccessException,
            NoSuchMethodException,
            IOException
    {
        CSVLexer lexer = new CSVLexer(new StringReader(Slurpie.slurp("testCorrelation.csv")), ",");
        Survey survey = new CSVParser(lexer).parse();
        String json = survey.jsonize();
        LOGGER.info(json);
        Assert.assertFalse(json.isEmpty());
    }

    @Test
    public void testBreakoffByPositionJson()
            throws InvocationTargetException,
            SurveyException,
            IllegalAccessException,
            NoSuchMethodException,
            IOException
    {
        CSVLexer lexer = new CSVLexer(testsFiles[0], String.valueOf(separators[0]));
        BreakoffByPosition breakoffByPosition = new BreakoffByPosition(
            new CSVParser(lexer).parse()
        );
        String json = breakoffByPosition.jsonize();
        LOGGER.debug("BreakoffByPositionStruct:\t"+json);
        final JsonNode jsonObj = JsonLoader.fromString(json);
        Assert.assertEquals(jsonObj.getNodeType(), JsonNodeType.OBJECT);
        Assert.assertTrue(jsonObj.size() > 0);
        Assert.assertEquals(jsonObj.get("1").asInt(), 0);
    }

    @Test
    public void testBreakoffByQuestionJson()
            throws InvocationTargetException,
                   SurveyException,
                   IllegalAccessException,
                   NoSuchMethodException,
                   IOException
    {
        CSVLexer lexer = new CSVLexer(testsFiles[0], String.valueOf(separators[0]));
        Survey survey = new CSVParser(lexer).parse();
        BreakoffByQuestion breakoffByQuestion = new BreakoffByQuestion(survey);
        String json = breakoffByQuestion.jsonize();
        LOGGER.debug("BreakoffByQuestionStruct:\t"+json);
        final JsonNode jsonObj = JsonLoader.fromString(json);
        Assert.assertEquals(jsonObj.getNodeType(), JsonNodeType.OBJECT);
        Assert.assertEquals(jsonObj.get(survey.questions.get(0).id).asText(), "0");
    }

    @Test
    public void testClassificationStructJson()
            throws SurveyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
                   IOException
    {
        CSVLexer lexer = new CSVLexer(testsFiles[0], String.valueOf(separators[0]));
        Survey survey = new CSVParser(lexer).parse();
        SurveyResponse abstractSurveyResponse =
                new RandomRespondent(survey, RandomRespondent.AdversaryType.FIRST).getResponse();
        ClassificationStruct classificationStruct = new ClassificationStruct(
                abstractSurveyResponse,
                Classifier.ENTROPY,
                5,
                1.0,
                1.5,
                true);
        String json = classificationStruct.jsonize();
        LOGGER.debug("ClassificationStruct:\t"+json);
        final JsonNode jsonObj = JsonLoader.fromString(json);
        Assert.assertEquals(jsonObj.getNodeType(), JsonNodeType.OBJECT);
        Assert.assertEquals(jsonObj.get(classificationStruct.RESPONSEID).asText(), abstractSurveyResponse.getSrid());
    }

    @Test
    public void testClassifiedRespondentsStructJson()
            throws SurveyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
                   IOException
    {
        CSVLexer lexer = new CSVLexer(testsFiles[0], String.valueOf(separators[0]));
        Survey survey = new CSVParser(lexer).parse();
        SurveyResponse abstractSurveyResponse1 =
                new RandomRespondent(survey, RandomRespondent.AdversaryType.FIRST).getResponse();
        SurveyResponse abstractSurveyResponse2 =
                new RandomRespondent(survey, RandomRespondent.AdversaryType.LAST).getResponse();
        ClassifiedRespondentsStruct classifiedRespondentsStruct = new ClassifiedRespondentsStruct();
        classifiedRespondentsStruct.add(new ClassificationStruct(
                abstractSurveyResponse1,
                Classifier.ENTROPY,
                5,
                1.0,
                1.5,
                true));
        classifiedRespondentsStruct.add(new ClassificationStruct(
                abstractSurveyResponse2,
                Classifier.ENTROPY,
                6,
                2.3,
                2.2,
                false
        ));
        String json = classifiedRespondentsStruct.jsonize();
        LOGGER.debug("ClassifiedRespondentsStruct:\t"+json);
        final JsonNode jsonObj = JsonLoader.fromString(json);
        Assert.assertEquals(jsonObj.getNodeType(), JsonNodeType.ARRAY);
    }

    @Test
    public void testCorrelationStructJson()
            throws SurveyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
                   IOException
    {
        CorrelationStruct correlationStruct = new CorrelationStruct(
                CoefficentsAndTests.CHI,
                2.2,
                new Question("asdf"),
                new Question("fdsa"),
                12,
                21
        );
        String json = correlationStruct.jsonize();
        LOGGER.debug("CorrelationStruct:\t"+json);
        final JsonNode jsonObj = JsonLoader.fromString(json);
        Assert.assertEquals(jsonObj.getNodeType(), JsonNodeType.OBJECT);
        Assert.assertEquals(jsonObj.get(correlationStruct.COEFFICIENTTYPE).asText(), CoefficentsAndTests.CHI.name());
    }

    @Test
    public void testOrderBiasStructJson()
            throws SurveyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
                   IOException
    {
        CSVLexer lexer = new CSVLexer(testsFiles[0], String.valueOf(separators[0]));
        Survey survey = new CSVParser(lexer).parse();
        OrderBiasStruct orderBiasStruct = new OrderBiasStruct(survey, 0.01);
        String json = orderBiasStruct.jsonize();
        LOGGER.debug("OrderBiasStruct:\t"+json);
        final JsonNode jsonObj = JsonLoader.fromString(json);
        Assert.assertEquals(jsonObj.getNodeType(), JsonNodeType.OBJECT);
        Question q = survey.questions.get(0);
        Assert.assertEquals(jsonObj.get(q.id).get(q.id).asText(), "null");
    }

    @Test
    public void testWordingBiasStructJson()
            throws InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException,
                   IOException
    {
        CSVLexer lexer = new CSVLexer("src/test/resources/prototypicality.csv", ",");
        Survey survey = new CSVParser(lexer).parse();
        WordingBiasStruct wordingBiasStruct = new WordingBiasStruct(survey, 0.01);
        String json = wordingBiasStruct.jsonize();
        LOGGER.debug("WordingBiasStruct:\t" + json);
        final JsonNode jsonObj1 = JsonLoader.fromString(json);
        Assert.assertEquals(jsonObj1.getNodeType(), JsonNodeType.OBJECT);

        lexer = new CSVLexer(testsFiles[0], String.valueOf(separators[0]));
        survey = new CSVParser(lexer).parse();
        wordingBiasStruct = new WordingBiasStruct(survey, 0.01);
        json = wordingBiasStruct.jsonize();
        LOGGER.debug("WordingBiasStruct:\t" + json);
        final JsonNode jsonObj2 = JsonLoader.fromString(json);
        Assert.assertEquals(jsonObj2.getNodeType(), JsonNodeType.OBJECT);
    }

}

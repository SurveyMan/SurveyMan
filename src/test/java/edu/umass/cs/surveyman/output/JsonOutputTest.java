package edu.umass.cs.surveyman.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.fge.jackson.JsonLoader;
import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.input.csv.CSVLexer;
import edu.umass.cs.surveyman.input.csv.CSVParser;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.qc.CoefficentsAndTests;
import edu.umass.cs.surveyman.qc.QCMetrics;
import edu.umass.cs.surveyman.qc.classifiers.AbstractClassifier;
import edu.umass.cs.surveyman.qc.classifiers.AllClassifier;
import edu.umass.cs.surveyman.qc.classifiers.EntropyClassifier;
import edu.umass.cs.surveyman.qc.respondents.RandomRespondent;
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
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;


@RunWith(JUnit4.class)
public class JsonOutputTest extends TestLog {

    public JsonOutputTest()
            throws IOException,
            SyntaxException
    {
        super.LOGGER = LogManager.getLogger(JsonOutputTest.class);
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
        String surveyString = Slurpie.slurp("testCorrelation.csv");
        StringReader surveyReader = new StringReader(surveyString);
        CSVLexer lexer = new CSVLexer(surveyReader, ",");
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
        abstractSurveyResponse.setComputedValidityStatus(KnownValidityStatus.YES);
        int i = 5;
        while (i-- > 0) abstractSurveyResponse.addResponse(null);
        abstractSurveyResponse.setScore(1.0);
        abstractSurveyResponse.setThreshold(1.5);
        AbstractClassifier classifier = new EntropyClassifier(survey);
        ClassificationStruct classificationStruct = new ClassificationStruct(abstractSurveyResponse, classifier);
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
        int i = 5;
        while (i-- > 0) abstractSurveyResponse1.addResponse(null);
        abstractSurveyResponse1.setScore(1.0);
        abstractSurveyResponse1.setThreshold(1.5);
        abstractSurveyResponse1.setComputedValidityStatus(KnownValidityStatus.YES);
        classifiedRespondentsStruct.add(new ClassificationStruct(abstractSurveyResponse1, new EntropyClassifier(survey)));
        i = 6;
        while (i-- > 0) abstractSurveyResponse2.addResponse(null);
        abstractSurveyResponse2.setScore(2.3);
        abstractSurveyResponse2.setThreshold(2.2);
        abstractSurveyResponse2.setComputedValidityStatus(KnownValidityStatus.YES);
        classifiedRespondentsStruct.add(new ClassificationStruct(abstractSurveyResponse2, new EntropyClassifier(survey)));
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
                -0.0,
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
        QCMetrics qcMetrics = new QCMetrics(survey, new AllClassifier(survey));
        OrderBiasStruct orderBiasStruct = OrderBiasStruct.calculateOrderBiases(qcMetrics, new ArrayList<SurveyResponse>(), 0.01);
        String json = orderBiasStruct.jsonize();
        LOGGER.debug("OrderBiasStruct:\t"+json);
        final JsonNode jsonObj = JsonLoader.fromString(json);
        Assert.assertEquals(jsonObj.getNodeType(), JsonNodeType.OBJECT);
        Question q1 = survey.questions.get(0);
        Question q2 = survey.questions.get(1);
        final JsonNode q1json = jsonObj.get(q1.id);
        final JsonNode q2json = q1json.get(q2.id);
        Assert.assertEquals(q2json.asText(), "null");
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

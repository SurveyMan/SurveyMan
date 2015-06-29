package edu.umass.cs.surveyman.input.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Slurpie;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class to parse SurveyMan JSON input.
 */
public final class JSONParser extends AbstractParser {

    private enum TopLevelFields {
        SURVEY, FILENAME, BREAKOFF;
    }

    private static String SURVEY = "survey";
    private static String FILENAME = "filename";
    private static String BREAKOFF = "breakoff";

    /**
     * The string JSON representation of the survey.
     */
    public final String json;

    /**
     * A String indicating the survey source (e.g., filename, url).
     */
    public final String source;
    private int row = 1;
    private final int QUESTION_COL = 1;
    private final int OPTION_COL = 2;
    private Map<String, Block> internalBlockLookup = new HashMap<> ();
    private Map<String, String> internalIdMap = new HashMap<>();

    /**
     * Returns a JSONParser for some string input JSON. This constructor should be used when constructing a programmatic
     * pipeline, such as piping the JSON output of a program into the SurveyMan language and runtime system. If the data
     * is instead staged, use {@link edu.umass.cs.surveyman.input.json.JSONParser makeParser}.
     *
     * @param json The JSON representation of a survey.
     */
    public JSONParser(String json)
    {
        this.json = json;
        this.source = "";
    }

    private JSONParser(String json, String filename)
    {
        this.json = json;
        this.source = filename;
    }

    /**
     * Creates a JSONParser from a source file containing the JSON represenatation of a survey.
     *
     * @param filename The source file name for the JSON represenation of the survey.
     * @return A JSONParser instance.
     * @throws IOException
     */
    public static JSONParser makeParser(String filename)
            throws IOException
    {
        String json = Slurpie.slurp(filename);
        return new JSONParser(json, filename);
    }

    private boolean validateInput()
    {
        try {
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            String schemaString = Slurpie.slurp(INPUT_SCHEMA);
            final JsonNode jsonSchema = JsonLoader.fromString(schemaString);
            final JsonNode instance = JsonLoader.fromString(this.json);
            final JsonSchema schema = factory.getJsonSchema(jsonSchema);
            ProcessingReport report = schema.validate(instance);
            LOGGER.info(report.toString());
            return true;
        } catch (IOException io) {
            LOGGER.fatal(io.getStackTrace());
            return false;
        } catch (ProcessingException pe) {
            LOGGER.fatal(pe.getStackTrace());
            return false;
        }
    }

    private Boolean assignBool(JsonNode question, String tag, int r)
            throws SurveyException
    {
        if (question.has(tag))
            return parseBool(null, tag, question.get(tag).asText(), r, -1);
        else return defaultValues.get(tag.toUpperCase());
    }

    private boolean handleFreetext(Question question, JsonNode jsonQuestion)
    {
        if (!jsonQuestion.has(FREETEXT.toLowerCase()))
            question.freetext = defaultValues.get(FREETEXT);
        else {
            String ft = jsonQuestion.get(FREETEXT.toLowerCase()).asText();
            if (ft.toLowerCase().equals(Boolean.TRUE.toString()))
                question.freetext = true;
            else if (ft.toLowerCase().equals(Boolean.FALSE.toString()))
                question.freetext = false;
            else if (ft.startsWith("#{")){
                question.freetext = true;
                question.freetextPattern = Pattern.compile(ft.substring(2, ft.length()-1));
            } else {
                question.freetext = true;
                question.freetextDefault = ft;
            }
        }
        return question.freetext;
    }

    private SurveyDatum makeComponent(JsonNode option, int r, int index)
    {
        String data = option.get("otext").asText();
        String id = option.get("id").asText();
        SurveyDatum c;
        if (HTMLDatum.isHTMLComponent(data))
            c = new HTMLDatum(data, r, OPTION_COL, index);
        else
            c = new StringDatum(data, r, OPTION_COL, index);
        internalIdMap.put(id, c.getId());
        return c;
    }

    private Map<String, SurveyDatum> getOptions(JsonNode options, int r)
    {
        Map<String, SurveyDatum> map = new HashMap<>();
        Iterator<JsonNode> array = options.elements();
        int index = 0;
        while (array.hasNext()) {
            JsonNode arrayItem = array.next();
            SurveyDatum c = makeComponent(arrayItem, map.size() + r, index);
            map.put(c.getId(), c);
            index++;
        }
        return map;
    }

    private Question makeQuestion(Block block, JsonNode question, int r)
            throws SurveyException
    {
        String data = question.get("qtext").asText();
        Question q = Question.makeQuestion(data, r, QUESTION_COL);
        q.block = block;
        q.data = HTMLDatum.isHTMLComponent(data) ?
                new HTMLDatum(data, r, OPTION_COL, -1) :
                new StringDatum(data, r, OPTION_COL, -1);
        q.exclusive = assignBool(question, "exclusive", r);
        q.ordered = assignBool(question, "ordered", r);
        q.permitBreakoff = assignBool(question, "permitBreakoff", r);
        q.randomize = assignBool(question, "randomize", r);
        handleFreetext(q, question);
        if (question.has("options")) {
            q.options = getOptions(question.get("options"), r);
        }
        return q;
    }

    private Block makeBlock(Block parent, JsonNode jsonBlock, int nth) {

        Block b;
        String thisID;

        if (jsonBlock.has("randomize") && jsonBlock.get("randomize").asBoolean())
            thisID = "a" + nth;
        else thisID = Integer.toString(nth);

        if (parent!=null) {
            b = new Block(parent.getStrId() + "." + thisID);
            b.parentBlock = parent;
            parent.subBlocks.add(b);
        } else b = new Block(thisID);

        this.allBlockLookUp.put(b.getStrId(), b);
        this.internalBlockLookup.put(jsonBlock.get("id").asText(), b);

        if (jsonBlock.has("subblocks")) {
            Iterator<JsonNode> subblocks = jsonBlock.get("subblocks").elements();
            int i = 0;
            while (subblocks.hasNext()) {
                makeBlock(b, subblocks.next(), i + 1);
                i++;
            }
        }

        return b;
    }

    private List<Question> getQuestionsFromBlock(
            Block b,
            JsonNode block)
            throws SurveyException
    {
        List<Question> qs = new ArrayList<>();
        if (block.has("questions")){
            Iterator<JsonNode> possibleQuestions = block.get("questions").elements();
            while (possibleQuestions.hasNext()) {
                JsonNode jsonQuestion = possibleQuestions.next();
                Question q = makeQuestion(b, jsonQuestion, row);
                qs.add(q);
                row += q.options.size();
                internalIdMap.put(jsonQuestion.get("id").asText(), q.id);
            }
        }
        b.questions.addAll(qs);
        if (block.has("subblocks")) {
            Iterator<JsonNode> subblocks = block.get("subblocks").elements();
            int i = 0;
            while (subblocks.hasNext()){
                JsonNode jsonBlock = subblocks.next();
                Block bb = makeBlock(b, jsonBlock, i+1);
                qs.addAll(getQuestionsFromBlock(bb, jsonBlock));
            }
        }
        return qs;
    }

    private Question findQuestion(
            List<Question> questions,
            String quid)
    {
        for (Question question : questions) {
            if (question.id.equals(quid))
                return question;
        }
        throw new RuntimeException(String.format("Could not find question for id %s", quid));
    }

    private SurveyDatum findOption(
            Question question,
            String optionid)
            throws SurveyException
    {
        return question.getOptById(optionid);
    }

    private Block findBlock(String blockid) {
        return this.internalBlockLookup.get(blockid);
    }

    private Map<String, List<Question>> makeCorrelationMap(
            List<Question> questions,
            JsonNode correlationMap)
    {
        Map<String, List<Question>> corrMap = new HashMap<>();
        if (correlationMap == null) return corrMap;
        Iterator<Map.Entry<String, JsonNode>> fields = correlationMap.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
           String key = entry.getKey();
            corrMap.put(key, new ArrayList<Question>());
            for (JsonNode e : entry.getValue()) {
                corrMap.get(key).add(findQuestion(questions, e.asText()));
            }
        }
        return corrMap;
    }

    private void setOtherValues(
            List<Question> questions,
            Survey s,
            JsonNode jsonOtherValues)
    {
        Set<String> otherHeaders = new HashSet<>();
        Iterator<Map.Entry<String, JsonNode>> otherValues = jsonOtherValues.fields();
        while (otherValues.hasNext()) {
            Map.Entry<String, JsonNode> e = otherValues.next();
            Question q = findQuestion(questions, e.getKey());
            JsonNode map = e.getValue();
            Iterator<Map.Entry<String, JsonNode>> mapIterator = map.fields();
            while (mapIterator.hasNext()) {
                Map.Entry<String, JsonNode> ee = mapIterator.next();
                q.otherValues.put(ee.getKey(), ee.getValue().asText());
                otherHeaders.add(ee.getKey());
            }
        }
        s.otherHeaders = otherHeaders.toArray(new String[otherHeaders.size()]);
    }

    private void unifyBranching(
            Survey survey,
            JsonNode jsonBlocks)
            throws SurveyException
    {
        Iterator<JsonNode> blockArray = jsonBlocks.elements();
        while (blockArray.hasNext()) {
            JsonNode block = blockArray.next();
            if (block.has("questions")) {
                Iterator<JsonNode> questionArray = block.get("questions").elements();
                while (questionArray.hasNext()) {
                    JsonNode q = questionArray.next();
                    if (q.has("branchMap")) {
                        String jsonQuestionId = q.get("id").asText();
                        Question question = findQuestion(survey.questions, internalIdMap.get(jsonQuestionId));
                        Iterator<Map.Entry<String, JsonNode>> branchMapElements = q.get("branchMap").fields();
                        while (branchMapElements.hasNext()) {
                            Map.Entry<String, JsonNode> ee = branchMapElements.next();
                            String jsonOptionId = ee.getKey();
                            String jsonBlockId = ee.getValue().asText();
                            if (jsonBlockId.equals(Block.NEXT))
                                continue;
                            SurveyDatum opt = findOption(question, internalIdMap.get(jsonOptionId));
                            Block dest = findBlock(jsonBlockId);
                            //question.addOption(opt, dest);
                            question.setBranchDest(opt, dest);
                        }
                        if (question.block.branchQ==null) {
                            question.block.updateBranchParadigm(Block.BranchParadigm.ONE);
                            question.block.branchQ = question;
                        } else if (question.block.branchQ != question) {
                            question.block.updateBranchParadigm(Block.BranchParadigm.ALL);
                        }
                    }
                }
            }
            if (block.has("subblocks")) {
                unifyBranching(survey, block.get("subblocks"));
            }
        }
    }

    private void populateSurvey(
            Survey survey)
            throws SurveyException, IOException {

        List<Question> questions = new ArrayList<>();
        JsonNode jsonObject = JsonLoader.fromString(this.json);
        JsonNode topLevelBlocks = jsonObject.get("survey");

        Iterator<JsonNode> blockList = topLevelBlocks.elements();
        int i = 0;

        while (blockList.hasNext()) {
            JsonNode jsonBlock = blockList.next();
            Block block = makeBlock(null, jsonBlock, i+1);
            this.topLevelBlocks.add(block);
            questions.addAll(getQuestionsFromBlock(block, jsonBlock));
        }

        addPhantomBlocks(allBlockLookUp);
        Block.getSorted(this.topLevelBlocks);

        survey.questions = questions;
        survey.blocks = allBlockLookUp;
        survey.topLevelBlocks = this.topLevelBlocks;
        for (Block b : survey.topLevelBlocks)
            b.setParentPointer();

        unifyBranching(survey, topLevelBlocks);
        propagateBranchParadigms(survey);

        survey.correlationMap = makeCorrelationMap(questions, jsonObject.get("correlation"));
        survey.encoding = "UTF-8";
        survey.source = this.source;
        if (jsonObject.has("otherValues"))
            setOtherValues(questions, survey, jsonObject.get("otherValues"));
    }

    /**
     * Validates and parses the survey.
     *
     * @return A {@link edu.umass.cs.surveyman.survey.Survey} object.
     * @throws SurveyException
     */
    public Survey parse()
            throws SurveyException
    {
        validateInput();
        Survey s = new Survey();
        try {
            populateSurvey(s);
        } catch (IOException io) {
            LOGGER.fatal(io);
            throw new RuntimeException(io);
        }
        return s;
    }

}

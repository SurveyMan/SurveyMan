package edu.umass.cs.surveyman.input.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.input.exceptions.BranchException;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.utils.Slurpie;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Class to parse SurveyMan JSON input.
 */
public final class JSONParser extends AbstractParser {

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
    private Map<String, Block> internalBlockLookup = new HashMap<String,Block> ();
    private Map<String, String> internalIdMap = new HashMap<String, String>();

    /**
     * Returns a JSONParser for some string input JSON. This constructor should be used when constructing a programmatic
     * pipeline, such as piping the JSON output of a program into the SurveyMan language and runtime system. If the data
     * is instead staged, use {@link edu.umass.cs.surveyman.input.json.JSONParser makeParser}.
     *
     * @param json The JSON representation of a survey.
     */
    public JSONParser(String json) {
        this.json = json;
        this.source = "";
    }

    private JSONParser(String json, String filename){
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
    public static JSONParser makeParser(String filename) throws IOException {
        String json = Slurpie.slurp(filename);
        return new JSONParser(json, filename);
    }

    private boolean validateInput(){
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

    private Boolean assignBool(JsonObject question, String tag, int r) throws SurveyException {
        if (question.has(tag))
            return parseBool(null, tag, question.get(tag).getAsString(), r, -1);
        else return defaultValues.get(tag);
    }

    private void handleFreetext(Question question, JsonObject jsonQuestion) {
        if (!jsonQuestion.has("freetext"))
            question.freetext = defaultValues.get("freetext");
        else {
            String ft = jsonQuestion.get("freetext").getAsString();
            if (ft.toLowerCase().equals("true"))
                question.freetext = true;
            else if (ft.toLowerCase().equals("false"))
                question.freetext = false;
            else if (ft.startsWith("#{")){
                question.freetext = true;
                question.freetextPattern = Pattern.compile(ft);
            } else {
                question.freetext = true;
                question.freetextDefault = ft;
            }
        }
    }

    private Component makeComponent(JsonObject option, int r) {
        String data = option.get("otext").getAsString();
        if (HTMLComponent.isHTMLComponent(data))
            return new HTMLComponent(data, r, OPTION_COL);
        else return new StringComponent(data, r, OPTION_COL);
    }

    private Map<String, Component> getOptions(JsonArray options, int r) {
        Map<String, Component> map = new HashMap<String, Component>();
        for (JsonElement e : options) {
            Component c = makeComponent(e.getAsJsonObject(), map.size() + r);
            map.put(c.getCid(), c);
        }
        return map;
    }

    private Question makeQuestion(Block block, JsonObject question, int r) throws SurveyException {
        String data = question.get("qtext").getAsString();
        Question q = new Question(data, r, QUESTION_COL);
        q.block = block;
        q.data = HTMLComponent.isHTMLComponent(data) ? new HTMLComponent(data, r, OPTION_COL) : new StringComponent(data, r, OPTION_COL);
        q.exclusive = assignBool(question, "exclusive", r);
        q.ordered = assignBool(question, "ordered", r);
        q.permitBreakoff = assignBool(question, "permitBreakoff", r);
        q.randomize = assignBool(question, "randomize", r);
        handleFreetext(q, question);
        q.options = getOptions(question.get("options").getAsJsonArray(), r);
        return q;
    }

    private Block makeBlock(Block parent, JsonObject jsonBlock, int nth) {

        Block b;
        String thisID;

        if (jsonBlock.has("randomize") && jsonBlock.get("randomize").getAsBoolean())
            thisID = "a" + nth;
        else thisID = Integer.toString(nth);

        if (parent!=null) {
            b = new Block(parent.getStrId() + "." + thisID);
            b.parentBlock = parent;
            parent.subBlocks.add(b);
        } else b = new Block(thisID);

        this.allBlockLookUp.put(b.getStrId(), b);
        this.internalBlockLookup.put(jsonBlock.get("id").getAsString(), b);

        if (jsonBlock.has("subblocks")) {
            JsonArray subblocks = jsonBlock.get("subblocks").getAsJsonArray();
            for (int i = 0; i < subblocks.size() ; i++)
                makeBlock(b, subblocks.get(i).getAsJsonObject(), i+1);
        }

        return b;
    }

    private List<Question> getQuestionsFromBlock(Block b, JsonObject block) throws SurveyException {
        List<Question> qs = new ArrayList<Question>();
        if (block.has("questions")){
            JsonArray possibleQuestions = block.get("questions").getAsJsonArray();
            for (JsonElement e : possibleQuestions) {
                JsonObject jsonQuestion = e.getAsJsonObject();
                Question q = makeQuestion(b, jsonQuestion, row);
                qs.add(q);
                row += q.options.size();
                internalIdMap.put(jsonQuestion.get("id").getAsString(), q.quid);
            }
        }
        b.questions.addAll(qs);
        if (block.has("subblocks")) {
            JsonArray subblocks = block.get("subblocks").getAsJsonArray();
            for (int i = 0; i < subblocks.size() ; i++) {
                JsonObject jsonBlock = subblocks.get(i).getAsJsonObject();
                Block bb = makeBlock(b, jsonBlock, i+1);
                qs.addAll(getQuestionsFromBlock(bb, jsonBlock));
            }
        }
        return qs;
    }

    private Question findQuestion(List<Question> questions, String quid) {
        for (Question question : questions) {
            if (question.quid.equals(quid))
                return question;
        }
        throw new RuntimeException(String.format("Could not find question for id %s", quid));
    }

    private Map<String, List<Question>> makeCorrelationMap(List<Question> questions, JsonObject correlationMap){
        Map<String, List<Question>> corrMap = new HashMap<String, List<Question>>();
        for (Map.Entry<String, JsonElement> e : correlationMap.entrySet()) {
           String key = e.getKey();
            corrMap.put(key, new ArrayList<Question>());
            for (JsonElement ee : e.getValue().getAsJsonArray()) {
                corrMap.get(key).add(findQuestion(questions, ee.getAsString()));
            }
        }
        return corrMap;
    }

    private void setOtherValues(List<Question> questions, Survey s, JsonObject jsonOtherValues) {
        Set<String> otherHeaders = new HashSet<String>();
        for (Map.Entry<String, JsonElement> e : jsonOtherValues.entrySet()){
            Question q = findQuestion(questions, e.getKey());
            JsonObject map = e.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> ee : map.entrySet()) {
                q.otherValues.put(ee.getKey(), ee.getValue().getAsString());
                otherHeaders.add(ee.getKey());
            }
        }
        s.otherHeaders = otherHeaders.toArray(new String[otherHeaders.size()]);
    }

    private void unifyBranching(Survey survey, JsonArray jsonBlocks) throws BranchException {
        for (JsonElement e : jsonBlocks) {
            JsonObject block = e.getAsJsonObject();
            if (block.has("questions")) {
                for (JsonElement qs: block.getAsJsonArray("questions")) {
                    JsonObject q = qs.getAsJsonObject();
                    if (q.has("branchMap")) {
                        String jsonId = q.get("id").getAsString();
                        Question question = findQuestion(survey.questions, internalIdMap.get(jsonId));
                        for (Map.Entry<String, JsonElement> ee : q.get("branchMap").getAsJsonObject().entrySet())
                            question.addOption(question.options.get(ee.getKey()), this.internalBlockLookup.get(ee.getValue().getAsString()));
                        if (question.block.branchQ==null) {
                            question.block.branchParadigm = Block.BranchParadigm.ONE;
                            question.block.branchQ = question;
                        } else if (question.block.branchQ != question) {
                            question.block.branchParadigm = Block.BranchParadigm.ALL;
                        }
                    }
                }
            }
            if (block.has("subblocks")) {
                unifyBranching(survey, block.get("subblocks").getAsJsonArray());
            }
        }
    }

    private void populateSurvey(Survey survey) throws SurveyException {
        List<Question> questions = new ArrayList<Question>();
        JsonObject s = new com.google.gson.JsonParser().parse(this.json).getAsJsonObject();
        // a survey is an array of blocks
        JsonArray topLevelBlocks = s.get("survey").getAsJsonArray();

        for (int i = 0 ; i < topLevelBlocks.size() ; i++) {
            JsonObject jsonBlock = topLevelBlocks.get(i).getAsJsonObject();
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

        survey.correlationMap = makeCorrelationMap(questions, s.get("correlation").getAsJsonObject());
        survey.encoding = "UTF-8";
        survey.source = this.source;
        if (s.has("otherValues"))
            setOtherValues(questions, survey, s.get("otherValues").getAsJsonObject());
    }

    /**
     * Validates and parses the survey.
     *
     * @return A {@link edu.umass.cs.surveyman.survey.Survey} object.
     * @throws SurveyException
     */
    public Survey parse() throws SurveyException{
        validateInput();
        Survey s = new Survey();
        populateSurvey(s);
        return s;
    }

}

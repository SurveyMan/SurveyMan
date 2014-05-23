package input.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import input.AbstractParser;
import survey.*;
import input.Slurpie;
import survey.exceptions.SurveyException;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public final class JSONParser extends AbstractParser {

    public final String json;
    public final String source;
    private int row = 1;
    private final int QUESTION_COL = 1;
    private final int OPTION_COL = 2;
    private Map<String, Block> internalBlockLookup = new HashMap<String,Block> ();

    public JSONParser(String json) {
        this.json = json;
        this.source = "";
    }

    private JSONParser(String json, String filename){
        this.json = json;
        this.source = filename;
    }

    public static JSONParser makeParser(String filename) throws IOException {
        String json = Slurpie.slurp(filename);
        return new JSONParser(json, filename);
    }

    private boolean validateInput(){
        try {
            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            String schemaString = Slurpie.slurp("schemata/survey_input.json");
            final JsonNode jsonSchema = JsonLoader.fromString(schemaString);
            final JsonNode instance = JsonLoader.fromString(this.json);
            final JsonSchema schema = factory.getJsonSchema(jsonSchema);
            ProcessingReport report = schema.validate(instance);
            LOGGER.info(report.toString());
            return true;
        } catch (IOException io) {
            LOGGER.fatal(io);
            io.printStackTrace();
            return false;
        } catch (ProcessingException pe) {
            LOGGER.fatal(pe);
            pe.printStackTrace();
            return false;
        }
    }

    private Boolean assignBool(JsonObject question, String tag, int r) throws SurveyException {
        if (question.has(tag))
            return parseBool(null, tag, question.get(tag).getAsString(), r, -1, this);
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
        if (option.has("id") && option.get("id").getAsString().startsWith("comp")) {
            String id = option.get("id").getAsString();
            String[] idStuff = id.split("_");
            if (HTMLComponent.isHTMLComponent(data))
                return new HTMLComponent(data, Integer.parseInt(idStuff[1]), Integer.parseInt(idStuff[2]));
            else return new StringComponent(data, Integer.parseInt(idStuff[1]), Integer.parseInt(idStuff[2]));
        } else {
            if (HTMLComponent.isHTMLComponent(data))
                return new HTMLComponent(data, r, OPTION_COL);
            else return new StringComponent(data, r, OPTION_COL);
        }
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
        Question q = new Question(r, QUESTION_COL);
        q.block = block;
        String data = question.get("qtext").getAsString();
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
        if (jsonBlock.has("randomize"))
            thisID = "a" + nth;
        else thisID = Integer.toString(nth);
        if (parent!=null)
            b = new Block(parent.getStrId() + "." + thisID);
        else b = new Block(thisID);
        this.allBlockLookUp.put(b.getStrId(), b);
        this.internalBlockLookup.put(jsonBlock.get("id").getAsString(), b);
        return b;
    }

    private List<Question> getQuestionsFromBlock(Block b, JsonObject block) throws SurveyException{
        List<Question> qs = new ArrayList<Question>();
        if (block.has("questions")){
            JsonArray possibleQuestions = block.get("questions").getAsJsonArray();
            for (JsonElement e : possibleQuestions) {
                Question q = makeQuestion(b, e.getAsJsonObject(), row);
                qs.add(q);
                row += q.options.size();
                if (block.has("subblocks")) {
                    int i = 1;
                    for (JsonElement ee : block.get("subblocks").getAsJsonArray()) {
                        JsonObject jsonBlock = ee.getAsJsonObject();
                        Block bb = makeBlock(b, jsonBlock, i);
                        qs.addAll(getQuestionsFromBlock(bb, jsonBlock));
                        i++;
                    }
                }
            }
            return qs;
        } else return new ArrayList<Question>();
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

    private void unifyBranching(Survey survey, JsonObject jsonSurvey) {
        for (JsonElement e : jsonSurvey.get("questions").getAsJsonArray()) {
            JsonObject q = e.getAsJsonObject();
            if (q.has("branchMap")) {
                Question question = findQuestion(survey.questions, q.get("id").getAsString());
                for (Map.Entry<String, JsonElement> ee : q.get("branchMap").getAsJsonObject().entrySet())
                    question.branchMap.put(question.options.get(ee.getKey()), this.internalBlockLookup.get(ee.getValue().getAsString()));
                if (question.block.branchQ==null) {
                    question.block.branchParadigm = Block.BranchParadigm.ONE;
                    question.block.branchQ = question;
                } else if (question.block.branchQ != question) {
                    question.block.branchParadigm = Block.BranchParadigm.ALL;
                }
            }
        }
    }

    private void populateSurvey(Survey survey) throws SurveyException {
        List<Question> questions = new ArrayList<Question>();
        JsonObject s = new com.google.gson.JsonParser().parse(this.json).getAsJsonObject();
        JsonArray topLevelBlocks = s.get("survey").getAsJsonArray();
        int i = 1;
        for (JsonElement e : topLevelBlocks) {
            Block block = makeBlock(null, e.getAsJsonObject(), i);
            this.topLevelBlocks.add(block);
            JsonObject jsonBlock = e.getAsJsonObject();
            questions.addAll(getQuestionsFromBlock(block, jsonBlock));
            i++;
        }
        addPhantomBlocks(allBlockLookUp);
        Collections.sort(this.topLevelBlocks);

        survey.questions = questions;
        survey.blocks = allBlockLookUp;
        survey.topLevelBlocks = this.topLevelBlocks;
        for (Block b : survey.topLevelBlocks)
            b.setParentPointer();

        unifyBranching(survey, s);
        propagateBranchParadigms(survey);

        survey.correlationMap = makeCorrelationMap(questions, s.get("correlation").getAsJsonObject());
        survey.encoding = "UTF-8";
        survey.source = this.source;
        if (s.has("otherValues"))
            setOtherValues(questions, survey, s.get("otherValues").getAsJsonObject());
    }

    public Survey parse() throws SurveyException{
        validateInput();
        Survey s = new Survey();
        populateSurvey(s);
        s.staticAnalysis();
        return s;
    }

}

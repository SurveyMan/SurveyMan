package system.generators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import com.googlecode.htmlcompressor.compressor.ClosureJavaScriptCompressor;
import input.AbstractParser;
import input.csv.CSVParser;
import survey.*;
import org.apache.log4j.Logger;
import survey.Block;
import survey.exceptions.SurveyException;
import interstitial.Library;
import util.Slurpie;

public class JS {
    
    private static final Logger LOGGER = Logger.getLogger("system.mturk");

    private static String makeLoadPreview(Component preview) {
        return String.format(" var loadPreview = function () { $('#preview').load('%s'); }; "
                , ((HTMLComponent) preview).data);
    }

    private static String jsonizeBranchMap(Map<Component, Block> branchMap) {
        Iterator<Entry<Component, Block>> entrySet = branchMap.entrySet().iterator();
        if (!entrySet.hasNext())
            return "";
        Entry<Component, Block> entry = entrySet.next();
        String oid = entry.getKey().getCid();
        String bid = entry.getValue() == null ? "null" : "\"" + entry.getValue().getStrId()+ "\"";
        StringBuilder s = new StringBuilder(String.format("\"%s\" : %s", oid, bid));
        while (entrySet.hasNext()) {
            entry = entrySet.next();
            oid = entry.getKey().getCid();
            bid = entry.getValue() == null ? "null" : "\"" + entry.getValue().getStrId() + "\"";
            s.append(String.format(", \"%s\" : %s", oid, bid));
        }
        return "{" + s.toString() + "}";
    }

    private static String jsonizeOption(Component option) {
        return String.format("{ \"id\" : \"%s\", \"otext\" : \"%s\" }"
                , option.getCid()
                , option.toString());
    }

    private static String jsonizeOptions(List<Component> options) {
        Iterator<Component> opts = options.iterator();
        if (!opts.hasNext())
            return "";
        StringBuilder s = new StringBuilder(jsonizeOption(opts.next()));
        while (opts.hasNext()) {
            Component o = opts.next();
            s.append(String.format(", %s", jsonizeOption(o)));
        }
        return String.format("[ %s ]", s.toString());
    }

    private static String getFreetextValue(Question question) {
        if ( question.freetextDefault != null )
            return String.format("\"%s\"", question.freetextDefault);
        else if ( question.freetextPattern != null )
            return String.format("\"#{%s}\"", question.freetextPattern.pattern());
        else return "true";
    }

    private static String jsonizeQuestion(Question question) throws SurveyException {

        String options = jsonizeOptions(Arrays.asList(question.getOptListByIndex()));
        String branchMap = jsonizeBranchMap(question.branchMap);
        StringBuilder qtext = new StringBuilder();
        StringBuilder otherStuff = new StringBuilder();

        qtext.append(HTML.stringify(question.data));

        if (options.equals(""))
            otherStuff.append(question.freetext ? String.format(", \"freetext\" : %s", getFreetextValue(question)) : "");
        else otherStuff.append(String.format(", \"options\" : %s", options));

        if (!branchMap.equals(""))
            otherStuff.append(String.format(", \"branchMap\" : %s ", branchMap));

        if (question.randomize != CSVParser.defaultValues.get(AbstractParser.RANDOMIZE).booleanValue())
            otherStuff.append(String.format(", \"randomize\" : \"%s\"", question.randomize));

        if (question.ordered != CSVParser.defaultValues.get(AbstractParser.ORDERED).booleanValue())
            otherStuff.append(String.format(", \"ordered\" : \"%s\"", question.ordered));

        if (question.exclusive != CSVParser.defaultValues.get(AbstractParser.EXCLUSIVE).booleanValue())
            otherStuff.append(String.format(", \"exclusive\" : \"%s\"", question.exclusive));

        if (!question.permitBreakoff)
            otherStuff.append( ", \"breakoff\" : \"false\"");

        if (!question.correlation.equals(""))
            otherStuff.append(String.format(", \"correlation\" : \"%s\"", question.correlation));

        if (question.answer != null)
            otherStuff.append(String.format(", \"answer\" : \"%s\"", question.answer.getCid()));

        return String.format("{ \"id\" : \"%s\", \"qtext\" : \"%s\" %s}"
                , question.quid
                , qtext.toString()
                , otherStuff.toString());
    }

    private static String jsonizeQuestions(List<Question> questionList) throws SurveyException {
        Iterator<Question> qs = questionList.iterator();
        if (!qs.hasNext())
            return "[]";
        StringBuilder s = new StringBuilder(jsonizeQuestion(qs.next()));
        while (qs.hasNext()) {
            Question q = qs.next();
            s.append(String.format(", %s", jsonizeQuestion(q)));
        }
        return String.format("[ %s ]", s.toString());
    }

    private static String jsonizeBlock(Block b) throws SurveyException{
        return String.format("{ \"id\" : \"%s\", \"questions\" : %s %s %s}"
                , b.getStrId()
                , jsonizeQuestions(b.questions)
                , b.isRandomized() ? String.format(", \"randomize\" : \"%s\"", b.isRandomized()) : ""
                , b.subBlocks.size() > 0 ? String.format(", \"subblocks\" : %s", jsonizeBlocks(b.subBlocks)) : ""
        );
    }

    private static String jsonizeBlocks(List<Block> blockList) throws SurveyException {
        Iterator<Block> bs = blockList.iterator();
        StringBuilder s = new StringBuilder(jsonizeBlock(bs.next()));
        while (bs.hasNext()) {
            Block b = bs.next();
            s.append(String.format(", %s", jsonizeBlock(b)));
        }
        return String.format("[ %s ]", s.toString());
    }

    public static String jsonizeSurvey(Survey survey) throws SurveyException, ProcessingException, IOException {
        String jsonizedBlocks, json;
        if (survey.topLevelBlocks.size() > 0)
            jsonizedBlocks = jsonizeBlocks(survey.topLevelBlocks);
        else {
            Block b = new Block();
            b.questions = survey.questions;
            b.setIdArray(new int[]{1});
            List<Block> blist = new LinkedList<Block>();
            blist.add(b);
            jsonizedBlocks = jsonizeBlocks(blist);
        }
        json = String.format("{ \"filename\" : \"%s\", \"breakoff\" :  \"%s\", \"survey\" : %s } "
                , survey.source
                , Boolean.toString(survey.permitsBreakoff())
                ,  jsonizedBlocks);

        LOGGER.debug(json);

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        String stuff = Slurpie.slurp(Library.OUTPUT_SCHEMA);
        final JsonNode jsonSchema = JsonLoader.fromString(stuff);
        final JsonNode instance = JsonLoader.fromString(json);
        final JsonSchema schema = factory.getJsonSchema(jsonSchema);
        ProcessingReport report = schema.validate(instance);
        LOGGER.info(report.toString());
        return json;
        //return "var jsonizedSurvey = " + json;
    }

    private static String makeJS(Survey survey, Component preview) throws SurveyException, IOException, ProcessingException {
        String json = "var jsonizedSurvey = " + jsonizeSurvey(survey) + ";";
        String loadPreview;
        if (preview instanceof HTMLComponent)
            loadPreview = makeLoadPreview(preview);
        else loadPreview = " var loadPreview = function () {}; ";
        return String.format("%s\n%s"
                , loadPreview
                , json
        );
    }

    public static String getJSString(Survey survey, Component preview) throws SurveyException, IOException {
        String js = "";
        try {
            String temp = String.format("var customInit = function() { %s };", Slurpie.slurp(Library.JSSKELETON, true)) ;
            js = makeJS(survey, preview) + temp;
        } catch (FileNotFoundException ex) {
            LOGGER.fatal(ex);
            ex.printStackTrace();
            System.exit(-1);
        } catch (IOException ex) {
            LOGGER.fatal(ex);
            ex.printStackTrace();
            System.exit(-1);
        } catch (ProcessingException e) {
            LOGGER.fatal(e);
            e.printStackTrace();
            System.exit(-1);
        }
        return new ClosureJavaScriptCompressor().compress(js);
    };
}
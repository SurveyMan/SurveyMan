package system.generators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import csv.CSVParser;
import survey.*;
import org.apache.log4j.Logger;
import survey.Block;
import system.Slurpie;
import system.mturk.MturkLibrary;

public final class JS {
    
    private static final Logger LOGGER = Logger.getLogger("system.mturk");

    private static String makeLoadPreview(Component preview) {
        return String.format(" var loadPreview = function () { $('#preview').load('%s'); }; "
                , ((URLComponent) preview).data.toExternalForm());
    }

    private static String jsonizeBranchMap(Map<Component, Block> branchMap) {
        Iterator<Entry<Component, Block>> entrySet = branchMap.entrySet().iterator();
        if (!entrySet.hasNext())
            return "";
        Entry<Component, Block> entry = entrySet.next();
        StringBuilder s = new StringBuilder(String.format("\"%s\" : \"%s\"", entry.getKey().getCid(), entry.getValue().strId));
        while (entrySet.hasNext()) {
            entry = entrySet.next();
            s.append(String.format(", \"%s\" : \"%s\"", entry.getKey().getCid(), entry.getValue().strId));
        }
        return s.toString();
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

    private static String jsonizeQuestion(Question question) throws SurveyException {

        String options = jsonizeOptions(Arrays.asList(question.getOptListByIndex()));
        String branchMap = jsonizeBranchMap(question.branchMap);
        StringBuilder qtext = new StringBuilder();

        try {
            for (Component q : question.data) {
                qtext.append(HTML.stringify(q));
            }
        } catch (SurveyException se) {
            LOGGER.info("SurveyException thrown in jsonizeQuestion" + se);
        }

        // default values need to move out of CSVParser and into Survey
        return String.format("{ \"id\" : \"%s\", \"qtext\" : \"%s\" %s %s %s}"
                , question.quid
                , qtext
                , options.equals("") ? (question.freetext ? ", \"freetext\" : \"true\"" : "") : String.format(", \"options\" : %s", options)
                , branchMap.equals("") ? "" : String.format(", \"branchMap\" : %s ", branchMap)
                , question.randomize.equals(CSVParser.defaultValues.get(Survey.RANDOMIZE)) ? "" : String.format(", \"randomize\" : \"%s\"", question.randomize)
                , question.ordered.equals(CSVParser.defaultValues.get(Survey.ORDERED)) ? "" : String.format(", \"ordered\" : \"%s\"", question.ordered)
                , question.exclusive.equals(CSVParser.defaultValues.get(Survey.EXCLUSIVE)) ? "" : String.format(", \"exclusive\" : \"%s\"", question.exclusive)
                , question.permitBreakoff == true ? ""  : ", \"breakoff\" : \"false\""
        );
    }

    private static String jsonizeQuestions(List<Question> questionList) throws SurveyException {
        Iterator<Question> qs = questionList.iterator();
        if (!qs.hasNext())
            return "";
        StringBuilder s = new StringBuilder(jsonizeQuestion(qs.next()));
        while (qs.hasNext()) {
            Question q = qs.next();
            s.append(String.format(", %s", jsonizeQuestion(q)));
        }
        return String.format("[ %s ]", s.toString());
    }

    private static String jsonizeBlock(Block b) throws SurveyException{
        return String.format("{ \"id\" : \"%s\", \"questions\" : %s %s %s}"
                , b.strId
                , jsonizeQuestions(b.questions)
                , b.isRandomized() ? String.format(", \"randomize\" : \"%s\"", b.isRandomized()) : ""
                , b.subBlocks.size() > 0 ? String.format(", \"subblocks\" : \"%s\"", jsonizeBlocks(b.subBlocks))
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

    private static String makeJSON(Survey survey) throws SurveyException, ProcessingException, IOException {
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
        json = String.format("{ \"filename\" : \"%s\", \"breakoff\" :  %b, \"survey\" : %s }; "
                , survey.source
                , survey.permitsBreakoff()
                ,  jsonizedBlocks);
        final JsonValidator validator= JsonSchemaFactory.byDefault().getValidator();
        String surveyJsonSpec = Slurpie.slurp("survey.json");
        final JsonNode schema = JsonLoader.fromString(surveyJsonSpec);
        final JsonNode instance = JsonLoader.fromString(json);
        final ProcessingReport report = validator.validate(schema, instance);
        LOGGER.info(report.toString());
        return "var jsonizedSurvey = " + json;
    }

    private static String makeJS(Survey survey, Component preview) throws SurveyException, IOException, ProcessingException {
        String json = makeJSON(survey);
        String loadPreview;
        if (preview instanceof URLComponent)
            loadPreview = makeLoadPreview(preview);
        else loadPreview = " var loadPreview = function () {}; ";
        return String.format("%s %s"
                , loadPreview
                , json
        );
    }

    public static String getJSString(Survey survey, Component preview) throws SurveyException, IOException {
        String js = "";
        try {
            String temp = String.format("var customInit = function() { %s };", Slurpie.slurp(MturkLibrary.JSSKELETON));
            js = makeJS(survey, preview) + temp;
        } catch (FileNotFoundException ex) {
            LOGGER.fatal(ex);
            System.exit(-1);
        } catch (IOException ex) {
            LOGGER.fatal(ex);
            System.exit(-1);
        } catch (ProcessingException e) {
            LOGGER.fatal(e);
            e.printStackTrace();
            System.exit(-1);
        }
        return js;
        //return new ClosureJavaScriptCompressor().compress(js);
    };
}
package edu.umass.cs.surveyman.survey;

import clojure.reflect__init;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.github.fge.jsonschema.util.JsonLoader;
import edu.umass.cs.surveyman.input.AbstractParser;
import edu.umass.cs.surveyman.utils.Slurpie;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import edu.umass.cs.surveyman.survey.exceptions.QuestionNotFoundException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;

import java.io.IOException;
import java.util.*;

/**
 * The class representing a Survey object.
 */
public class Survey {

    // schemata
    private static final String OUTPUT_SCHEMA = "https://surveyman.github.io/Schemata/survey_output.json";
    private static final String TLBID = "1";
    private static final Logger LOGGER = LogManager.getLogger(Survey.class);
    private static final Gensym gensym = new Gensym("survey");
    /**
     * Internal survey identifier.
     */
    public String sid = gensym.next();
    /**
     * Top level list of all questions in this survey.
     */
    public List<Question> questions = new ArrayList<Question>();
    /**
     * Map of all block identifiers to {@link edu.umass.cs.surveyman.survey.Block}objects. Includes top level blocks,
     * sub-blocks, and "phantom" blocks.
     */
    public Map<String, Block> blocks = new HashMap<String, Block>();
    /**
     * List of all top-level blocks.
     */
    public List<Block> topLevelBlocks = new ArrayList<Block>();
    /**
     * Source string encoding. Typically UTF-8.
     */
    public String encoding;
    /**
     * Array of headers not contained in {@link edu.umass.cs.surveyman.input.AbstractParser knownHeaders}.
     */
    public String[] otherHeaders;
    /**
     * Name of this survey derived from the source input URL
     */
    public String sourceName;
    /**
     * Source input url.
     */
    public String source;
    /**
     * Map from correlation labels to the Questions that are correlated.
     */
    public Map<String, List<Question>> correlationMap;

    public Survey() {

    }

    public Survey(Question... surveyQuestions) throws SurveyException {
        this.addQuestions(surveyQuestions);
    }

    /**
     * Returns the {@link edu.umass.cs.surveyman.survey.Question} object associated with the input question identifier.
     * If the input identifier matches a known ad hoc custom question id, it will return a new
     * {@link edu.umass.cs.surveyman.survey.Question} object with the idenfier "q_-1_-1".
     *
     * @param quid A question identifier.
     * @return A {@link edu.umass.cs.surveyman.survey.Question} object.
     * @throws edu.umass.cs.surveyman.survey.exceptions.QuestionNotFoundException if a question with this identifier is
     * not found in this survey.
     */
    public Question getQuestionById(String quid) throws SurveyException {
        if (quid.equals("assignmentId") || quid.startsWith("start") || quid.equals(AbstractParser.CUSTOM_ID))
            return new Question("", -1, -1);
        for (Question q : questions)
            if (q.quid.equals(quid))
                return q;
        throw new QuestionNotFoundException(quid, sid);
    }

    /**
     * Returns the {@link edu.umass.cs.surveyman.survey.Question} that contains the associated input line number.
     *
     * @param lineno The input line number.
     * @return The associated {@link edu.umass.cs.surveyman.survey.Question} object.
     * @throws edu.umass.cs.surveyman.survey.exceptions.QuestionNotFoundException if none of the questions associated
     * with this survey span the input line number.
     */
    public Question getQuestionByLineNo(int lineno) throws SurveyException{
        for (Question q : questions)
            for (int ln : q.sourceLineNos)
                if (ln==lineno)
                    return q;
        throw new QuestionNotFoundException(lineno);
    }

    /**
     * Indicates whether any breakoff is permitted in this survey.
     * @return {@code true} if at least one question permits breakoff.
     */
    public boolean permitsBreakoff () {
        for (Question q : this.questions) {
            if (q.permitBreakoff)
                return true;
        }
        return false;
    }

    /**
     * If the input question belongs to a Block having {@link edu.umass.cs.surveyman.survey.Block.BranchParadigm} equal
     * to {@code ALL}, it is part of a question variant set.
     *
     * @param thisQ Input question that may belong to a variant set.
    * @return The variant set if it exists; otherwise, {@code null}.
     */
    public Set<Question> getVariantSet(Question thisQ){
        if (thisQ.block.branchParadigm.equals(Block.BranchParadigm.ALL))
            return new HashSet<Question>(thisQ.block.questions);
        return null;
    }

    /**
     * Returns the {@link org.supercsv.cellprocessor.ift.CellProcessor}s needed to parse the output of a deployed
     * SurveyMan survey. See the <a href="http://github.com/SurveyMan/Runtime">SurveyMan Runtime</a>.
     * @return Array of cell processors for parsing Runtime output CSVs.
     */
    public CellProcessor[] makeProcessorsForResponse() {

        List<CellProcessor> cells = new ArrayList<CellProcessor>(Arrays.asList(new CellProcessor[]{
                new StrRegEx("sr[0-9]+") //srid
                , null // workerid
                , null  //surveyid
                , new StrRegEx("(assignmentId)|(start_)?q_-?[0-9]+_-?[0-9]+") // quid
                , null //qtext
                , new ParseInt() //qloc
                , new StrRegEx("comp_-?[0-9]+_-?[0-9]+") //optid
                , null //opttext
                , new ParseInt() // oloc
                //, new ParseDate(dateFormat)
                //, new ParseDate(dateFormat)
        }));


        LOGGER.info(this.otherHeaders.length + " other headers");

        for (int i = 0 ; i < this.otherHeaders.length ; i++) {
            LOGGER.info("other header" + this.otherHeaders[i]);
            cells.add(null);
        }

        if (!this.correlationMap.isEmpty()) {
            LOGGER.info(this.source + " has correlations");
            cells.add(null);
        }

        return cells.toArray(new CellProcessor[cells.size()]);

    }

    /**
     * Returns the correlation label associated with the input question. If there is no correlation label assocaited
     * with the input question, returns an empty string.
     *
     * @param q The question whose correlation label we would like.
     * @return String indicating a label or empty string indicating none. Note that only one correlation label may be
     * assocaited with a particular question.
     */
    public String getCorrelationLabel(Question q) {
        for (Map.Entry<String, List<Question>> entry : correlationMap.entrySet()) {
            List<Question> qs = entry.getValue();
            if (qs.contains(q))
                return entry.getKey();
        }
        return "";
    }

    public String jsonize() throws SurveyException, ProcessingException, IOException {
        String jsonizedBlocks, json;
        if (this.topLevelBlocks.size() > 0)
            jsonizedBlocks = Block.jsonize(this.topLevelBlocks);
        else {
            Block b = new Block("");
            b.questions = this.questions;
            b.setIdArray(new int[]{1});
            List<Block> blist = new LinkedList<Block>();
            blist.add(b);
            jsonizedBlocks = Block.jsonize(blist);
        }
        json = String.format("{ \"filename\" : \"%s\", \"breakoff\" :  %s, \"survey\" : %s }"
                , this.source
                , Boolean.toString(this.permitsBreakoff())
                , jsonizedBlocks);

        LOGGER.debug(json);

        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        String stuff = Slurpie.slurp(OUTPUT_SCHEMA);
        final JsonNode jsonSchema = JsonLoader.fromString(stuff);
        final JsonNode instance = JsonLoader.fromString(json);
        final JsonSchema schema = factory.getJsonSchema(jsonSchema);
        ProcessingReport report = schema.validate(instance);
        LOGGER.info(report.toString());
        if (!report.isSuccess())
            throw new RuntimeException(report.toString());
        return json;
    }

    public void addBlock(Block b) {
        this.topLevelBlocks.add(b);
        this.blocks.put(b.getStrId(), b);
        this.questions.addAll(b.getAllQuestions());
    }

    /**
     * Adds the questions provided in the arguments to a top level block and adds this block to the survey. The default
     * top level block id is "1".
     * @param surveyQuestions The questions to be added to the top-level block of the survey.
     */
    public void addQuestions(Question... surveyQuestions) throws SurveyException {
        Block topLevelBlock;
        if (this.blocks.containsKey(TLBID))
            topLevelBlock = this.blocks.get(TLBID);
        else {
            topLevelBlock = new Block(TLBID);
            this.topLevelBlocks.add(topLevelBlock);
        }
        topLevelBlock.addQuestions(surveyQuestions);
        for (Question q: surveyQuestions) {
            if (this.questions.contains(q))
                throw new SurveyException(
                        String.format("Attempting to add question %s, which is already part of the survey.", q)){};
            else {
                q.updateFromSurvey(this);
                this.questions.add(q);
            }
        }
    }

    public void addQuestion(Question q) throws SurveyException {
        Block topLevelBlock;
        if (this.blocks.containsKey(TLBID))
            topLevelBlock = this.blocks.get(TLBID);
        else {
            topLevelBlock = new Block(TLBID);
            this.topLevelBlocks.add(topLevelBlock);
        }
        topLevelBlock.addQuestion(q);
        if (this.questions.contains(q))
            throw new SurveyException(
                    String.format("Attempting to add question %s, which is already part of the survey.", q)){};
        else {
            q.updateFromSurvey(this);
            this.questions.add(q);
        }
    }

    /**
     * A string representation of the survey is an indented illustration of the blocks and their questions.
     * @return String of survey.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("Survey id ").append(sid).append("\n");
        if (blocks.size() > 0) {
            for (Block b : blocks.values())
                str.append("\n").append(b.toString());
        } else {
            for (Question q : questions)
                str.append("\n").append(q.toString());
        }
        return str.toString();
    }

}

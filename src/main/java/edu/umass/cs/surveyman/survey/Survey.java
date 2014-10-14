package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.input.AbstractParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import edu.umass.cs.surveyman.survey.exceptions.QuestionNotFoundException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Gensym;

import java.util.*;

/**
 * The class representing a Survey object.
 */
public class Survey {

    private static final Logger LOGGER = LogManager.getLogger(Survey.class);
    private static final Gensym gensym = new Gensym("survey");
    /**
     * Internal survey identifier.
     */
    public String sid = gensym.next();
    /**
     * Top level list of all questions in this survey.
     */
    public List<Question> questions;
    /**
     * Map of all block identifiers to {@link edu.umass.cs.surveyman.survey.Block}objects. Includes top level blocks,
     * sub-blocks, and "phantom" blocks.
     */
    public Map<String, Block> blocks;
    /**
     * List of all top-level blocks.
     */
    public List<Block> topLevelBlocks;
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
            return new Question(-1, -1);
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

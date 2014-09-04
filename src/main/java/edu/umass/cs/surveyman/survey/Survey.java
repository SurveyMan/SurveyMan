package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.input.AbstractParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import edu.umass.cs.surveyman.survey.exceptions.QuestionNotFoundException;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.util.Gensym;

import java.util.*;

public class Survey {

    private static final Logger LOGGER = LogManager.getLogger(Survey.class);
    private static final Gensym gensym = new Gensym("edu/umass/cs/surveyman/survey");
    public String sid = gensym.next();
    public List<Question> questions; //top level list of questions
    public Map<String, Block> blocks;
    public List<Block> topLevelBlocks;
    public String encoding;
    public String[] otherHeaders;
    public String sourceName;
    public String source;
    public Map<String, List<Question>> correlationMap;
    public float training;
    public List<Question> trainingQuestions = new ArrayList<Question>();

    public Question getQuestionById(String quid) throws SurveyException {
        if (quid.equals("assignmentId") || quid.startsWith("start") || quid.equals(AbstractParser.CUSTOM_ID))
            return new Question(-1, -1);
        for (Question q : questions)
            if (q.quid.equals(quid))
                return q;
        throw new QuestionNotFoundException(quid, sid);
    }

    public Question getQuestionByLineNo(int lineno) throws SurveyException{
        for (Question q : questions)
            for (int ln : q.sourceLineNos)
                if (ln==lineno)
                    return q;
        throw new QuestionNotFoundException(lineno);
    }

    public boolean permitsBreakoff () {
        for (Question q : this.questions) {
            if (q.permitBreakoff)
                return true;
        }
        return false;
    }

    public Set<Question> getVariantSet(Question thisQ){
        if (thisQ.block.branchParadigm.equals(Block.BranchParadigm.ALL))
            return new HashSet<Question>(thisQ.block.questions);
        return null;
    }

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

    public String getCorrelationLabel(Question q) {
        for (Map.Entry<String, List<Question>> entry : correlationMap.entrySet()) {
            List<Question> qs = entry.getValue();
            if (qs.contains(q))
                return entry.getKey();
        }
        return "";
    }

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

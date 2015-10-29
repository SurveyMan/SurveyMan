package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.analyses.IQuestionResponse;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.survey.Block;
import edu.umass.cs.surveyman.survey.Question;

import java.util.*;

public class SurveyPath extends LinkedHashSet<Block> {

    public SurveyPath() {
        super();
    }

    public SurveyPath(Collection<Block> coll) {
        super();
        for (Block b : coll) {
            this.add(b);
        }
    }

    /**
     * Returns the set of enclosing blocks for this survey response.
     * @param r A single survey responses
     * @return The blocks the respondent has traversed in order to produce this response.
     */
    public static SurveyPath getPath(SurveyResponse r) {
        SurveyPath retval = new SurveyPath();
        for (IQuestionResponse questionResponse : r.getNonCustomResponses()) {
            Question q = questionResponse.getQuestion();
            retval.add(q.block.getFarthestContainingBlock());
        }
        return retval;
    }

    /**
     * Returns all questions in a block list (typically the topLevelBlocks of a Survey).
     * @return A list of questions.
     */
    public List<Question> getQuestionsFromPath() {
        List<Question> questions = new ArrayList<>();
        for (Block block : this) {
            if (block.getBranchParadigm() != Block.BranchParadigm.ALL)
                questions.addAll(block.questions);
            else {
                questions.add(block.questions.get(new Random().nextInt(block.questions.size())));
            }
            questions.addAll(new SurveyPath(block.subBlocks).getQuestionsFromPath());
        }
        return questions;
    }

    public int getPathLength() {
        return this.getQuestionsFromPath().size();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SurveyPath) {
            SurveyPath that = (SurveyPath) o;
            if (that.size() != this.size()) return false;
            for (Block block: this) {
                if (!that.contains(block)) {
                    return false;
                }
            }
            return true;
        } else return false;
    }
}

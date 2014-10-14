package edu.umass.cs.surveyman.analyses;

import java.util.List;
import edu.umass.cs.surveyman.survey.Question;

public interface IQuestionResponse {

    public Question getQuestion();
    public List<OptTuple> getOpts();
    public int getIndexSeen();

}

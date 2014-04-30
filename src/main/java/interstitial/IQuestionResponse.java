package interstitial;

import survey.Question;

import java.util.List;

public interface IQuestionResponse {

    public Question getQuestion();
    public List<OptTuple> getOpts();
    public int getIndexSeen();

}

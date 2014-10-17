package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.analyses.ISurveyResponse;
import edu.umass.cs.surveyman.survey.Survey;

/**
 * Simulates a cluster of responses.
 */
public class NonRandomRespondent extends AbstractRespondent {

    private Survey survey;
    private double[] qCard;

    private NonRandomRespondent(Survey survey)  {
        this.survey = survey;
        qCard = new double[survey.questions.size()];
    }


    @Override
    public ISurveyResponse getResponse() {
        return null;
    }
}
}

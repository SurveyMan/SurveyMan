package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.analyses.AbstractSurveyResponse;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.Reader;
import java.util.List;

public interface ISurveyResponseReader {
    public List<AbstractSurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException;

}

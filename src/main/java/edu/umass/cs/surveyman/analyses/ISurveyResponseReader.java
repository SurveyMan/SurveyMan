package edu.umass.cs.surveyman.analyses;

import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.io.Reader;
import java.util.List;

public interface ISurveyResponseReader {
    List<? extends SurveyResponse> readSurveyResponses(Survey s, Reader r) throws SurveyException;
}

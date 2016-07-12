package edu.umass.cs.surveyman.utils;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public interface Jsonable {

    java.lang.String jsonize() throws SurveyException;

}

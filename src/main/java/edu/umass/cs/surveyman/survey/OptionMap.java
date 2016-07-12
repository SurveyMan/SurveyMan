package edu.umass.cs.surveyman.survey;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;
import edu.umass.cs.surveyman.utils.jsonify.Jsonify;

import java.io.Serializable;
import java.util.HashMap;

public class OptionMap extends HashMap<String, SurveyDatum> implements Serializable, Jsonable {

    @Override
    public String jsonize() throws SurveyException
    {
        return Jsonify.jsonify(this);
    }
}

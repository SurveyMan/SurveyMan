package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;
import edu.umass.cs.surveyman.utils.Tabularable;

import java.util.HashMap;

abstract class BreakoffStruct<K> extends HashMap<K, Integer> implements Jsonable, Tabularable {

    abstract public void update(K k);

    abstract public java.lang.String tabularize();

    @Override
    abstract public java.lang.String jsonize() throws SurveyException;

    @Override
    abstract public java.lang.String toString();

}

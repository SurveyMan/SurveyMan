package edu.umass.cs.surveyman.utils.jsonify;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jsonify {

    static class JsonableException extends SurveyException {
        public JsonableException(Object o) {
            super("Does not implement Jsonize: " + o.getClass().getName());
        }
        public JsonableException(java.lang.String msg) {
            super(msg);
        }
    }

    public static Map<java.lang.String, Jsonable> mapify(Object... items) throws SurveyException {

        HashMap<java.lang.String, Jsonable> output = new HashMap<>();

        if (items.length % 2 != 0) {
            throw new JsonableException("Must have an even number of arguments to mapify (key-value pairs incomplete)");
        }
        for (int i = 0; i < items.length; i+=2) {
            java.lang.String key = items[i].toString();
            Object val = items[i + 1];
            if (val instanceof Jsonable) {
                output.put(key, (Jsonable) val);
            } else if (val instanceof java.lang.Boolean) {
                output.put(key, new Boolean((java.lang.Boolean) val));
            } else if (val instanceof Integer) {
                output.put(key, new Number((Integer) val));
            } else if (val instanceof Double) {
                output.put(key, new Number((Double) val));
            } else if (val instanceof java.lang.String) {
                output.put(key, new String((java.lang.String) val));
            } else throw new JsonableException(val);
        }
        return output;
    }

    public static java.lang.String jsonify(Map<java.lang.String, ? extends Jsonable> m) throws SurveyException {
        List<java.lang.String> list = new ArrayList<>();
        for (Map.Entry<java.lang.String, ? extends Jsonable> entry : m.entrySet()) {
            java.lang.String key = entry.getKey();
            Jsonable val = entry.getValue();
            if (!key.equals("")) {
                 list.add(java.lang.String.format("\"%s\" : %s", key, val.jsonize()));
            }
        }
        return java.lang.String.format("{ %s }", StringUtils.join(list, ","));
    }

}

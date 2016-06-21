package edu.umass.cs.surveyman.utils;

import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ajax.JSON;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Jsonable {

    static class JsonableException extends SurveyException {
        public JsonableException(Object o) {
            super("Does not implement Jsonable: " + o.getClass().getName());
        }
        public JsonableException(String msg) {
            super(msg);
        }
    }

    public static Map<String, Object> mapify(Object... items) throws SurveyException {
        HashMap<String,Object> output = new HashMap<>();
        if (items.length % 2 != 0) {
            throw new JsonableException("Must have an even number of arguments to mapify (key-value pairs incomplete)");
        }
        for (int i = 0; i < items.length; i+=2) {
            output.put((String) items[i], items[i+1]);
        }
        return output;
    }

    public static String jsonify(Map<String, Object> m) throws SurveyException {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Jsonable) {
                list.add(String.format("\"%s\" : \"%s\"",
                        entry.getKey(),
                        ((Jsonable) val).jsonize()));
            } else throw new JsonableException(val);
        }
        return String.format("{ %s }", StringUtils.join(list, ","));
    }

    public abstract String jsonize() throws SurveyException;
}

package edu.umass.cs.surveyman.utils;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class ArgReader {
//    arg,description,class,mandatory,default

    private static final String ARG = "arg";
    private static final String DESCRIPTION = "description";
    private static final String CLASS = "class";
    private static final String MANDATORY = "mandatory";
    private static final String DEFAULT = "default";
    private static final String CHOICES = "choices";

    private static Map<String, List<String>> arguments = new HashMap<String,List<String>>();
    private static Map<String, String> descriptions = new HashMap<String, String>();
    private static Map<String, String> mandatory = new HashMap<String,String>();
    private static Map<String, String> optional = new HashMap<String,String>();
    private static Map<String, String[]> choices = new HashMap<String, String[]>();
    private static List<String> headers;

    static {
        try {
            String contents = Slurpie.slurp("args.csv");
            CsvListReader reader = new CsvListReader(new StringReader(contents), CsvPreference.STANDARD_PREFERENCE);
            headers = Arrays.asList(reader.getHeader(true));
            List<String> cols;

            while ((cols = reader.read()) != null){
                String arg = cols.get(headers.indexOf(ARG));
                String des= cols.get(headers.indexOf(DESCRIPTION));
                String clz = cols.get(headers.indexOf(CLASS));
                String man = cols.get(headers.indexOf(MANDATORY));
                String def = cols.get(headers.indexOf(DEFAULT));
                String cho = cols.get(headers.indexOf(CHOICES));

                if (arguments.containsKey(clz))
                    arguments.get(clz).add(arg);
                else {
                    List<String> argsForClass = new ArrayList<String>();
                    argsForClass.add(arg);
                    arguments.put(clz,argsForClass);
                }
                descriptions.put(arg,des);
                if (Boolean.parseBoolean(man))
                    mandatory.put(arg, def);
                else optional.put(arg,def);
                if (cho!=null)
                    choices.put(arg, cho.split("\\|"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> intersect(String className, Map m) {
        Map<String, String> retval = new HashMap<String, String>();
        List<String> args = arguments.get(className);
        for (String arg : args)
            if (m.containsKey(arg))
                retval.put(arg, (String) m.get(arg));
        return retval;
    }

    public static Map<String, String> getMandatoryAndDefault(Class clz) {
        return intersect(clz.getName(), mandatory);
    }

    public static Map<String, String> getOptionalAndDefault(Class clz) {
        return intersect(clz.getName(), optional);
    }

    public static String[] getChoices(String arg) {
        String[] retval = choices.get(arg);
        if (retval==null)
            return new String[0];
        else return retval;
    }

    public static String getDescription(String arg){
        return descriptions.get(arg);
    }

}

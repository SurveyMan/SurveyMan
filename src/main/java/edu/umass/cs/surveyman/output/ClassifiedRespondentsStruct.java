package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import edu.umass.cs.surveyman.utils.Jsonable;
import edu.umass.cs.surveyman.utils.Tabularable;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ClassifiedRespondentsStruct extends ArrayList<ClassificationStruct> implements Jsonable, Tabularable {

    @Override
    public java.lang.String jsonize() throws SurveyException {
        List<java.lang.String> strings = new ArrayList<>();
        for (ClassificationStruct classificationStruct : this) {
            strings.add(classificationStruct.jsonize());
        }
        return java.lang.String.format("[ %s ] ", StringUtils.join(strings, ", "));
    }

    @Override
    public java.lang.String tabularize() {
        List<java.lang.String> strings = new ArrayList<>();
        for (ClassificationStruct classificationStruct : this){
            strings.add(classificationStruct.toString());
        }
        return StringUtils.join(strings, "\n");
    }

    @Override
    public java.lang.String toString() {
        int numvalid = 0;
        int n = this.size();
        for (ClassificationStruct classificationStruct : this) {
            numvalid += classificationStruct.isValid() ? 1 : 0;
        }
        return java.lang.String.format("Response classifications (%d valid, %f perc. of sample)\n" +
                "srid\tclassifiername\tnumanswered\tscore\tthreshold\tisvalid\n",
                numvalid, ((double) numvalid) / n) + tabularize();

    }

}

package system.mturk;

import utils.Slurpie;
import java.io.FileNotFoundException;
import java.io.IOException;
import survey.*;

class HTMLGenerator{
    
    private static String stringify(Component c) {
        if (c instanceof StringComponent)
            return ((StringComponent) c).data;
        else 
            return String.format("<embed src='%s' />"
                    , ((URLComponent) c).data.toExternalForm());
    }
    
    private static String stringify(Question q) {
        StringBuilder retval = new StringBuilder();
        for (Component c : q.data)
            retval.append(String.format("%s <br />\r\n"
                    , stringify(c)));
        for (Component o : q.options.values())
            retval.append(String.format("<input type='%s' name='%s' value='%s'>%s\r\n"
                    , q.exclusive?"radio":"checkbox"
                    , q.quid
                    , o.oid
                    , stringify(o)));
        retval.append("<br><input type='button' name='prev' value='Previous'>\r\n");
        retval.append("<input type='button' name='next' value='Next'>\r\n");
        retval.append("<input type='submit' name='submit' value='Submit'>");
        return retval.toString();
    }
    
    private static String stringify(Survey survey) {
        StringBuilder retval = new StringBuilder();
        for (Question q : survey.questions) 
            retval.append(String.format("<div id=question_%s>%s</div>\r\n"
                    , q.quid
                    , stringify(q)));
        return retval.toString();
    }
                    
    
    public static String getHTMLString(Survey survey) throws FileNotFoundException, IOException{
        return String.format(Slurpie.slurp(".metadata/HTMLSkeleton.html")
                , survey.encoding
                , Slurpie.slurp(".metadata/JSSkeleton.js")
                , stringify(survey));
    }

    public static void main(String[] args){
    }

}
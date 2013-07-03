package system.mturk;

import utils.Slurpie;
import java.io.FileNotFoundException;
import java.io.IOException;
import survey.*;

class HTMLGenerator{

    private static String offset2 = "\t\t";
    private static String offset3 = "\t\t\t";

    private static String stringify(Component c) {
        if (c instanceof StringComponent)
            return ((StringComponent) c).data;
        else 
            return String.format("%s<embed src='%s' />"
                    , offset2
                    , ((URLComponent) c).data.toExternalForm());
    }
    
    private static String stringify(Question q) {
        StringBuilder retval = new StringBuilder();
        for (Component c : q.data)
            retval.append(String.format("%s <br />\r\n"
                    , stringify(c)));
        for (Component o : q.options.values())
            retval.append(String.format("%s<input type='%s' name='%s' value='%s'>%s\r\n"
                    , offset3
                    , q.exclusive?"radio":"checkbox"
                    , q.quid
                    , o.cid
                    , stringify(o)));
        retval.append(offset3+"<br><input type='button' name='prev' value='Previous'>\r\n");
        retval.append(offset3+"<input type='button' name='next' value='Next'>\r\n");
        retval.append(offset3+"<input type='submit' name='submit' value='Submit'>");
        return retval.toString();
    }
    
    private static String stringify(Survey survey) {
        StringBuilder retval = new StringBuilder();
        for (Question q : survey.questions) 
            retval.append(String.format("\n%s<div id=question_%s>%s</div>\r\n"
                    , offset2
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
package system.mturk;

import survey.*;
import csv.CSVParser;
import system.Library;
import utils.Slurpie;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

class HTMLGenerator{
    private static String offset2 = "\t\t";
    private static String offset3 = "\t\t\t";
    private static String offset4 ="\t\t\t\t";
    public static final int DROPDOWN_THRESHHOLD = 7;

    private static String stringify(Component c) {
        if (c instanceof StringComponent)
            return ((StringComponent) c).data;
        else 
            return String.format("%s<embed src='%s' />"
                    , offset2
                    , ((URLComponent) c).data.toExternalForm());
    }
    
    private static String stringify(Question q) throws SurveyException {
        StringBuilder retval = new StringBuilder();
        for (Component c : q.data)
            retval.append(String.format("%s <br />\r\n"
                    , stringify(c)));
        Collection<Component> optList = Arrays.asList(q.getOptListByIndex());
        if (q.options.size() > DROPDOWN_THRESHHOLD) {
            StringBuilder options = new StringBuilder();
            for (Component o : optList) {
                options.append(String.format("%s<option value='%s'>%s</option>\r\n"
                        , offset4
                        , o.cid
                        , stringify(o)));
            }
            retval.append(String.format("%s<select %s>\r\n%s\r\n%s</select>"
                    , offset3
                    , q.exclusive?"":"multiple"
                    , options
                    , offset3));
        } else {
            for (Component o : optList) {
                retval.append(String.format("%s<input type='%s' name='%s' value='%s'>%s\r\n"
                        , offset3
                        , q.exclusive?"radio":"checkbox"
                        , q.quid
                        , o.cid
                        , stringify(o)));
            }
        }
        retval.append("<br><input type='button' name='prev' value='Previous'>");
        retval.append("<input type='button' name='next' value='Next'>");
        retval.append("<input type='submit' name='commit' value='Submit'>");
        return retval.toString();
    }
    
    private static String stringify(Survey survey) throws SurveyException {
        StringBuilder retval = new StringBuilder();
        for (Question q : survey.getQuestionsByIndex()) 
            retval.append(String.format("\n%s<div id=question_%s>%s</div>\r\n"
                    , offset2
                    , q.quid
                    , stringify(q)));
        return retval.toString();
    }
                    
    
    public static String getHTMLString(Survey survey) throws SurveyException{
        String html = "";
        try { 
            html = String.format(Slurpie.slurp(Library.HTMLSKELETON)
                    , survey.encoding
                    , Slurpie.slurp(Library.JSSKELETON)
                    , stringify(survey.splashPage)
                    , stringify(survey));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(HTMLGenerator.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.getMessage());
            System.exit(-1);
        } catch (IOException ex) {
            Logger.getLogger(HTMLGenerator.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.getMessage());
            System.exit(-1);
        }
        return html;
    }

    public static void main(String[] args) throws SurveyException, FileNotFoundException, IOException {
        String fileSep = System.getProperty("file.separator");
        Survey survey = CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", fileSep)
                , ":"
                , String.format("data%1$slinguistics%1$sconsent.html", fileSep));
        System.out.println(getHTMLString(survey));
    }

}

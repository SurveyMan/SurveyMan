package system.mturk;

import system.Library;
import utils.Slurpie;
import java.io.*;
import utils.Gensym;
import survey.Survey;
import survey.SurveyException;

public class XMLGenerator {

    private static final Gensym gensym = new Gensym("xmlsobj");
    public final String xmlid = gensym.next();
    
    public static String getXMLString(Survey survey) throws SurveyException {
        String retval;
        try {
            retval = String.format(Slurpie.slurp(Library.XMLSKELETON), HTMLGenerator.getHTMLString(survey));
        } catch (FileNotFoundException e1) {
            throw new SurveyException(e1.getMessage()){};
        } catch (IOException e2) {
            throw new SurveyException(e2.getMessage()){};
        }
        return retval;
    }
    
    public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException, IOException, SurveyException{
        String fileSep = System.getProperty("file.separator");
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        Survey survey = csv.CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", fileSep), ":");
        out.println(getXMLString(survey));
    }
}

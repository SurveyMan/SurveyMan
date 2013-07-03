package system.mturk;

import utils.Slurpie;
import java.io.*;
import utils.Gensym;
import survey.Survey;

public class XMLGenerator {

    private static final Gensym gensym = new Gensym("xmlsobj");
    public final String xmlid = gensym.next();
    
    public static String getXMLString(Survey survey) throws FileNotFoundException, IOException {
        return String.format(Slurpie.slurp(".metadata/XMLSkeleton.xml")
                , HTMLGenerator.getHTMLString(survey));
    }
    
    public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException, IOException{
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        Survey survey = csv.CSVParser.parse("data/linguistics/test3.csv", ":");
        out.println(getXMLString(survey));
    }
}

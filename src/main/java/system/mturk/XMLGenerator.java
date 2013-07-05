package system.mturk;

import system.Library;
import utils.Slurpie;
import java.io.*;
import utils.Gensym;
import survey.Survey;

public class XMLGenerator {

    private static final Gensym gensym = new Gensym("xmlsobj");
    public final String xmlid = gensym.next();
    
    public static String getXMLString(Survey survey) throws FileNotFoundException, IOException {
        return String.format(Slurpie.slurp(Library.XMLSKELETON), HTMLGenerator.getHTMLString(survey));
    }
    
    public static void main(String[] args) throws UnsupportedEncodingException, FileNotFoundException, IOException{
        String fileSep = System.getProperty("line.separator");
        PrintStream out = new PrintStream(System.out, true, "UTF-8");
        Survey survey = csv.CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", fileSep), ":");
        out.println(getXMLString(survey));
    }
}

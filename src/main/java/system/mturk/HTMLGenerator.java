package system.mturk;

import csv.CSVLexer;
import survey.*;
import system.Library;
import utils.Gensym;
import utils.Slurpie;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import org.apache.log4j.Logger;

public class HTMLGenerator{

    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    private static Gensym gensym = new Gensym("none");
    private static String offset2 = "\t\t";
    private static String offset3 = "\t\t\t";
    private static String offset4 ="\t\t\t\t";
    public static final int DROPDOWN_THRESHHOLD = 7;
    public static String htmlFileName = "";

    private static String stringify(Component c) {
        if (c instanceof StringComponent)
            return CSVLexer.xmlChars2HTML(((StringComponent) c).data);
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
                options.append(String.format("%1$s<option value='%2$s' id='%2$s' onchange='showNext(\"%3$s\")'>%4$s</option>\r\n"
                        , offset4
                        , o.cid
                        , q.quid
                        , stringify(o)
                ));
            }
            retval.append(String.format("%s<select %s onchange='showNext(\"%s\")'>\r\n%s\r\n%s</select>"
                    , offset3
                    , q.exclusive?"":"multiple"
                    , q.quid
                    , options
                    , offset3
            ));
        } else {
            for (Component o : optList) {
                retval.append(String.format("%1$s<br><input type='%2$s' name='%3$s' value='%4$s' id='%4$s' onclick='showNext(\"%3$s\")'>%5$s\r\n"
                        , offset3
                        , q.exclusive?"radio":"checkbox"
                        , q.quid
                        , o.cid
                        , stringify(o)
                ));
            }
        }
        boolean skip = MturkLibrary.props.getProperty("canskip", "").equals("true");
        retval.append(String.format("<br><input type='button' name='prev' value='Previous' id='prev_%s' %s>", gensym.next(), skip?"":"hidden"));
        retval.append(String.format("<input type='button' name='next' value='Next' id='next_%s' %s>", skip ? gensym.next() : q.quid, skip ? "" : "hidden"));
        retval.append(String.format("<input type='submit' name='commit' value='Submit' id='submit_%s' %s>", skip? gensym.next() :q.quid, skip?"":"hidden"));
        return retval.toString();
    }
    
    private static String stringify(Survey survey) throws SurveyException {
        StringBuilder retval = new StringBuilder();
        for (Question q : survey.getQuestionsByIndex()) 
            retval.append(String.format("\n%s<div name='question' id='%s'>%s</div>\r\n"
                    , offset2
                    , q.quid
                    , stringify(q)));
        return retval.toString();
    }

    public static void spitHTMLToFile(String html, Survey survey) throws IOException {
        htmlFileName = String.format("%s%slogs%s%s_%s_%s.html"
                , (new File("")).getAbsolutePath()
                , Library.fileSep
                , Library.fileSep
                , survey.sourceName
                , survey.sid
                , Library.TIME);
        BufferedWriter bw = new BufferedWriter(new FileWriter(htmlFileName));
        bw.write(html);
        bw.close();
    }
    
    public static String getHTMLString(Survey survey) throws SurveyException{
        String html = "";
        try {
            html = String.format(Slurpie.slurp(MturkLibrary.HTMLSKELETON)
                    , survey.encoding
                    , JSGenerator.getJSString(survey)
                    , MturkLibrary.props.getProperty("splashpage", "")
                    , stringify(survey)
                    , MturkLibrary.EXTERNAL_HIT);
        } catch (FileNotFoundException ex) {
            LOGGER.fatal(ex);
            System.exit(-1);
        } catch (IOException ex) {
            LOGGER.fatal(ex);
            System.exit(-1);
        }
        try{
            spitHTMLToFile(html, survey);
        } catch (IOException io) {
            LOGGER.warn(io);
        }
        return html;
    }
}

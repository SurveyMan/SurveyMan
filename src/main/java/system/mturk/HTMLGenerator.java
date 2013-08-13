package system.mturk;

import csv.CSVLexer;
import org.apache.log4j.FileAppender;
import survey.*;
import csv.CSVParser;
import utils.Gensym;
import utils.Slurpie;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.apache.log4j.Logger;

class HTMLGenerator{

    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    private static Gensym gensym = new Gensym("none");
    private static String offset2 = "\t\t";
    private static String offset3 = "\t\t\t";
    private static String offset4 ="\t\t\t\t";
    public static final int DROPDOWN_THRESHHOLD = 7;

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
                options.append(String.format("%s<option value='%s' onchange='showNext(\"%s\")'>%s</option>\r\n"
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
                retval.append(String.format("%s<br><input type='%s' name='%s' value='%s' onclick='showNext(\"%s\")'>%s\r\n"
                        , offset3
                        , q.exclusive?"radio":"checkbox"
                        , q.quid
                        , o.cid
                        , q.quid
                        , stringify(o)
                ));
            }
        }
        boolean skip = MturkLibrary.props.getProperty("canskip").equals("true");
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

    public static void spitHTMLToFile(String html, String sid) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(String.format("logs/survey_%s_%d.html", sid, System.currentTimeMillis())));
        bw.write(html);
        bw.close();
    }
    
    public static String getHTMLString(Survey survey) throws SurveyException{
        String html = "";
        try {
            html = String.format(Slurpie.slurp(MturkLibrary.HTMLSKELETON)
                    , survey.encoding
                    , Slurpie.slurp(MturkLibrary.JSSKELETON)
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
            spitHTMLToFile(html, survey.sid);
        } catch (IOException io) {
            LOGGER.warn(io);
        }
        return html;
    }

    public static void main(String[] args) throws SurveyException, FileNotFoundException, IOException {
        String fileSep = System.getProperty("file.separator");
        Survey survey = CSVParser.parse(String.format("data%1$slinguistics%1$stest3.csv", fileSep), ":");
        System.out.println(getHTMLString(survey));
    }

}

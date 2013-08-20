package system.mturk;

import csv.CSVLexer;
import survey.*;
import system.Library;
import utils.Gensym;
import utils.Slurpie;

import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

public class HTMLGenerator{

    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    private static Gensym gensym = new Gensym("none");
    public static final int DROPDOWN_THRESHHOLD = 7;
    public static String htmlFileName = "";
    public static final String[] VIDEO = {"ogv", "ogg", "mp4"};
    public static final String[] AUDIO = {"oga", "wav", "mp3"};

    private static String getMediaTag(String ext) {
        if (Arrays.asList(VIDEO).contains(ext))
            return "video";
        else if (Arrays.asList(AUDIO).contains(ext))
            return "audio";
        else return "";
    }

    private static String stringify(Component c) throws SurveyException{
        if (c instanceof StringComponent)
            return CSVLexer.xmlChars2HTML(((StringComponent) c).data);
        else {
            String url = ((URLComponent) c).data.toExternalForm();
            String ext = url.substring(url.lastIndexOf(".")+1);
            String tag = getMediaTag(ext);
            if (tag.equals(""))
                return String.format("<embed src='%s' id='%s'>", url, c.cid);
            else return String.format("<%1$s controls preload='none' src='%2$s' type='%1$s/%3$s' id='%4$s'></%1$s>", tag, url, ext, c.cid);
        }
    }

    private static String getFreetextString(Collection<Component> optList) throws SurveyException{
        if (optList.isEmpty())
            return String.format("<input type='text' name='Response:' id='%s'>", gensym.next());
        else {
            StringBuilder retval = new StringBuilder();
            for (Component o : optList)
                retval.append(String.format("<input type='text' name='Response:' value='%s' id='%s'>", stringify(o), o.cid));
            return retval.toString();
        }
    }

    private static String getDropdownString(Collection<Component> optList, Question q) throws SurveyException{
        StringBuilder options = new StringBuilder("<option>CHOOSE ONE</option>");
        for (Component o : optList) {
            options.append(String.format("<option value='%1$s' id='%1$s'>%2$s</option>\r\n"
                    , o.cid
                    , stringify(o)
            ));
        }
        return String.format("<select %1$s id='select_%3$s' onchange='showNext(\"%3$s\", getDropdownOpt(\"%3$s\"))'>%2$s</select>"
                , q.exclusive ? "" : "multiple" //%1$s
                , options //%2$s
                , q.quid);
    }

    private static String getRadioOrCheckboxString(Collection<Component> optList, Question q) throws SurveyException{
        StringBuilder retval = new StringBuilder();
        for (Component o : optList) {
            retval.append(String.format("<input type='%1$s' name='%2$s' value='%3$s' id='%3$s' onclick='showNext(\"%2$s\", \"%3$s\")'>%4$s\r\n"
                    , q.exclusive?"radio":"checkbox"
                    , q.quid
                    , o.cid
                    , stringify(o)
            ));
        }
        return retval.toString();
    }

    private static String stringify(Question q) throws SurveyException, MalformedURLException {
        StringBuilder retval = new StringBuilder();
        for (Component c : q.data)
            retval.append(String.format("%s <br />\r\n"
                    , stringify(c)));
        Collection<Component> optList = Arrays.asList(q.getOptListByIndex());
        if (q.freetext) {
            retval.append(getFreetextString(optList));
        } else if (q.options.size() > DROPDOWN_THRESHHOLD) {
            retval.append(getDropdownString(optList, q));
        } else {
            retval.append(getRadioOrCheckboxString(optList, q));
        }
        boolean skip = MturkLibrary.props.getProperty("canskip", "").equals("true");
        retval.append(String.format("<br><input type='button' value='Prev' id='prev_%1$s' onclick='showPrevQuestion(\"%1$s\")' %2$s>", q.quid, skip?"":"hidden"));
        retval.append(String.format("<input type='button' value='Next' id='next_%1$s' %2$s>"
                , q.quid
                , (skip || q.freetext || !(q.freetext || q.exclusive || q.ordered || q.perturb )) ?
                        String.format("onclick='showNextQuestion(\"%s\")'", q.quid) :
                        "hidden"));
        if (!skip) retval.append(String.format("<input type='submit' id='submit_%s'>", q.quid));
        return retval.toString();
    }
    
    private static String stringify(Survey survey) throws SurveyException, MalformedURLException {
        StringBuilder retval = new StringBuilder();
        Question[] questions = survey.getQuestionsByIndex();
        for (int i = 0; i < questions.length; i++)
            retval.append(String.format("<div name='question' id='%s'>%s%s</div>"
                    , questions[i].quid
                    , stringify(questions[i])
                    , (MturkLibrary.props.getProperty("canskip","").equals("true") && i==questions.length-1) ?
                        String.format("<input type='submit' id='submit_%s'>", questions[i].quid) : ""));
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
        return (new HtmlCompressor()).compress(html);
    }
}

class UnknownMediaExtension extends SurveyException {
    public UnknownMediaExtension(String msg){
        super(String.format("Unknown media extension (%s).", msg));
    }
}
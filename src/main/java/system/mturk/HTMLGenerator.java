package system.mturk;

import csv.CSVLexer;
import csv.CSVParser;
import survey.*;
import system.Library;
import utils.Gensym;
import utils.Slurpie;

import java.io.*;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;

import org.apache.log4j.Logger;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

public class HTMLGenerator{

    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    private static Gensym gensym = new Gensym("none");
    public static final int DROPDOWN_THRESHHOLD = 7;
    public static String htmlFileName = "";
    public static final String[] IMAGE = {"jpg"};
    public static final String[] VIDEO = {"ogv", "ogg", "mp4"};
    public static final String[] AUDIO = {"oga", "wav", "mp3"};
    public static final String[] PAGE = {"html", "htm"};

    private static String getMediaTag(String ext) {
        if (Arrays.asList(VIDEO).contains(ext))
            return "video";
        else if (Arrays.asList(AUDIO).contains(ext))
            return "audio";
        else if (Arrays.asList(PAGE).contains(ext))
            return "page";
        else if (Arrays.asList(IMAGE).contains(ext))
            return "image";
        else return "";
    }

    protected static String stringify(Component c) throws SurveyException{
        if (c instanceof StringComponent)
            return CSVLexer.xmlChars2HTML(((StringComponent) c).data);
        else {
            String url = ((URLComponent) c).data.toExternalForm();
            String ext = url.substring(url.lastIndexOf(".")+1);
            String tag = getMediaTag(ext);
            if (tag.equals(""))
                return String.format("<embed src=\"%s\" id=\"%s\">", url, c.cid);
            else if (tag.equals("page"))
                return "";
            else if (tag.equals("image"))
                return String.format("<img src=\"%s\" id=\"%s\" />", url, c.cid);
            else return String.format("<%1$s controls preload=\"none\" src=\"%2$s\" type=\"%1$s/%3$s\" id=\"%4$s\"></%1$s>", tag, url, ext, c.cid);
        }
    }

    private static String stringify(Question q) throws SurveyException, MalformedURLException {
        StringBuilder retval = new StringBuilder();
        for (Component c : q.data)
            retval.append(String.format("%s <br />\r\n"
                    , stringify(c)));
        retval.append("<p></p>");
        boolean skip = MturkLibrary.props.getProperty("canskip", "").equals("true");
        retval.append(String.format("<br><input type=\"button\" value=\"Prev\" id=\"prev_%1$s\" onclick=\"showPrevQuestion('%1$s')\" %2$s>", q.quid, skip?"":"hidden"));
        retval.append(String.format("<input type=\"button\" value=\"Next\" id=\"next_%1$s\" %2$s>"
                , q.quid
                , (skip || q.freetext || !(q.freetext || q.exclusive || q.ordered || q.perturb )) ?
                        String.format("onclick=\"showNextQuestion('%s')\"", q.quid) :
                            "hidden"));
        if (!skip) retval.append(String.format("<input type=\"submit\" id=\"submit_%s\">", q.quid));
        return retval.toString();
    }
    
    private static String stringify(Survey survey) throws SurveyException, MalformedURLException {
        StringBuilder retval = new StringBuilder();
        Question[] questions = survey.getQuestionsByIndex();
        for (int i = 0; i < questions.length; i++)
            retval.append(String.format("<div name=\"question\" id=\"%s\">%s%s</div>"
                    , questions[i].quid
                    , stringify(questions[i])
                    , (MturkLibrary.props.getProperty("canskip","").equals("true") && i==questions.length-1) ?
                        String.format("<input type=\"submit\" id=\"submit_%s\">", questions[i].quid) : ""));
        return retval.toString();
    }

    private static String stringifyPreview(Component c) throws SurveyException {
        String baseString = stringify(c);
        return String.format("<div id=\"preview\" %s>%s</div>"
                , (c instanceof URLComponent) ? String.format("onload=\"loadPreview();\""
                                                , "#preview"
                                                , ((URLComponent) c).data.toExternalForm())
                                              : ""
                , (c instanceof StringComponent) ? CSVLexer.htmlChars2XML(baseString) : "");
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
            Component preview = CSVParser.parseComponent(CSVParser.stripQuots(MturkLibrary.props.getProperty("splashpage", "").trim()).trim());
            html = String.format(Slurpie.slurp(MturkLibrary.HTMLSKELETON)
                    , survey.encoding
                    , JSGenerator.getJSString(survey, preview)
                    , stringifyPreview(preview)
                    , stringify(survey)
                    , MturkLibrary.EXTERNAL_HIT);
            System.out.println(html.substring(2836, 2844));
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
        //return (new HtmlCompressor()).compress(html);
    }
}

class UnknownMediaExtension extends SurveyException {
    public UnknownMediaExtension(String msg){
        super(String.format("Unknown media extension (%s).", msg));
    }
}
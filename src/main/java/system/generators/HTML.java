package system.generators;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import csv.CSVLexer;
import csv.CSVParser;
import org.apache.log4j.Logger;
import survey.*;
import system.BackendType;
import system.Library;
import system.Slurpie;
import system.Record;
import system.interfaces.ResponseManager;
import system.mturk.MturkResponseManager;

import java.io.*;
import java.net.MalformedURLException;
import java.util.Arrays;

public class HTML {


    static class UnknownMediaExtension extends SurveyException {
        public UnknownMediaExtension(String msg){
            super(String.format("Unknown media extension (%s).", msg));
        }
    }

    private static final Logger LOGGER = Logger.getLogger(HTML.class);
    public static final String[] IMAGE = {"jpg", "jpeg", "png"};
    public static final String[] VIDEO = {"ogv", "ogg", "mp4"};
    public static final String[] AUDIO = {"oga", "wav", "mp3"};
    public static final String[] PAGE = {"html", "htm"};

    private static String getMediaTag(String ext) {
        ext = ext.toLowerCase();
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

    protected static String stringify(Component c) throws SurveyException {
        if (c instanceof StringComponent)
            return CSVLexer.xmlChars2HTML(((StringComponent) c).data);
        else {
            String url = CSVLexer.xmlChars2HTML(((URLComponent) c).data.toExternalForm());
            String ext = url.substring(url.lastIndexOf(".")+1);
            String tag = getMediaTag(ext);
            if (tag.equals(""))
                return String.format("<embed src='%s' id='%s'>", url, c.getCid());
            else if (tag.equals("page"))
                return "";
            else if (tag.equals("image"))
                return String.format("<img src='%s' id='%s' />", url, c.getCid());
            else return String.format("<%1$s controls preload='none' src='%2$s' type='%1$s/%3$s' id'%4$s'></%1$s>", tag, url, ext, c.getCid());
        }
    }

    private static String stringify() throws SurveyException, MalformedURLException {
        return "<div name=\"question\" hidden>"
                    + "<p class=\"question\"></p>"
                    + "<p class=\"answer\"></p>"
                    + "</div>";
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

    public static void spitHTMLToFile(String html, Survey survey)
            throws IOException, SurveyException, InstantiationException, IllegalAccessException {

        Record r;
        synchronized (MturkResponseManager.manager) {
            if (MturkResponseManager.manager.containsKey(survey.sid))
                r = MturkResponseManager.manager.get(survey.sid);
            else {
                LOGGER.info(String.format("Record for %s (%s) not found in manager; creating new record.", survey.sourceName, survey.sid));
                r = new Record(survey, new Library(), BackendType.LOCALHOST);
                MturkResponseManager.manager.put(survey.sid, r);
            }
        }
        LOGGER.info(String.format("Source html found at %s", r.getHtmlFileName()));
        BufferedWriter bw = new BufferedWriter(new FileWriter(r.getHtmlFileName()));
        bw.write(html);
        bw.close();

    }

    public static String getHTMLString(Survey survey, system.interfaces.HTML backendHTML) throws SurveyException {
        String html = "";
        try {
            if (ResponseManager.getRecord(survey)==null)
                ResponseManager.manager.put(survey.sid, new Record(survey, new Library(), BackendType.LOCALHOST));
            Record record = ResponseManager.getRecord(survey);
            assert(record!=null);
            assert(record.library!=null);
            assert(record.library.props!=null);
            Component preview = CSVParser.parseComponent(record.library.props.getProperty("splashpage", ""), -1, -1);
            System.out.println(backendHTML.getClass().getName());
            html = String.format(Slurpie.slurp(Library.HTMLSKELETON)
                    , survey.encoding
                    , JS.getJSString(survey, preview)
                    , stringifyPreview(preview)
                    , stringify()
                    , backendHTML.getActionForm(record)
                    , survey.source
                    , record.outputFileName
                    , backendHTML.getHTMLString()
            );
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
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return (new HtmlCompressor()).compress(html);
    }
}
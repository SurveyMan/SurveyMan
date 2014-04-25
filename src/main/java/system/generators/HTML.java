package system.generators;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import input.AbstractLexer;
import input.AbstractParser;
import input.csv.CSVLexer;
import org.apache.log4j.Logger;
import survey.*;
import system.BackendType;
import system.Library;
import system.Slurpie;
import system.Record;
import system.interfaces.AbstractResponseManager;
import system.interfaces.IHTML;

import java.io.*;
import java.net.MalformedURLException;

public class HTML {

    private static final Logger LOGGER = Logger.getLogger(HTML.class);

    protected static String stringify(Component c) throws SurveyException {
        if (c instanceof StringComponent)
            return CSVLexer.xmlChars2HTML(((StringComponent) c).data).replace("\"", "&quot;");
        else {
            String data = ((HTMLComponent) c).data;
            return data.replace("\"", "&quot;");
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
                , ((c instanceof HTMLComponent) ? "onload=\"loadPreview();\""
                                              : "")
                , ((c instanceof StringComponent) ? CSVLexer.htmlChars2XML(baseString) : ""));
    }

    public static void spitHTMLToFile(String html, Survey survey)
            throws IOException, SurveyException, InstantiationException, IllegalAccessException {

        Record r;
        if (AbstractResponseManager.existsRecordForSurvey(survey))
            r = AbstractResponseManager.getRecord(survey);
        else {
            LOGGER.info(String.format("Record for %s (%s) not found in manager; creating new record.", survey.sourceName, survey.sid));
            AbstractResponseManager.putRecord(survey, new Library(survey), BackendType.LOCALHOST);
            r = AbstractResponseManager.getRecord(survey);
        }
        LOGGER.info(String.format("Source html found at %s", r.getHtmlFileName()));
        BufferedWriter bw = new BufferedWriter(new FileWriter(r.getHtmlFileName()));
        bw.write(html);
        bw.close();

    }

    public static String getHTMLString(Survey survey, IHTML backendHTML) throws SurveyException {
        String html = "";
        try {
            if (AbstractResponseManager.getRecord(survey)==null)
                AbstractResponseManager.putRecord(survey, new Library(survey), BackendType.LOCALHOST);
            Record record = AbstractResponseManager.getRecord(survey);
            assert(record!=null);
            assert(record.library!=null);
            assert(record.library.props!=null);
            String strPreview = record.library.props.getProperty("splashpage", "");
            Component preview = AbstractParser.parseComponent(HTMLComponent.isHTMLComponent(strPreview) ? AbstractLexer.xmlChars2HTML(strPreview) : strPreview, -1, -1);
            html = String.format(Slurpie.slurp(Library.HTMLSKELETON)
                    , survey.encoding
                    , JS.getJSString(survey, preview)
                    , stringifyPreview(preview)
                    , stringify()
                    , backendHTML.getActionForm(record)
                    , survey.source
                    , record.outputFileName
                    , backendHTML.getHTMLString()
                    , Slurpie.slurp(Library.CUSTOMCSS, true)
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
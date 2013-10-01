package system.mturk.generators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import scala.Tuple2;
import survey.*;
import system.mturk.MturkLibrary;
import system.mturk.generators.HTML;
import utils.Slurpie;
import org.apache.log4j.Logger;
import survey.Block;

public class JS {
    
    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    
    private static String makeBranchTable(Survey survey) {
        ArrayList<Tuple2<String, String>> entries = new ArrayList<Tuple2<String, String>>();
        for (Question q : survey.questions) {
            if (q.branchMap.size() > 0) {
                for (Entry<Component, Block> entry : q.branchMap.entrySet()) {
                    // get the id of the first question in the block we need to jump to
                    String quid = entry.getValue().questions.get(0).quid;
                    String oid = entry.getKey().cid;
                    entries.add(new Tuple2<String, String>(oid, "\""+quid+"\""));
                }
            }
        }
        if (entries.isEmpty())
            return " var branchTable = {}; ";
        else {
            StringBuilder table = new StringBuilder(String.format("%s : %s", entries.get(0)._1(), entries.get(0)._2()));
            for (Tuple2<String, String> entry : entries.subList(1,entries.size()))
                table.append(String.format(", %s : %s", entry._1(), entry._2()));
            return String.format(" var branchTable = { %s }; ", table.toString());
        }
    }

    private static String makeLoadPreview(Component preview) {
        return String.format(" var loadPreview = function () { $('#preview').load('%s'); }; "
                , ((URLComponent) preview).data.toExternalForm());
    }

    private static String getQType(Question q) {
        if (q.options.size()==0)
            return "none";
        else if (q.freetext)
            return "text";
        else if (q.exclusive)
            return "radio";
        else return "checkbox";
    }

    private static String makeQOptions(Question q) throws SurveyException {
        StringBuilder s = new StringBuilder();
        for (Component c : q.getOptListByIndex())
            s.append(String.format("%s { 'text' : \"%s\", 'value' : '%s' }"
                    , c.index==0 ? "" : ","
                    , HTML.stringify(c)
                    , c.cid
                    ));
        return String.format("{ 'input' : '%s', 'data' : [ %s ] }"
                , getQType(q)
                , s.toString());
    }

    private static String makeOptionTable(Survey survey) throws SurveyException{
        StringBuilder s = new StringBuilder();
        for (Question q : survey.getQuestionsByIndex()) {
            s.append(String.format("%s '%s' : %s"
                    , q.index==0 ? "" : ","
                    , q.quid
                    , makeQOptions(q)
            ));
        }
        return String.format(" var oTable = { %s }; ", s.toString());
    }

    private static String displayQ() {
        return "";
    }

    private static String makeJS(Survey survey, Component preview) throws SurveyException {
        String branchTable = makeBranchTable(survey);
        String oTable = makeOptionTable(survey);
        String loadPreview = "";
        if (preview instanceof URLComponent)
            loadPreview = makeLoadPreview(preview);
        else loadPreview = " var loadPreview = function () {}; ";
        return branchTable + " " + loadPreview + " " + oTable;
    }

    public static String getJSString(Survey survey, Component preview) throws SurveyException{
        String js = "";
        try {
            js = String.format(Slurpie.slurp(MturkLibrary.JSSKELETON)
                    , makeJS(survey, preview));
        } catch (FileNotFoundException ex) {
            LOGGER.fatal(ex);
            System.exit(-1);
        } catch (IOException ex) {
            LOGGER.fatal(ex);
            System.exit(-1);
        }
        return js;
        //return new ClosureJavaScriptCompressor().compress(js);
    };
}
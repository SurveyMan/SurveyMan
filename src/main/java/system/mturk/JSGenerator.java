package system.mturk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import scala.Tuple2;
import survey.*;
import utils.Slurpie;
import org.apache.log4j.Logger;
import survey.Block;
import com.googlecode.htmlcompressor.compressor.ClosureJavaScriptCompressor;

public class JSGenerator{
    
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

    private static String makeJS(Survey survey, Component preview) {
        String branchTable = makeBranchTable(survey);
        String loadPreview = "";
        if (preview instanceof URLComponent)
            loadPreview = makeLoadPreview(preview);
        else loadPreview = " var loadPreview = function () {}; ";
        return branchTable + " " + loadPreview;
    }

    public static String getJSString(Survey survey, Component preview) {
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
        return new ClosureJavaScriptCompressor().compress(js);
    }
}
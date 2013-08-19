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
            return "";
        else {
            StringBuilder table = new StringBuilder(String.format("%s : %s", entries.get(0)._1(), entries.get(0)._2()));
            for (Tuple2<String, String> entry : entries.subList(1,entries.size()))
                table.append(String.format(", %s : %s", entry._1(), entry._2()));
            return String.format("var branchTable = { %s };\r\n", table.toString());
        }
    }


    
    private static String makeGetNextQuestionID(boolean branch) {
        // if the option chosen is in the branch table, 
        // jump to the first question in the corresponding block
        return String.format("var getNextQuestionID = function (oid) { %s };"
                , (branch ? "if (branchTable.hasOwnProperty(oid)) { return $(\"#\"+branchTable[oid]); } " : "")
                + "return $(\"#\"+oid).closest(\"div\").next();");
    }
    
    private static String makeJS(Survey survey) {
        String branchTable = makeBranchTable(survey);
        String nextQuestions = makeGetNextQuestionID(!branchTable.equals(""));
        return branchTable+"\r\n"+nextQuestions;
    }

    public static String getJSString(Survey survey) {
        String js = "";
        try {
            js = String.format(Slurpie.slurp(MturkLibrary.JSSKELETON)
                    , makeJS(survey));
        } catch (FileNotFoundException ex) {
            LOGGER.fatal(ex);
            System.exit(-1);
        } catch (IOException ex) {
            LOGGER.fatal(ex);
            System.exit(-1);
        }
        return (new ClosureJavaScriptCompressor()).compress(js);
    }
}
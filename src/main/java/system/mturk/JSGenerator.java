package system.mturk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map.Entry;
import survey.*;
import utils.Slurpie;
import org.apache.log4j.Logger;
import survey.Block;

public class JSGenerator{
    
    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    
    private static String makeBranchTable(Survey survey) {
        String branchTable = "var branchTable = {";
        StringBuilder entries = new StringBuilder();
        for (Question q : survey.questions) {
            if (q.branchMap.size() > 0) {
                for (Entry<Component, Block> entry : q.branchMap.entrySet()) {
                    // get the id of the first question in the block we need to jump to
                    String quid = entry.getValue().questions.get(0).quid;
                    String oid = entry.getKey().cid;
                    entries.append(oid+":"+quid+", ");
                }
            }
        }
        if (entries.length()==0)
            return "";
        else return branchTable + entries.toString() + "};";
    }
    
    private static String makeGetNextQuestionID(boolean branch) {
        // if the option chosen is in the branch table, 
        // jump to the first question in the corresponding block
        StringBuilder getNextQuestionID = new StringBuilder("var getNextQuestionID = function (oid) {");
        if (branch)
            getNextQuestionID.append("if (branchTable.hasOwnProperty(oid)) { return branchTable[oid]; } ");
        else getNextQuestionID.append("return $(\"#\"+oid).closest(\"div\").id");
        return getNextQuestionID.toString();
    }
    
    private static String makeJS(Survey survey) {
        String branchTable = makeBranchTable(survey);
        String nextQuestions = makeGetNextQuestionID(branchTable.equals(""));
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
        return js;
    }
}
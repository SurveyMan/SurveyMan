package system.mturk.generators;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.Map.Entry;
import scala.Tuple2;
import survey.*;
import org.apache.log4j.Logger;
import survey.Block;
import survey.Block.BranchParadigm;
import system.Slurpie;
import system.mturk.MturkLibrary;

public class JS {
    
    private static final Logger LOGGER = Logger.getLogger("system.mturk");
    
    private static String makeBranchTable(Survey survey) {
        ArrayList<Tuple2<String, String>> entries = new ArrayList<Tuple2<String, String>>();
        for (Question q : survey.questions) {
            if (q.branchMap.size() > 0) {
                for (Entry<Component, Block> entry : q.branchMap.entrySet()) {
                    // get the id of the first question in the block we need to jump to
                    String quid = entry.getValue().questions.get(0).quid;
                    String oid = entry.getKey().getCid();
                    entries.add(new Tuple2<String, String>(oid, "\""+quid+"\""));
                }
            }
        }
        if (entries.isEmpty())
            return " branchTable = {}; ";
        else {
            StringBuilder table = new StringBuilder(String.format("%s : %s", entries.get(0)._1(), entries.get(0)._2()));
            for (Tuple2<String, String> entry : entries.subList(1,entries.size()))
                table.append(String.format(", %s : %s", entry._1(), entry._2()));
            return String.format(" branchTable = { %s }; ", table.toString());
        }
    }
    
    private static String makeBreakoffList(Survey survey) throws SurveyException {
        // for now, only the last question is okay for breakoff
        String lastQ = String.format(" lastQuestionId = \"%s\"; ", survey.getQuestionsByIndex()[survey.questions.size() - 1].quid);
        StringBuilder s = new StringBuilder();
        for (Question q : survey.questions)
            if (q.permitBreakoff)
                s.append(String.format("%s \"%s\"", s.length()==0 ? "" : ",", q.quid));
        return String.format("%s bList = [ %s ]; ", lastQ, s.toString());
    }

    private static String makeLoadPreview(Component preview) {
        return String.format(" var loadPreview = function () { $('#preview').load('%s'); }; "
                , ((URLComponent) preview).data.toExternalForm());
    }

    private static String getQType(Question q) {
        if (q.options.isEmpty())
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
                    , c.getCid()
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
        return String.format(" oTable = { %s }; ", s.toString());
    }
    
    private static String makeQuestionTable(Survey survey) 
            throws SurveyException, MalformedURLException {
        StringBuilder s = new StringBuilder();
        for (Question q : survey.getQuestionsByIndex())
            s.append(String.format("%s \"%s\" : \"%s\" "
                    , q.index == 0 ? "" : ","
                    , q.quid
                    , HTML.stringify(q)
                ));
        return String.format(" qTable = { %s }; ", s.toString());
    }
    
    private static String getNextQuestionId(Survey survey, Question q) throws SurveyException {
        String quid = "";
        if (q.block==null) {
            q.block = new Block();
            q.block.branchParadigm = BranchParadigm.NONE;
        }
        Question[] questions = survey.getQuestionsByIndex();
        if (q!=questions[questions.length-1])
            quid = questions[q.index+1].quid;

        return quid;
    }
    
    private static String makeQuestionTransitionTable(Survey survey) throws SurveyException {
        StringBuilder s = new StringBuilder();
        for (Question q : survey.getQuestionsByIndex()) {
            String nextQuid = getNextQuestionId(survey, q);
            if (nextQuid.isEmpty())
                continue;
            s.append(String.format("%s \"%s\" : \"%s\" "
                    , q.index == 0 ? "" : ","
                    , q.quid
                    , nextQuid
            ));
        }
        return String.format(" qTransTable = { %s }; ", s.toString());
    }

    private static String makeOneBranchTable(Survey survey) {
        // returns a JS map
        StringBuilder s = new StringBuilder();
        for (Block b : survey.blocks) {
            if (b.branchParadigm.equals(BranchParadigm.ONE)) {
                Collections.sort(b.questions);
                s.append(String.format("%s \"%s\" : \"%s\" "
                        , s.length()==0 ? "" : ","
                        , b.branchQ.quid
                        , b.questions.get(b.questions.size() - 1).quid
                    )
                );
            }
        }
        return String.format(" oneBranchTable = { %s }; ", s.toString());
    }

    private static String setFirstQuestion(Survey survey) throws SurveyException {
        return "firstQuestionId = \"" + survey.getQuestionsByIndex()[0].quid + "\";";
    }

    private static String jsonizeBranchMap(Map<Component, Block> branchMap) {
        Iterator<Entry<Component, Block>> entrySet = branchMap.entrySet().iterator();
        if (!entrySet.hasNext())
            return "";
        Entry<Component, Block> entry = entrySet.next();
        StringBuilder s = new StringBuilder(String.format("\"%s\" : \"%s\"", entry.getKey().getCid(), entry.getValue().strId));
        while (entrySet.hasNext()) {
            entry = entrySet.next();
            s.append(String.format(", \"%s\" : \"%s\"", entry.getKey().getCid(), entry.getValue().strId));
        }
        return s.toString();
    }

    private static String jsonizeOption(Component option) {
        return String.format("{ \"id\" : \"%s\", \"otext\" : \"%s\" }"
        , option.getCid()
        , option.toString());
    }

    private static String jsonizeOptions(List<Component> options) {
        StringBuilder s = new StringBuilder(jsonizeOption(options.get(0)));
        for (Component o : options.subList(1, options.size() - 1)) {
            s.append(String.format(", %s", jsonizeOption(o)));
        }
        return s.toString();
    }

    private static String jsonizeQuestion(Question question) throws SurveyException {
        StringBuilder qtext = new StringBuilder();

        try {
            for (Component q : question.data) {
                qtext.append(HTML.stringify(q));
            }
        } catch (SurveyException se) {
            LOGGER.info("SurveyException thrown in jsonizeQuestion" + se);
        }

        return String.format("{ \"id\" : \"%s\", \"qtext\" : \"%s\", \"options\" : [ %s ], \"branchMap\" : { %s }, \"randomize\" : %s, \"ordered\" : %s, \"exclusive\" : %s, \"breakoff\" : %s }"
                , question.quid
                , qtext
                , jsonizeOptions(Arrays.asList(question.getOptListByIndex()))
                , jsonizeBranchMap(question.branchMap)
                , question.randomize
                , question.ordered
                , question.exclusive
                , question.permitBreakoff);
    }

    private static String jsonizeQuestions(List<Question> questionList) throws SurveyException {
        StringBuilder s = new StringBuilder(jsonizeQuestion(questionList.get(0)));
        for (Question q : questionList.subList(1, questionList.size() - 1)) {
            s.append(String.format(", %s", jsonizeQuestion(q)));
        }
        return s.toString();
    }

    private static String jsonizeBlock(Block b) throws SurveyException{
        return String.format("{ \"idString\" : \"%s\", \"questions\" : [ %s ], \"randomize\" : %s}"
                , b.strId
                , jsonizeQuestions(b.questions)
                , b.isRandomized());
    }

    private static String jsonizeBlocks(List<Block> blockList) throws SurveyException{
        StringBuilder s = new StringBuilder(jsonizeBlock(blockList.get(0)));
        for (Block b : blockList.subList(1, blockList.size() - 1)) {
            s.append(String.format(", %s", jsonizeBlock(b)));
        }
        return s.toString();
    }

    private static String makeJSON(Survey survey) throws SurveyException{
        return String.format("var jsonizedSurvey = { \"filename\" : \"%s\", \"breakoff\" :  %b, \"survey\" : [ %s ] }; var sm = SurveyMan(jsonizedSurvey);"
                , survey.source
                , survey.permitsBreakoff()
                , jsonizeBlocks(survey.topLevelBlocks));
    }

    private static String makeJS(Survey survey, Component preview) throws SurveyException, MalformedURLException {
        String json = makeJSON(survey);
//        String firstQ = setFirstQuestion(survey);
//        String qTransTable = makeQuestionTransitionTable(survey);
//        String qTable = makeQuestionTable(survey);
//        String branchTable = makeBranchTable(survey);
//        String oTable = makeOptionTable(survey);
//        String bList = makeBreakoffList(survey);
//        String oneBranchTable = makeOneBranchTable(survey);
        String loadPreview;
        if (preview instanceof URLComponent)
            loadPreview = makeLoadPreview(preview);
        else loadPreview = " var loadPreview = function () {}; ";
        return String.format("%s %s"
                , loadPreview
                , json
//                    , firstQ
//                    , qTransTable
//                    , qTable
//                    , branchTable
//                    , oTable
//                    , bList
//                    , oneBranchTable
        );
    }

    public static String getJSString(Survey survey, Component preview) throws SurveyException, IOException{
        String js = "";
        try {
            String temp = String.format("var customInit = function() { %s };", Slurpie.slurp(MturkLibrary.JSSKELETON));
            js = makeJS(survey, preview) + temp;
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
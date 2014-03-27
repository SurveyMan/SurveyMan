import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import csv.CSVLexer;
import csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import survey.*;
import system.BackendType;
import system.Library;
import system.Record;
import system.localhost.LocalLibrary;
import system.localhost.LocalResponseManager;
import system.localhost.Server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@RunWith(JUnit4.class)
public class JSTest extends TestLog {

    public JSTest(){
        super.init(this.getClass());
    }

    private Component getAnswer(Question q) {
        return (Component) q.options.values().toArray()[0];
    }

    public Map<Question, Component> generateAnswers(Survey survey){
        Map<Question, Component> answers = new HashMap<Question, Component>();
        for (int i = 0 ; i < survey.topLevelBlocks.size() ; i++) {
            Block block = survey.topLevelBlocks.get(i);
            Iterator<Question> questionList = block.getAllQuestions().iterator();
            switch (block.branchParadigm) {
                case NONE:
                    while (questionList.hasNext()) {
                        Question question =  questionList.next();
                        answers.put(question, getAnswer(question));
                        if (question.block.branchParadigm.equals(Block.BranchParadigm.SAMPLE)) {
                            break;
                        }
                    }
                    break;
                case ONE:
                    Question branchQ = block.branchQ;
                    Block destBlock = null;
                    while (questionList.hasNext()) {
                        Question q = questionList.next();
                        Component c = getAnswer(q);
                        if (q.equals(branchQ))
                             destBlock = q.branchMap.get(c);
                        answers.put(q, c);
                    }
                    // advance to the appropriate block
                    for (int j = i ; j < survey.topLevelBlocks.size() ; j++) {
                        if (survey.topLevelBlocks.get(j).equals(destBlock)) {
                            i = j - 1;
                            break;
                        }
                    }
                    break;
                case SAMPLE:
                    Question selectOne = questionList.next();
                    Component answer = getAnswer(selectOne);
                    Block branchTo = selectOne.branchMap.get(answer);
                    answers.put(selectOne, answer);
                    for (int j = i ; j < survey.topLevelBlocks.size() ; j++) {
                        if (survey.topLevelBlocks.get(j).equals(branchTo)) {
                            i = j - 1;
                            break;
                        }
                    }
                    break;
           }
       }
       return answers;
    }

    private Map<String, Question> generateOidToQuestionMap(Survey s) throws SurveyException {
        Map<String, Question> o2q = new HashMap<String, Question>();
        for (Question q : s.getQuestionsByIndex())
            for (Component c : q.getOptListByIndex())
                o2q.put(c.getCid(), q);
        return o2q;
    }

    @Test
    public void testPaths() throws IOException, InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException, InterruptedException {
        // generate test paths through the survey that pick the lexicographically first option for
        // each test survey. Take the test. Assert that the results are equivalent
        for (int i = 0 ; i < super.testsFiles.length ; i++) {
            // run the survey using the local server
            Survey survey = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i]))).parse();
            Map<Question, Component> answerMap = generateAnswers(survey);
            LocalResponseManager.putRecord(survey, new LocalLibrary(), BackendType.LOCALHOST);
            Record record = LocalResponseManager.getRecord(survey);
            String[] filename = record.getHtmlFileName().split(Library.fileSep);
            Server.startServe();
            Map<String, Question> opt2qMap = generateOidToQuestionMap(survey);
            List<Component> answers = new ArrayList<Component>();
            for (BrowserVersion bv : new BrowserVersion[]{BrowserVersion.CHROME, BrowserVersion.FIREFOX_17, BrowserVersion.INTERNET_EXPLORER_10}) {
                WebClient webClient = new WebClient(bv);
                HtmlPage page = webClient.getPage(String.format("http://localhost:%d/logs/%s", Server.frontPort, filename[filename.length - 1]));
                while (Server.requests < 5) {
                    //webClient.waitForBackgroundJavaScript(1000);
                    return;
                }
                final HtmlForm form = page.getFormByName("mturk_form");
                int ansId = 1;
                while (page.getElementById("final_submit") == null){
                    List<HtmlElement> ps = form.getHtmlElementsByTagName("p");
                    Question q = null;
                    // get the appropriate answer
                    for (HtmlElement elt : ps) {
                        if (elt.getId().equals("ans" + ansId)) {
                            List<HtmlElement> inputs = elt.getHtmlElementsByTagName("input");
                            q = opt2qMap.get(inputs.get(0).getAttribute("name"));
                            Component a = answerMap.get(q);
                            for (HtmlElement input : inputs) {
                                String oid = input.getAttribute("id");
                                if (oid.equals(a.getCid())) {
                                    String type = ((HtmlInput) input).getTypeAttribute();
                                    if (type.equals("text"))
                                        ((HtmlTextArea) input).setText("foo");
                                    else if (type.equals("radio"))
                                        ((HtmlRadioButtonInput) input).setChecked(true);
                                    else if (type.equals("check"))
                                        ((HtmlCheckBoxInput) input).setChecked(true);
                                    answers.add(a);
                                    break;
                                }
                            }
                        }
                    }
                    HtmlButton next = (HtmlButton) page.getElementById("submit_"+q.quid);
                    next.click();
                    webClient.waitForBackgroundJavaScript(1000);
                }
                assert(answers.size() == answerMap.size());
                webClient.closeAllWindows();
            }
            Server.endServe();
        }
    }

}

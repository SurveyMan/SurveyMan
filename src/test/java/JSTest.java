import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import csv.CSVLexer;
import csv.CSVParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import survey.*;
import system.localhost.Server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RunWith(JUnit4.class)
public class JSTest extends TestLog {

    public JSTest(){
        super.init(this.getClass());
    }

    private Component getAnswer(Question q) {
        return (Component) q.options.values().toArray()[0];
    }
}

    public Map<Question, Component> generateAnswers(Survey survey){
        Map<Question, Component> answers = new HashMap<Question, Component>();
        for (int i = 0 ; i < survey.topLevelBlocks.size() ; i++) {
            Block block = survey.topLevelBlocks.get(i);
            List<Question> questionList = block.getAllQuestions();
            switch (block.branchParadigm) {
                case NONE:
                    for (Question question : questionList) {
                        answers.put(question, getAnswer(question));
                        if (question.block.branchParadigm.equals(Block.BranchParadigm.ALL)) {
                            // remove all other questions in the immediate block
                            for (Question q : question.block.getAllQuestions())
                                questionList.remove(q);
                        }
                    }
                    break;
                case ONE:
                    Question branchQ = block.branchQ;
                    Block destBlock = null;
                    for (Question q : questionList) {
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
                case ALL:
                    Question selectOne = questionList.get(0);
                    Component answer = getAnswer(selectOne);
                    Block branchTo = selectOne.branchMap.get(answer);
                    answers.put(selectOne, answer);
                    for (int j = i ; j < survey.topLevelBlocks.size() ; j++) {
                        if (survey.topLevelBlocks.get(j).equals(destBlock)) {
                            i = j - 1;
                            break;
                        }
                    }
                    break;
           }
       }
       return answers;
    }

    @Test
    public void testPaths() throws IOException, InvocationTargetException, SurveyException, IllegalAccessException, NoSuchMethodException {
        // generate test paths through the survey that pick the lexicographically first option for
        // each test survey. Take the test. Assert that the results are equivalent
        for (int i = 0 ; i < super.testsFiles.length ; i++) {
            // run the survey using the local server
            Survey survey = new CSVParser(new CSVLexer(testsFiles[i], String.valueOf(separators[i]))).parse();
            Map<Question, Component> answerMap = generateAnswers(survey);
            String filename = survey.sourceName;
            Server.startServe();
            for (BrowserVersion bv : new BrowserVersion[]{BrowserVersion.CHROME, BrowserVersion.FIREFOX_24, BrowserVersion.INTERNET_EXPLORER_11}) {
                final WebClient webClient = new WebClient(bv);
                final HtmlPage page = webClient.getPage(String.format("http://localhost:%d/logs/%s", Server.port, filename));
                final HtmlForm form = page.getFormByName("mturk_form");
                int ansId = 1;
                while (form.getButtonByName("Submit") == null){
                    //form.getOneHtmlElementByAttribute()
                    //form.getInputsByName(qid);
                }
            }
        }
    }

}

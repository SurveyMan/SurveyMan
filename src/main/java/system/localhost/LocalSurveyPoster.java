package system.localhost;

import survey.Survey;
import survey.SurveyException;
import system.Library;
import system.Record;
import system.generators.HTML;
import system.interfaces.ResponseManager;
import system.interfaces.SurveyPoster;
import system.interfaces.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalSurveyPoster implements SurveyPoster{

    private boolean firstPost = true;

    @Override
    public boolean getFirstPost() {
            return firstPost;
    }

    @Override
    public void setFirstPost(boolean post) {
        firstPost = post;
    }

    @Override
    public void refresh(Record r) {

    }

    @Override
    public List<Task> postSurvey(ResponseManager responseManager, Record r) throws SurveyException {
        List<Task> tasks = new ArrayList<Task>();
        try {
            Task task = new LocalTask(r);
            tasks.add(task);
            HTML.spitHTMLToFile(HTML.getHTMLString(r.survey, new system.localhost.generators.HTML()), r.survey);
            firstPost = false;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return tasks;
    }

    @Override
    public boolean postMore(ResponseManager mturkResponseManager, Survey survey) {
        return firstPost;
    }

    @Override
    public String makeTaskURL(Task task) {
        Record r = task.getRecord();
        String[] pieces = r.getHtmlFileName().split(Library.fileSep);
        return String.format("http://localhost:%d/logs/%s", Server.frontPort, pieces[pieces.length - 1]);
    }
}

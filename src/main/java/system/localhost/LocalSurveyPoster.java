package system.localhost;

import survey.Survey;
import survey.exceptions.SurveyException;
import system.Library;
import system.Record;
import system.Runner;
import system.generators.HTML;
import system.interfaces.AbstractResponseManager;
import system.interfaces.ISurveyPoster;
import system.interfaces.ITask;
import system.localhost.generators.LocalHTML;
import system.localhost.server.WebServerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalSurveyPoster implements ISurveyPoster {

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
    public List<ITask> postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException {
        List<ITask> tasks = new ArrayList<ITask>();
        try {
            ITask task = new LocalTask(r);
            tasks.add(task);
            HTML.spitHTMLToFile(HTML.getHTMLString(r.survey, new LocalHTML()), r.survey);
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
    public boolean postMore(AbstractResponseManager responseManager, Record r) {
        boolean fp = firstPost;
        firstPost = false;
        return fp;
    }

    @Override
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, Runner.BoxedBool interrupt) {
       try {
           boolean success = Server.endSurvey();
           interrupt.setInterrupt(true);
           return success;
       } catch (WebServerException se) {
           return false;
       }
    }

    @Override
    public String makeTaskURL(ITask task) {
        Record r = task.getRecord();
        String[] pieces = r.getHtmlFileName().split(Library.fileSep);
        while (!Server.serving) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return String.format("http://localhost:%d/logs/%s", Server.frontPort, pieces[pieces.length - 1]);
    }
}

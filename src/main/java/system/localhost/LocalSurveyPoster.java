package system.localhost;

import interstitial.*;
import survey.exceptions.SurveyException;
import system.generators.HTML;
import system.localhost.generators.LocalHTML;
import system.localhost.server.WebServerException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalSurveyPoster implements ISurveyPoster {

    private boolean firstPost = true;

    @Override
    public void refresh(Record r) {

    }

    @Override
    public ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException {
        ITask task = null;
        try {
            task = new LocalTask(r);
            HTML.spitHTMLToFile(HTML.getHTMLString(r.survey, new LocalHTML()), r.survey);
            firstPost = false;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        task.setRecord(r);
        return task;
    }

    @Override
    public boolean stopSurvey(AbstractResponseManager responseManager, Record r, BoxedBool interrupt) {
       try {
           boolean success = Server.endSurvey();
           interrupt.setInterrupt(true, "Call to stop survey.", this.getClass().getEnclosingMethod());
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

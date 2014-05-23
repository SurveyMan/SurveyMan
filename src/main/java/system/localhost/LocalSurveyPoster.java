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

    @Override
    public void refresh(Record r) {

    }

    @Override
    public ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException {
        ITask task = null;
        try {
            task = new LocalTask(r);
            task.setRecord(r);
            HTML.spitHTMLToFile(HTML.getHTMLString(r.survey, new LocalHTML()), r.survey);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
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
        if (Server.serving)
            return String.format("http://localhost:%d/logs/%s", Server.frontPort, pieces[pieces.length - 1]);
        else return "";
    }
}

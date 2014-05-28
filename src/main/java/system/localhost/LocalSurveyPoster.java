package system.localhost;

import interstitial.*;
import survey.exceptions.SurveyException;
import system.generators.HTML;
import system.localhost.generators.LocalHTML;
import system.localhost.server.WebServerException;

import java.io.IOException;

public class LocalSurveyPoster implements ISurveyPoster {

<<<<<<< HEAD
    @Override
    public void init(String configURL) {

    }

=======
>>>>>>> 4c70a41beeaa860bd1a36014af0c182bbfe704bd
    @Override
    public ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException {

        ITask task = null;

        if (r.getAllTasks().length>0)
            return task;

<<<<<<< HEAD
        try {
            task = new LocalTask(r);
            task.setRecord(r);
            HTML.spitHTMLToFile(HTML.getHTMLString(r, new LocalHTML()), r.survey);
=======
    @Override
    public ITask postSurvey(AbstractResponseManager responseManager, Record r) throws SurveyException {
        ITask task = null;
        try {
            task = new LocalTask(r);
            task.setRecord(r);
            HTML.spitHTMLToFile(HTML.getHTMLString(r.survey, new LocalHTML()), r.survey);
>>>>>>> 4c70a41beeaa860bd1a36014af0c182bbfe704bd
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

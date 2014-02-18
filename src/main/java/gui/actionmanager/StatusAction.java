package gui.actionmanager;

import com.amazonaws.mturk.requester.HIT;
import csv.CSVLexer;
import csv.CSVParser;
import gui.ExperimentActions;
import gui.SurveyMan;
import gui.display.Experiment;
import survey.Survey;
import survey.SurveyException;
import system.*;
import system.interfaces.ResponseManager;
import system.interfaces.SurveyPoster;
import system.interfaces.Task;
import system.mturk.*;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Properties;

public class StatusAction implements MenuListener{
    private ExperimentActions action;
    private JMenu menu;

    public StatusAction (ExperimentActions action, JMenu menu) {
        this.action = action;
        this.menu = menu;
    }

    @Override
    public void menuDeselected(MenuEvent event){

    }

    @Override
    public void menuCanceled(MenuEvent event){

    }

    @Override
    public void menuSelected(MenuEvent actionEvent) {
        switch (action) {
            case CANCEL_RUNNING:
                list_running();
                add_cancellation();
                break;
            case STOP_SAVE:
                list_running();
                add_stop_save();
                break;
            case RUN_UNFINISHED:
                list_unfinished();
                add_run_unfinished();
                break;
            case RERUN:
                list_completed();
                add_run_completed();
                break;
            case STATUS:
                list_running();
                add_list_status();
                break;
        }
    }

    private void add_run_completed() {
        menu.removeAll();
        JMenuItem experiment = new JMenuItem();
        experiment.setText("This feature is not yet implemented.");
        menu.add(experiment);
    }

    private void list_completed(){
        menu.removeAll();
        JMenuItem experiment = new JMenuItem();
        experiment.setText("This feature is not yet implemented.");
        menu.add(experiment);
    }

    private void add_run_unfinished(){
        for (Component item : menu.getMenuComponents()) {
            final JMenuItem menuItem = (JMenuItem) item;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    // load in parameters and run
                    // an old experiment that's already running shouldn't be run again (i.e. two instances concurrently)
                    System.out.println("running unfinished");
                    Properties params = new Properties();
                    String jobId = menuItem.getText();
                    String dir = Library.STATEDATADIR + Library.fileSep + jobId;
                    String paramFile = dir + Library.fileSep + jobId + ".params";
                    String surveyFile = dir + Library.fileSep + jobId + ".csv";
                    try {
                        params.load(new FileReader(paramFile));
                    } catch (IOException e) {
                        SurveyMan.LOGGER.warn(e);
                    }
                    final Survey survey;
                    try {
                        survey = new CSVParser(new CSVLexer(surveyFile, params.getProperty("fieldsep", ","))).parse();
                        BackendType backendType = JobManager.getBackendTypeByJobID(jobId);
                        ResponseManager responseManager = Runner.responseManagers.get(backendType);
                        SurveyPoster surveyPoster = Runner.surveyPosters.get(backendType);
                        surveyPoster.setFirstPost(false);
                        Record record = new Record(survey, ExperimentAction.getBackendLibClass(), backendType);
                        JobManager.populateTasks(jobId, record, responseManager);
                        synchronized(responseManager.manager) {
                            ResponseManager.manager.put(survey.sid, record);
                            ExperimentAction.cachedSurveys.put(surveyFile, survey);
                            Experiment.csvLabel.addItem(surveyFile);
                            Experiment.csvLabel.setSelectedItem(surveyFile);
                        }
                        Runner.BoxedBool interrupt = new Runner.BoxedBool(false);
                        Thread runner = (new ExperimentAction(null)).makeRunner(record, interrupt);
                        Thread notifier = (new ExperimentAction(null)).makeNotifier(runner, survey);
                        runner.start();
                        notifier.start();
                        Thread getter = Runner.makeResponseGetter(survey, interrupt, ExperimentAction.backendType);
                        Thread writer = Runner.makeWriter(survey, interrupt);
                        getter.start(); writer.start();
                        System.out.println("Removing this unfinished job");
                        JobManager.removeUnfinished(jobId);
                    } catch (IOException e) {
                        SurveyMan.LOGGER.warn(e);
                    } catch (SurveyException e) {
                        SurveyMan.LOGGER.warn(e);
                    } catch (InvocationTargetException e) {
                        SurveyMan.LOGGER.warn(e);
                    } catch (NoSuchMethodException e) {
                        SurveyMan.LOGGER.warn(e);
                    } catch (IllegalAccessException e) {
                        SurveyMan.LOGGER.warn(e);
                    } catch (SystemException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    private String[] getUnfinishedJobs() {
        return null;
    }

    private void list_unfinished(){
        menu.removeAll();
        // read in names of unfinished jobs
        try {
            String unfinished = Slurpie.slurp(Library.UNFINISHED_JOB_FILE);
            for (String record : unfinished.split("\n")) {
                String txt = record.split(",")[0];
                if (txt.equals("")) // if it the job is currently running
                    continue;
                JMenuItem unfinishedJob = new JMenuItem();
                unfinishedJob.setName(record);
                unfinishedJob.setText(txt);
                menu.add(unfinishedJob);
          }
        } catch (IOException ex) {
          SurveyMan.LOGGER.error(ex);
        }
    }

    private void add_list_status(){
        for (Component item : menu.getMenuComponents()) {
            final JMenuItem menuItem = (JMenuItem) item;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String sid = menuItem.getName();
                    Survey survey = StatusAction.getFromThreadMapBySID(sid);
                    try {
                        Record record = ResponseManager.getRecord(survey);
                        assert(record!=null);
                        ResponseManager responseManager = Runner.responseManagers.get(record.backendType);
                        int totalPosted = record.getAllTasks().length;
                        int responsesSoFar = record.responses.size();
                        int stillLive = responseManager.listAvailableTasksForRecord(record).size();
                        boolean complete = record.qc.complete(record.responses, record.library.props);
                        String hitId = record.getLastTask().getTaskId();
                        Experiment.updateStatusLabel(String.format("Status of survey %s with id %s:" +
                                "\n\tTotal surveys posted: %d" +
                                "\n\t#/responses so far: %d" +
                                "\n\t#/surveys still live: %d" +
                                "\n\tlast HIT posted: %s" +
                                "\n\tsurvey complete: %b"
                                , survey.sourceName
                                , survey.sid
                                , totalPosted
                                , responsesSoFar
                                , stillLive
                                , hitId
                                , complete
                        ));
                    } catch (IOException e) {
                        SurveyMan.LOGGER.warn(e);
                    } catch (SurveyException ex) {
                        SurveyMan.LOGGER.warn(ex);
                    }
                }
            });
       }
    }

    private static Survey getFromThreadMapBySID(String sid){
        for (Survey s : ExperimentAction.threadMap.keySet()){
            if (s.sid.equals(sid))
                return s;
        }
        throw new RuntimeException("Survey not found in current thread map");
    }

    private void add_stop_save(){
        // add action listener to the contents of the menu that stops all threads
        // and saves metadata for future runs
        for (Component item : menu.getMenuComponents()) {
            final JMenuItem menuItem = (JMenuItem) item;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String sid = menuItem.getName();
                    Survey survey = getFromThreadMapBySID(sid);
                    Record record;
                    BackendType backendType;
                    try {
                        record = ResponseManager.getRecord(survey);
                        backendType = record.backendType;
                        // stop threads
                        for (ExperimentAction.ThreadBoolTuple tupe : ExperimentAction.threadMap.get(survey))
                            tupe.boxedBool.setInterrupt(true);
                        JobManager.saveParametersAndSurvey(survey, record);
                        // write the id and a list of the associated hits
                        JobManager.addToUnfinishedJobsList(survey, record, record.backendType);
                        try {
                            while (ResponseManager.getRecord(survey)!=null) {}
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (SurveyException e) {
                            e.printStackTrace();
                        }
                        //ResponseManager responseManager = Runner.responseManagers.get(backendType);
                        //responseManager.makeTaskAvailable(JobManager.makeJobID(survey));
                        Experiment.updateStatusLabel(String.format("Stopped and saved survey %s (%s)."
                                , survey.sourceName
                                , survey.sid
                        ));

                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (SurveyException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void add_cancellation(){
        // for each experiment listed in the menu, add an action listener that stops the thread
        // and expires all related hits
        for (Component item : menu.getMenuComponents()){
            final JMenuItem menuItem = (JMenuItem) item;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    String sid = menuItem.getName();
                    Survey survey = getFromThreadMapBySID(sid);
                    ResponseManager responseManager = Runner.responseManagers.get(ExperimentAction.backendType);
                    Experiment.updateStatusLabel(String.format("Survey %s (%s) being cancelled..."
                            , survey.sourceName
                            , survey.sid
                    ));
                    synchronized (ExperimentAction.threadMap) {
                        List<ExperimentAction.ThreadBoolTuple> threadList = ExperimentAction.threadMap.get(survey);
                        for (ExperimentAction.ThreadBoolTuple tupe : threadList) {
                            Thread t = tupe.t;
                            Runner.BoxedBool b = tupe.boxedBool;
                            System.out.println(t.getName() + t.getState().name());
                            b.setInterrupt(true);
                        }
                    }
                    try {
                        System.out.println("stuff");
                        Record record = ResponseManager.getRecord(survey);
                        assert(record!=null);
                        for (Task hit : record.getAllTasks()) {
                            System.out.println(hit.getTaskId());
                            responseManager.makeTaskUnavailable(hit);
                            Experiment.updateStatusLabel("Expired HIT : " + hit.getTaskId());
                        }
                        JobManager.removeUnfinished(JobManager.makeJobID(survey));
                    } catch (IOException ex) {
                        SurveyMan.LOGGER.warn(ex);
                    } catch (SurveyException ex) {
                        SurveyMan.LOGGER.warn(ex);
                    }
                }
            });
        }
    }

    private void list_running() {
        // clear current menu
        menu.removeAll();
        // adds the currently running experiments to the menu
        synchronized (ExperimentAction.threadMap) {
            for (Survey survey : ExperimentAction.threadMap.keySet()){
                JMenuItem experiment = new JMenuItem();
                experiment.setText(String.format("%s (%s)", survey.sourceName, survey.sid));
                experiment.setName(survey.sid);
                menu.add(experiment);
            }
        }
    }

}

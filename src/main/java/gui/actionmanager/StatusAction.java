package gui.actionmanager;

import com.amazonaws.mturk.requester.HIT;
import csv.CSVLexer;
import csv.CSVParser;
import gui.ExperimentActions;
import gui.SurveyMan;
import gui.display.Experiment;
import scala.Tuple2;
import survey.Survey;
import survey.SurveyException;
import system.Library;
import system.mturk.*;
import system.Slurpie;

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

    }

    private void list_completed(){
        menu.removeAll();
        List<Tuple2<String, Properties>> completed = MturkLibrary.surveyDB.get(Library.JobStatus.COMPLETED);
        for (Tuple2<String, Properties> tupe : completed) {
           JMenuItem experiment = new JMenuItem();
            experiment.setText(tupe._1());
            experiment.setName(tupe._1());
            menu.add(experiment);
        }
    }

    private void add_run_unfinished(){
        for (Component item : menu.getMenuComponents()) {
            final JMenuItem menuItem = (JMenuItem) item;
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    // load in parameters and run
                    Properties params = new Properties();
                    String jobId = menuItem.getText();
                    String name = MturkLibrary.STATEDATADIR+Library.fileSep+jobId;
                    String data = menuItem.getName();
                    try {
                        params.load(new FileReader(name));
                    } catch (IOException e) {
                        SurveyMan.LOGGER.warn(e);
                    }
                    String filename = name +".csv";
                    MturkLibrary.props = params;
                    SurveyPoster.updateProperties();
                    final Survey survey;
                    try {
                        survey = new CSVParser(new CSVLexer(filename, params.getProperty("fieldsep", ","))).parse();
                        Record record = new Record(survey, params);
                        String[] oldHITIds = data.split(",");
                        for (int i = 1 ; i < oldHITIds.length ; i++)
                            record.addNewHIT(ResponseManager.getHIT(oldHITIds[i]));
                        synchronized(ResponseManager.manager) {
                          ResponseManager.manager.put(survey.sid, record);
                          ExperimentAction.cachedSurveys.put(filename, survey);
                          Experiment.csvLabel.addItem(filename);
                          Experiment.csvLabel.setSelectedItem(filename);
                        }
                        Runner.BoxedBool interrupt = new Runner.BoxedBool(false);
                        Thread runner = (new ExperimentAction(null)).makeRunner(survey, interrupt);
                        Thread notifier = (new ExperimentAction(null)).makeNotifier(runner, survey);
                        runner.start();
                        notifier.start();
                        Thread getter = Runner.makeResponseGetter(survey, interrupt);
                        Thread writer = Runner.makeWriter(survey, interrupt);
                        getter.start(); writer.start();
                        removeUnfinished(data);
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
                    }
                }
            });
        }
    }
    
    private void removeUnfinished(String data) {
      try {
        String unfinished = Slurpie.slurp(SurveyMan.UNFINISHED_JOB_FILE);
        String writeMe = "";
        for (String line : unfinished.split("\n"))
          if (!line.equals(data))
            writeMe += String.format("%s\n", line);
        dump(SurveyMan.UNFINISHED_JOB_FILE, writeMe);
      } catch (IOException ex) {
        SurveyMan.LOGGER.warn(ex);
      }
    }

    private void list_unfinished(){
        menu.removeAll();
        // read in names of unfinished jobs
        try {
          String unfinished = Slurpie.slurp(SurveyMan.UNFINISHED_JOB_FILE);
          for (String record : unfinished.split("\n")) {
              String txt = record.split(",")[0];
              SurveyMan.LOGGER.info(String.format("Adding %s to the menu", txt));
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
                        int totalPosted = record.getAllHITs().length;
                        int responsesSoFar = record.responses.size();
                        int stillLive = ResponseManager.listAvailableHITsForRecord(record).size();
                        boolean complete = record.qc.complete(record.responses, record.parameters);
                        String hitId = record.getLastHIT().getHITId();
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

    private void dump(String filename, String s) throws IOException{
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true));
        writer.write(s);
        writer.close();
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
                    // stop threads
                    for (Tuple2<Thread, Runner.BoxedBool>  tupe : ExperimentAction.threadMap.get(survey))
                        tupe._2().setInterrupt(true);
                    // write all responses to file
                    String jobID = survey.sourceName+"_"+survey.sid+"_"+Library.TIME;
                    Record record = null;
                    try {
                        record = ResponseManager.getRecord(survey);
                    } catch (IOException ex) {
                        SurveyMan.LOGGER.warn(ex);
                    } catch (SurveyException ex) {
                        SurveyMan.LOGGER.warn(ex);
                    }
                    // save parameters
                    try {
                        String prefix = Library.DIR+Library.fileSep+"data"+Library.fileSep;
                        record.parameters.store(new FileWriter(prefix+jobID), "");
                        dump(prefix+jobID+".csv", Slurpie.slurp(survey.source));
                    } catch (IOException ex) {
                        SurveyMan.LOGGER.warn(ex);
                    }
                    // write the id and a list of the associated hits
                    String data = jobID;
                    for (HIT hit : record.getAllHITs())
                        data = data + "," + hit.getHITId();
                    data = data + "\n";
                    try {
                        dump(SurveyMan.UNFINISHED_JOB_FILE, data);
                    } catch (IOException ex) {
                        SurveyMan.LOGGER.warn(ex);
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
                    Experiment.updateStatusLabel(String.format("Survey %s (%s) being cancelled..."
                            , survey.sourceName
                            , survey.sid
                    ));
                    synchronized (ExperimentAction.threadMap) {
                        List<Tuple2<Thread, Runner.BoxedBool>> threadList = ExperimentAction.threadMap.get(survey);
                        for (Tuple2<Thread, Runner.BoxedBool> tupe : threadList) {
                            Thread t = tupe._1();
                            Runner.BoxedBool b = tupe._2();
                            System.out.println(t.getName() + t.getState().name());
                            b.setInterrupt(true);
                        }
                    }
                    try {
                        System.out.println("stuff");
                        Record record = ResponseManager.getRecord(survey);
                        for (HIT hit : record.getAllHITs()) {
                            System.out.println(hit.getHITId());
                            ResponseManager.expireHIT(hit);
                            Experiment.updateStatusLabel("Expired HIT : " + hit.getHITId());
                        }
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

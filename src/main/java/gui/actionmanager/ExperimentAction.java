package gui.actionmanager;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.exception.ServiceException;
import gui.ExperimentActions;
import gui.SurveyMan;
import gui.display.Experiment;
import survey.Survey;
import survey.SurveyException;
import system.Runner;
import system.mturk.HTMLGenerator;
import system.mturk.MturkLibrary;
import system.mturk.ResponseManager;
import system.mturk.SurveyPoster;
import utils.Slurpie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class ExperimentAction implements ActionListener {
    public class BoxedString {
        public String string;
    }
    /* current values from the user */
    public static Map<String, Survey> cachedSurveys = new HashMap<String, Survey>();
    private static Map<String, Component> componentMap = new HashMap<String, Component>();

    public ExperimentActions action;
    public BoxedString filename = new BoxedString();
    final private JFileChooser fc = new JFileChooser();
    final private int previewSize = 300;



    public ExperimentAction(ExperimentActions action) {
        this.action = action;
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setCurrentDirectory(new File("."));
        fc.addActionListener(new FileListener(fc, filename));
    }

    public void registerComponent(String name, Component component) {
        componentMap.put(name, component);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        switch (action) {
            case LOAD_SPLASH:
                loadSplashPage(); break;
            case SELECT_CSV:
                selectCSVFile(); break;
            case PREVIEW_CSV:
                previewCSV(); break;
            case PREVIEW_HIT:
                openPreviewHTML(); break;
            case DUMP_PARAMS:
                try {
                    Experiment.loadParameters();
                    saveParameters();
                } catch (IOException io) {
                    Experiment.updateStatusLabel(String.format("Unable to write parameter file %s : %s"
                            , MturkLibrary.PARAMS
                            , io.getMessage()));
                    SurveyMan.LOGGER.warn(io);
                }
                break;
            case VIEW_HIT:
                openViewHIT(); break;
            case SEND_SURVEY:
                sendSurvey(); break;
            case VIEW_RESULTS:
                viewResults(); break;
        }
    }

    private void loadSplashPage() {
        JButton splashLoaderButton = (JButton) componentMap.get("splashLoaderButton");
        JComboBox splashLoadOpt = (JComboBox) componentMap.get("splashLoadOpt");
        JTextArea splashPage = (JTextArea) componentMap.get("splashPage");
        fc.showOpenDialog(splashLoaderButton);
        if (filename.string!=null) {
            if (splashLoadOpt.getSelectedIndex()==0)
                splashPage.setText(filename.string);
            else {
                try {
                    splashPage.setText(Slurpie.slurp(filename.string));
                } catch (IOException e) {
                    SurveyMan.LOGGER.fatal(e);
                }
            }
        }
    }

    private void selectCSVFile(){
        JButton selectCSV = (JButton) componentMap.get("selectCSV");
        JComboBox csvLabel = (JComboBox) componentMap.get("csvLabel");
        if (fc.showOpenDialog(selectCSV)==JFileChooser.APPROVE_OPTION) {
            // check whether
            for (int i = 0 ; i <csvLabel.getItemCount(); i++)
                if (((String) csvLabel.getItemAt(i)).equals(filename.string)) {
                    csvLabel.setSelectedItem(filename.string);
                    return;
                }
            // redisplay
            csvLabel.addItem(filename.string);
            csvLabel.setSelectedItem(filename.string);
        }
    }

    private void previewCSV(){
        JComboBox csvLabel = (JComboBox) componentMap.get("csvLabel");
        if (csvLabel!=null) {
            try{
                Experiment.updateStatusLabel(Slurpie.slurp(((String) csvLabel.getSelectedItem()), previewSize).substring(0, previewSize)+"...");
           } catch (IOException io) {
                SurveyMan.LOGGER.warn(io);
                Experiment.updateStatusLabel(io.getMessage());
            }
        }
    }

    private void viewResults(){
        JComboBox csvLabel = (JComboBox) componentMap.get("csvLabel");
        if (csvLabel!=null){
            // grab the results file corresponding to this csv
            String csv = (String) csvLabel.getSelectedItem();
            System.out.println(csv);
            Survey survey = cachedSurveys.get(csv);
            if (survey!=null) {
                try{
                    ResponseManager.Record record = ResponseManager.getRecord(survey);
                    if (record!=null) {
                        Experiment.updateStatusLabel("Results in file "+record.outputFileName);
                        Experiment.updateStatusLabel(Slurpie.slurp(record.outputFileName));
                   }
                }catch(IOException io) {
                    SurveyMan.LOGGER.warn(io);
                    Experiment.updateStatusLabel(io.getMessage());
                }
            }
        }
    }

    public static void saveParameters() throws IOException {
        FileWriter writer = new FileWriter(MturkLibrary.PARAMS);
        MturkLibrary.props.store(writer, "");
        writer.close();
    }

    private void openPreviewHTML(){
        JComboBox csvLabel = (JComboBox) componentMap.get("csvLabel");
        if (csvLabel!=null) {
            String csv = (String) csvLabel.getSelectedItem();
            try{
                Survey survey;
                if (cachedSurveys.containsKey(csv)) {
                    Experiment.loadParameters();
                SurveyPoster.updateProperties();
                survey = cachedSurveys.get(csv);
                } else {
                    survey = Experiment.makeSurvey();
                    cachedSurveys.put(csv, survey);
                }
                HTMLGenerator.spitHTMLToFile(HTMLGenerator.getHTMLString(survey), survey);
                Desktop.getDesktop().browse(new URI("file://"+HTMLGenerator.htmlFileName));
            } catch (IOException io) {
                Experiment.updateStatusLabel(String.format("IO Exception when opening file %s", HTMLGenerator.htmlFileName));
                SurveyMan.LOGGER.fatal(io);
            } catch (SurveyException se) {
                Experiment.updateStatusLabel(se.getMessage());
                cachedSurveys.remove(csv);
                SurveyMan.LOGGER.warn(se);
            } catch (URISyntaxException uri) {
                SurveyMan.LOGGER.fatal(uri);
            }
        }
    }

    private void openViewHIT() {
        if (!SurveyPoster.hitURL.equals("")) {
            try {
                Desktop.getDesktop().browse(new URI(SurveyPoster.hitURL));
            } catch (URISyntaxException urise) {
                SurveyMan.LOGGER.warn(urise);
            } catch (IOException ioe) {
                SurveyMan.LOGGER.warn(ioe);
            }
        }
    }

    private void sendSurvey() {
        // need to register survey with Response Manager.

        final Survey survey;
        final Thread runner, writer, notifier;

        try{
            JComboBox csvLabel = (JComboBox) componentMap.get("csvLabel");
            // if we've made this survey before, grab it
            if (csvLabel!=null) {
                String csv = (String) csvLabel.getSelectedItem();
                if (cachedSurveys.containsKey(csv))
                    survey = cachedSurveys.get(csv);
                else {
                    survey = Experiment.makeSurvey();
                    cachedSurveys.put(csv, survey);
                }
            } else survey=null;

            runner = new Thread() {
                public void run() {
                    boolean done = false;
                    while(!done) {
                        try{
                            Runner.run(survey);
                            done=true;
                        } catch (SurveyException se) {
                            SurveyMan.LOGGER.warn(se);
                            Experiment.updateStatusLabel(String.format("%s\r\nSee SurveyMan.log for more detail.", se.getMessage()));
                            done=true;
                        } catch (ServiceException mturkse) {
                            SurveyMan.LOGGER.warn(mturkse);
                            Experiment.updateStatusLabel(String.format("Could not send request:\r\n%s\r\nSee SurveyMan.log for more detail.", mturkse.getMessage()));
                        } catch (IOException io) {
                            SurveyMan.LOGGER.warn(io);
                            Experiment.updateStatusLabel(String.format("%s\r\nSee SurveyMan.log for more detail.", io.getMessage()));
                            done=true;
                        }
                    }
                }
            };

            writer = new Thread() {
                public void run() {
                    boolean notJoined = true;
                    try {
                        while (true) {
                            ResponseManager.Record record = ResponseManager.getRecord(survey);
                            if (record!=null) {
                                Runner.writeResponses(survey);
                                // need to rethink this:
                                if (!runner.isAlive() || !Runner.stillLive(survey)) {
                                    Experiment.updateStatusLabel(String.format("Survey %s completed with %d responses. See file %s for results."
                                            , survey.sourceName
                                            , record.responses.size()
                                            , record.outputFileName)
                                    );
                                    // remove from cached surveys. when this is re-run, it will generate a new survey and a new results file
                                    cachedSurveys.remove(survey);
                                    break;
                                }
                                try {
                                    sleep(Runner.waitTime);
                                } catch (InterruptedException ie) {
                                    SurveyMan.LOGGER.warn(ie);
                                }
                            }
                        }
                    } catch (IOException io) {
                        SurveyMan.LOGGER.warn(io);
                        Experiment.updateStatusLabel(String.format("IOException caused experiment (%s, %s) to fail. Expiring associated HITs. Please try again."
                            , survey.sid
                            , survey.sourceName)
                        );
                        // if something goes wrong in recording results, kill this job
                        boolean success = false;
                        while (! success) {
                            try{
                                runner.join();
                                ResponseManager.Record record = ResponseManager.getRecord(survey);
                                for (HIT hit : record.getAllHITs())
                                    ResponseManager.expireHIT(hit);
                                success = true;
                            } catch (InterruptedException ie) {
                                SurveyMan.LOGGER.warn(ie);
                            } catch (IOException ioe) {
                                SurveyMan.LOGGER.warn(ioe);
                            }
                        }
                    }
                }
            };

            notifier = new Thread() {
                public void run() {
                    Map<String, HIT> hitsNotified = new HashMap<String, HIT>();
                    Experiment.updateStatusLabel(String.format("Sending Survey %s to MTurk...", survey.sourceName));
                    long waitTime = 1000;
                    while(runner.isAlive()) {
                        try{
                            while (ResponseManager.getRecord(survey)==null) {
                                try {
                                    sleep(waitTime);
                                } catch (InterruptedException e) {
                                    SurveyMan.LOGGER.warn(e);
                                }
                            }

                            while (ResponseManager.getRecord(survey).getLastHIT()==null) {
                                try{
                                    sleep(waitTime);
                                } catch (InterruptedException e) {
                                    SurveyMan.LOGGER.warn(e);
                                }
                            }

                            ResponseManager.Record record = ResponseManager.getRecord(survey);
                            HIT hit = record.getLastHIT();
                            if (! hitsNotified.containsKey(hit.getHITId())) {
                                hitsNotified.put(hit.getHITId(), hit);
                                Experiment.updateStatusLabel(String.format("Most recent HIT %s for survey %s. To view, press 'View HIT'."
                                    , hit.getHITId()
                                    , survey.sourceName)
                                );
                            } else waitTime = waitTime*(long)1.5;
                        } catch (IOException io) {
                            SurveyMan.LOGGER.warn(io);
                        }
                    }
                }
            };

            if (survey!=null) {
                runner.setPriority(Thread.MIN_PRIORITY);
                writer.setPriority(Thread.MIN_PRIORITY);
                notifier.setPriority(Thread.MIN_PRIORITY);
                runner.start();
                writer.start();
                notifier.start();
            }

            } catch (IOException e) {
               SurveyMan.LOGGER.warn(e);
                Experiment.updateStatusLabel(e.getMessage());
            } catch (SurveyException se) {
                SurveyMan.LOGGER.warn(se);
                Experiment.updateStatusLabel(String.format("%s\r\nSee SurveyMan.log for more detail.", se.getMessage()));
            } catch (ServiceException mturkse) {
                SurveyMan.LOGGER.warn(mturkse.getMessage());
                Experiment.updateStatusLabel(String.format("Could not send request:\r\n%s\r\nSee SurveyMan.log for more detail.", mturkse.getMessage()));
            }
        }
}

class FileListener implements ActionListener {
    public ExperimentAction.BoxedString filename;
    final private JFileChooser fc;

    public FileListener(final JFileChooser fc, ExperimentAction.BoxedString filename) {
        this.fc = fc;
        this.filename = filename;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getActionCommand().equals(JFileChooser.APPROVE_SELECTION))
            try {
                filename.string = fc.getSelectedFile().getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
    }
}
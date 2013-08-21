package gui.actionmanager;

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

/**
 * Created with IntelliJ IDEA.
 * User: etosch
 * Date: 8/21/13
 * Time: 10:38 AM
 * To change this template use File | Settings | File Templates.
 */
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
        JLabel csvLabel = (JLabel) componentMap.get("csvLabel");
        if (fc.showOpenDialog(selectCSV)==JFileChooser.APPROVE_OPTION)
            // redisplay
            csvLabel.setText(filename.string);
    }

    private void previewCSV(){
        JLabel csvLabel = (JLabel) componentMap.get("csvLabel");
        if (csvLabel!=null) {
            try{
                Experiment.updateStatusLabel(Slurpie.slurp(csvLabel.getText()).substring(0,500)+"...");
            }catch(Exception e){
                SurveyMan.LOGGER.warn(e);
            }
        }
    }

    public static void saveParameters() throws IOException {
        FileWriter writer = new FileWriter(MturkLibrary.PARAMS);
        MturkLibrary.props.store(writer, "");
        writer.close();
    }

    private void openPreviewHTML(){
        JLabel csvLabel = (JLabel) componentMap.get("csvLabel");
        if (csvLabel!=null) {
            String csv = csvLabel.getText();
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
        try {
            final Survey survey = Experiment.makeSurvey();
            final Thread runner = new Thread() {
                public void run() {
                    try{
                        Runner.run(survey);
                    } catch (SurveyException se) {
                        // pop up some kind of alert
                        SurveyMan.LOGGER.warn(se);
                        Experiment.updateStatusLabel(String.format("%s\r\nSee SurveyMan.log for more detail.", se.getMessage()));
                        boolean notJoined  = true;
                        while(notJoined) {
                            try{
                                this.join();
                                notJoined = false;
                            }catch(InterruptedException ie) {
                                SurveyMan.LOGGER.warn(ie);
                            }
                        }
                    } catch (ServiceException mturkse) {
                        SurveyMan.LOGGER.warn(mturkse);
                        Experiment.updateStatusLabel(String.format("Could not send request:\r\n%s\r\nSee SurveyMan.log for more detail.", mturkse.getMessage()));
                    }
                }
            };
            final Thread waiter = new Thread() {
                public void run() {
                    while (true) {
                        try {
                            Runner.writeResponses(survey);
                        } catch (IOException io) {
                            SurveyMan.LOGGER.warn(io);
                        }
                        // need to rethink this:
                        if (! (runner.isAlive() && ResponseManager.hasJobs())) {
                            Experiment.updateStatusLabel(String.format("Survey %s completed with %d responses.", survey.sourceName, ResponseManager.manager.get(survey).responses.size()));
                            break;
                        }
                        try {
                            Thread.sleep(Runner.waitTime);
                        } catch (InterruptedException ie) {
                            SurveyMan.LOGGER.warn(ie);
                        }
                    }
                    boolean notJoined = true;
                    while (notJoined) {
                        try{
                            this.join();
                            notJoined = false;
                        }catch (InterruptedException ie) {
                            SurveyMan.LOGGER.warn(ie);
                        }
                    }
                }
            };
            runner.setPriority(Thread.MIN_PRIORITY);
            waiter.setPriority(Thread.MIN_PRIORITY);
            runner.start();
            waiter.start();
        } catch (IOException e) {
           SurveyMan.LOGGER.warn(e);
        } catch (SurveyException se) {
            SurveyMan.LOGGER.warn(se);
            Experiment.updateStatusLabel(String.format("%s\r\nSee SurveyMan.log for more detail.", se.getMessage()));
        } catch (ServiceException mturkse) {
            SurveyMan.LOGGER.warn(mturkse);
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
            filename.string = fc.getSelectedFile().getAbsolutePath();
    }
}
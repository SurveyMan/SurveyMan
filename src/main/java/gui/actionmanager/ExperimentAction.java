package gui.actionmanager;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.exception.AccessKeyException;
import com.amazonaws.mturk.service.exception.InsufficientFundsException;
import com.amazonaws.mturk.service.exception.ServiceException;
import gui.GUIActions;
import gui.SurveyMan;
import gui.display.Display;
import gui.display.Experiment;
import scala.Tuple2;
import survey.Survey;
import survey.SurveyException;
import system.mturk.Runner;
import system.mturk.*;
import system.mturk.generators.HTML;
import system.Slurpie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import java.util.List;

public class ExperimentAction implements ActionListener {


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
                    e.printStackTrace();
                }
        }
    }

    public class BoxedString {
        public String string;
    }

    public static Map<String, Survey> cachedSurveys = new HashMap<String, Survey>();
    public static Map<Survey, List<Tuple2<Thread, Runner.BoxedBool>>> threadMap = new HashMap<Survey, List<Tuple2<Thread, Runner.BoxedBool>>>();
    public GUIActions action;
    public BoxedString filename = new BoxedString();

    final private JFileChooser fc = new JFileChooser();
    final private int previewSize = 300;

    public ExperimentAction(GUIActions action) {
        this.action = action;
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setCurrentDirectory(new File("."));
        fc.addActionListener(new FileListener(fc, filename));
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try{
            switch (action) {
                case LOAD_SPLASH_FROM_FILE:
                    loadSplashPage(GUIActions.LOAD_SPLASH_FROM_FILE); break;
                case LOAD_SPLASH_FROM_URL:
                    loadSplashPage(GUIActions.LOAD_SPLASH_FROM_URL); break;
                case SELECT_CSV:
                    selectCSVFile(); break;
                case PREVIEW_CSV:
                    previewCSV(); break;
                case PREVIEW_HIT:
                    openPreviewHTML(); break;
                case DUMP_PARAMS:
                    try {
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
                case UPDATE_FORMATTER_DURATION:
                    updateFormatter(Experiment.duration, Experiment.duration_units); break;
                case UPDATE_FORMATTER_LIFETIME:
                    updateFormatter(Experiment.lifetime, Experiment.lifetime_units); break;
            }
            //Display.frame.setVisible(true);
        } catch (AccessKeyException ake) {
            Experiment.updateStatusLabel(String.format("Access key issue : %s. Deleting access keys in your surveyman home folder. Please restart this program.", ake.getMessage()));
            (new File(MturkLibrary.CONFIG)).delete();
            SurveyMan.LOGGER.fatal(ake);
        } catch (IOException op) {
            Experiment.updateStatusLabel(op.getLocalizedMessage());
            SurveyMan.LOGGER.warn(op.getStackTrace().toString());
        }
    }

    private void updateFormatter(JFormattedTextField txtField, JComboBox selections) {
    }

    private void loadSplashPage(GUIActions splashAction) {
        if ( splashAction == GUIActions.LOAD_SPLASH_FROM_FILE ) {
            fc.showOpenDialog(Experiment.splashLoadFromFile);
            if (filename.string!=null) {
                try {
                    Experiment.splashPage.setText(Slurpie.slurp(filename.string));
                } catch (IOException e) {
                    SurveyMan.LOGGER.fatal(e);
                }
            }
        } else if ( splashAction == GUIActions.LOAD_SPLASH_FROM_URL ) {
            String txt = Experiment.splashPage.getText();
            String newTxt = "";
            try {
                newTxt = Slurpie.slurp(txt);
            } catch (IOException io) { }
            try {
                URL splash = new URL(txt);
                BufferedReader br = new BufferedReader(new InputStreamReader(splash.openStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    newTxt = newTxt + line;
                }
                br.close();
            } catch (MalformedURLException e) {
            } catch (IOException io) {
            }
            if (newTxt.equals(""))
                JOptionPane.showMessageDialog(Display.frame, new JLabel("Content of the textbox is not a valid URI."));
            else
                Experiment.splashPage.setText(newTxt);
        }
    }

    private void selectCSVFile(){
        if (fc.showOpenDialog(Experiment.selectCSV)==JFileChooser.APPROVE_OPTION) {
            // check whether it's already added. if so, remove from map and set as selected
            for (int i = 0 ; i < Experiment.csvLabel.getItemCount(); i++)
                if (Experiment.csvLabel.getItemAt(i).equals(filename.string)) {
                    Experiment.csvLabel.setSelectedItem(filename.string);
                    ExperimentAction.cachedSurveys.remove(filename.string);
                    return;
                }
            // redisplay
            Experiment.csvLabel.addItem(filename.string);
            Experiment.csvLabel.setSelectedItem(filename.string);
        }
    }

    private void previewCSV(){
        if (Experiment.csvLabel!=null) {
            try{
                Experiment.updateStatusLabel(Slurpie.slurp(((String) Experiment.csvLabel.getSelectedItem()), previewSize).substring(0, previewSize)+"...");
           } catch (IOException io) {
                SurveyMan.LOGGER.warn(io);
                Experiment.updateStatusLabel(io.getMessage());
            }
        }
    }

    private void viewResults(){
        if (Experiment.csvLabel!=null){
            // grab the results file corresponding to this csv
            String csv = (String) Experiment.csvLabel.getSelectedItem();
            System.out.println(csv);
            Survey survey = cachedSurveys.get(csv);
            if (survey!=null) {
                try{
                    Record record = ResponseManager.getRecord(survey);
                    if (record!=null) {
                        Experiment.updateStatusLabel("Results in file "+record.outputFileName);
                        Experiment.updateStatusLabel(Slurpie.slurp(record.outputFileName));
                   }
                }catch(IOException io) {
                    SurveyMan.LOGGER.warn(io);
                    Experiment.updateStatusLabel(io.getMessage());
                } catch (SurveyException ex) {
                    SurveyMan.LOGGER.warn(ex);
                }
            }
        }
    }

    public static void saveParameters() throws IOException {
        FileWriter writer = new FileWriter(MturkLibrary.PARAMS);
        Properties props = new Properties();
        props.store(writer, "");
        writer.close();
    }

    private void openPreviewHTML(){
        if (Experiment.csvLabel!=null) {
            String csv = (String) Experiment.csvLabel.getSelectedItem();
            String htmlFileName = "NOT SET";
            try{
                Survey survey;
                Record record;
                if (cachedSurveys.containsKey(csv)) {
                    survey = cachedSurveys.get(csv);
                    if (ResponseManager.manager.containsKey(survey.sid))
                        record = ResponseManager.getRecord(survey);
                    else record = new Record(survey);
                } else {
                    record = Experiment.makeSurvey();
                    survey = record.survey;
                    cachedSurveys.put(csv, survey);
                }
                survey.randomize();
                Experiment.loadParameters(record);
                if (!ResponseManager.manager.containsKey(survey.sid))
                    ResponseManager.manager.put(survey.sid, record);
                HTML.spitHTMLToFile(HTML.getHTMLString(survey), survey);
                htmlFileName = record.getHtmlFileName();
                SurveyMan.LOGGER.info(String.format("Attempting to open file (%s)", htmlFileName));
                Desktop.getDesktop().open(new File(htmlFileName));
            } catch (IOException io) {
                Experiment.updateStatusLabel(String.format("IO Exception when opening file %s", htmlFileName));
                io.printStackTrace();
                SurveyMan.LOGGER.fatal(io);
            } catch (SurveyException se) {
                Experiment.updateStatusLabel(se.getMessage());
                cachedSurveys.remove(csv);
                SurveyMan.LOGGER.warn(se);
            }
        }
    }

    private void openViewHIT() throws IOException {
        try {
            Survey selectedSurvey = cachedSurveys.get(Experiment.csvLabel.getSelectedItem());
            Record record = ResponseManager.getRecord(selectedSurvey);
            HIT hit = record.getLastHIT();
            String hitURL = SurveyPoster.makeHITURL(hit);
            if (!hitURL.equals("")) {
                try {
                    Desktop.getDesktop().browse(new URI(hitURL));
                } catch (URISyntaxException urise) {
                    SurveyMan.LOGGER.warn(urise);
                } catch (IOException ioe) {
                    SurveyMan.LOGGER.warn(ioe);
                }
            }
        } catch (SurveyException ex) {
            SurveyMan.LOGGER.warn(ex);
        }
    }

    public static void addThisThread(Survey survey, Tuple2<Thread, Runner.BoxedBool> thread) {
        synchronized (threadMap) {
            if (threadMap.containsKey(survey))
                threadMap.get(survey).add(thread);
            else {
                threadMap.put(survey, new ArrayList<Tuple2<Thread, Runner.BoxedBool>>());
                threadMap.get(survey).add(thread);
            }
        }
    }

    public static void removeThisThread(Survey survey, Tuple2<Thread, Runner.BoxedBool> threadData) {
        synchronized (threadMap) {
            threadMap.get(survey).remove(threadData);
            if (threadMap.get(survey).isEmpty())
                threadMap.remove(survey);
        }
    }

    public Thread makeRunner(final Record record, final Runner.BoxedBool interrupt){
        return new Thread(){
            @Override
            public void run() {
                Tuple2 threadData = new Tuple2(this, interrupt);
                Survey survey = record.survey;
                ExperimentAction.addThisThread(survey, threadData);
                while (!interrupt.getInterrupt()) {
                    try{
                        Runner.run(record, interrupt);
                        System.out.println("finished survey.");
                    } catch (ParseException pe) {
                        SurveyMan.LOGGER.fatal(pe);
                        System.exit(-1);
                    } catch (AccessKeyException ake) {
                        Experiment.updateStatusLabel(String.format("Access key issue : %s. Deleting access keys in your surveyman home folder. Please restart this program.", ake.getMessage()));
                        (new File(MturkLibrary.CONFIG)).delete();
                        SurveyMan.LOGGER.fatal(ake);
                        System.exit(-1);
                    } catch (SurveyException se) {
                        SurveyMan.LOGGER.warn(se);
                        Experiment.updateStatusLabel(String.format("%s\r\nSee SurveyMan.log for more detail.", se.getMessage()));
                        interrupt.setInterrupt(true);
                    } catch (InsufficientFundsException ife) {
                        SurveyMan.LOGGER.warn(ife);
                        int opt = JOptionPane.showConfirmDialog(Display.frame, "Your account has run out of money. Would you like to add more and continue?");
                        if (opt == JOptionPane.NO_OPTION) {
                            Experiment.updateStatusLabel(String.format("Cancelling survey %s", survey.sourceName));
                            interrupt.setInterrupt(true);
                        } else if (opt == JOptionPane.CANCEL_OPTION) {
                            Experiment.updateStatusLabel(String.format("Saving survey %s and stopping computation.", survey.sourceName));
//                                Runner.saveJob(survey, MturkLibrary.JobStatus.INTERRUPTED);
                            interrupt.setInterrupt(true);
                        }
                    } catch (ServiceException mturkse) {
                        SurveyMan.LOGGER.warn(mturkse);
                        Experiment.updateStatusLabel(String.format("Could not send request:\r\n%s\r\nSee SurveyMan.log for more detail.", mturkse.getMessage()));
                        mturkse.printStackTrace();
                    } catch (IOException io) {
                        SurveyMan.LOGGER.warn(io);
                        Experiment.updateStatusLabel(String.format("%s\r\nSee SurveyMan.log for more detail.", io.getMessage()));
                        interrupt.setInterrupt(true);
                    }
                }
                ExperimentAction.removeThisThread(survey, threadData);
            }
        };
    }

    public Thread makeNotifier(final Thread runner, final Survey survey){
        return new Thread() {
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

                        Record record = ResponseManager.getRecord(survey);
                        HIT hit = record.getLastHIT();
                        if (! hitsNotified.containsKey(hit.getHITId())) {
                            hitsNotified.put(hit.getHITId(), hit);
                            Experiment.updateStatusLabel(String.format("Most recent HIT %s for survey %s. To view, press 'View HIT'."
                                    , hit.getHITId()
                                    , survey.sourceName)
                            );
                        } else waitTime = waitTime*(long)1.5;

                    } catch (AccessKeyException ake) {
                        Experiment.updateStatusLabel(String.format("Access key issue : %s. Deleting access keys in your surveyman home folder. Please restart this program.", ake.getMessage()));
                        (new File(MturkLibrary.CONFIG)).delete();
                        SurveyMan.LOGGER.fatal(ake);
                        System.exit(-1);
                    } catch (IOException io) {
                        SurveyMan.LOGGER.warn(io);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                Record record = null;
                try {
                    record = ResponseManager.getRecord(survey);
                } catch (IOException e) {
                    SurveyMan.LOGGER.fatal(e);
                    e.printStackTrace();
                    System.exit(-1);
                } catch (SurveyException ex) {
                    SurveyMan.LOGGER.warn(ex);
                }
                if (record.qc.complete(record.responses, record.library.props))
                    Experiment.updateStatusLabel(String.format("Survey completed with %d responses. See %s for output."
                            , record.responses.size()
                            , record.outputFileName));
                else Experiment.updateStatusLabel(String.format("Survey terminated prematurely with %d responses, %s shy of the objective. See %s for output."
                        , record.responses.size()
                        , record.library.props.getProperty("numparticipants")
                        , record.outputFileName));

            }
        };
    }

    public void sendSurvey() {

        final Survey survey;
        final Record record;
        final Thread runner, notifier, writer, getter;

        try{
            // if we've made this survey before, grab it
            if (Experiment.csvLabel!=null) {
                String csv = (String) Experiment.csvLabel.getSelectedItem();
                if (csv==null || csv.equals("")) {
                    survey = null;
                    record = null;
                } else {
                    if (cachedSurveys.containsKey(csv)) {
                        survey = cachedSurveys.get(csv);
                        record = ResponseManager.getRecord(survey);
                    } else {
                        record = Experiment.makeSurvey();
                        survey = record.survey;
                        cachedSurveys.put(csv, survey);
                        ResponseManager.manager.put(survey.sid, record);
                    }
                }
            } else {
                survey=null; record=null;
            }

            Runner.BoxedBool interrupt = new Runner.BoxedBool(false);
            runner = makeRunner(record, interrupt);
            notifier = makeNotifier(runner, survey);
            getter = Runner.makeResponseGetter(survey, interrupt);
            writer = Runner.makeWriter(survey, interrupt);

            if (survey!=null) {
                Experiment.loadParameters(record);
                runner.start();
                getter.start();
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

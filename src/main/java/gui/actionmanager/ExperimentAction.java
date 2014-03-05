package gui.actionmanager;

import com.amazonaws.mturk.addon.XhtmlValidator;
import com.amazonaws.mturk.service.exception.AccessKeyException;
import com.amazonaws.mturk.service.exception.InsufficientFundsException;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.service.exception.ValidationException;
import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import gui.GUIActions;
import gui.SurveyMan;
import gui.display.Display;
import gui.display.Experiment;
import survey.Survey;
import survey.SurveyException;
import system.*;
import system.interfaces.ResponseManager;
import system.interfaces.Task;
import system.localhost.LocalLibrary;
import system.localhost.LocalSurveyPoster;
import system.localhost.Server;
import system.mturk.*;
import system.generators.HTML;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class ExperimentAction implements ActionListener {

    static class ThreadBoolTuple {
        public Thread t;
        public Runner.BoxedBool boxedBool;
        public ThreadBoolTuple(Thread t, Runner.BoxedBool b) {
            this.t = t; this.boxedBool = b;
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
                    e.printStackTrace();
                }
        }
    }

    public class BoxedString {
        public String string;
    }

    public static Map<String, Survey> cachedSurveys = new HashMap<String, Survey>();
    public static Map<Survey, List<ThreadBoolTuple>> threadMap = new HashMap<Survey, List<ThreadBoolTuple>>();
    public GUIActions action;
    public BoxedString filename = new BoxedString();

    final private JFileChooser fc = new JFileChooser();
    final private int previewSize = 300;
    public static BackendType backendType = BackendType.LOCALHOST;
    public static Thread localserver;
    static {
        try {
            localserver = Server.startServe();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }


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
                    try {
                        openPreviewHTML();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case DUMP_PARAMS:
                    try {
                        saveParameters();
                    } catch (IOException io) {
                        Experiment.updateStatusLabel(String.format("Unable to write parameter file %s : %s"
                                , MturkLibrary.PARAMS
                                , io.getMessage()));
                        SurveyMan.LOGGER.warn(io);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    } catch (ValidationException e) {
                        e.printStackTrace();
                    }
                    break;
                case VIEW_HIT:
                    try {
                        openViewHIT();
                    } catch (SurveyException e) {
                        e.printStackTrace();
                    }
                    break;
                case SEND_MTURK:
                    backendType = BackendType.MTURK;
                    if (new File(MturkLibrary.CONFIG).exists())
                        sendSurvey(BackendType.MTURK);
                    else
                        Experiment.updateStatusLabel("No ~/surveyman/mturk_config file set up for use with Mechanical Turk. See https://github.com/etosch/SurveyMan/wiki/Getting-started-on-Mechanical-Turk");
                    break;
                case SEND_LOCAL:
                    backendType = BackendType.LOCALHOST;
                    sendSurvey(BackendType.LOCALHOST); break;
                case PRINT_SURVEY:
                    printSurvey(); break;
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
        } finally {

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
                    Record record = MturkResponseManager.getRecord(survey);
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

    public Properties getParameters()
            throws IOException, ParseException, ValidationException {
        // this should really save whatever's in the GUI
        Locale locale = new Locale("en", "US");
        Properties props = new Properties();
        props.setProperty("title", Experiment.title.getText());
        props.setProperty("description", Experiment.description.getText());
        props.setProperty("keywords", Experiment.kwds.getText());
        // validate and compress the splash page
        String splash = Experiment.splashPage.getText();
        String validated = XhtmlValidator.validateAndClean(splash);
        HtmlCompressor compressor = new HtmlCompressor();
        String compressed = compressor.compress(validated);
        props.setProperty("splashpage", compressed);
        NumberFormat cf = NumberFormat.getCurrencyInstance(locale);
        props.setProperty("reward", Double.toString(cf.parse(Experiment.reward.getText()).doubleValue()));
        NumberFormat f = NumberFormat.getNumberInstance(locale);
        props.setProperty("assignmentduration",
                String.valueOf((f.parse(Experiment.duration.getText())).longValue()
                        * ((long) Experiment.conversion[Experiment.duration_units.getSelectedIndex()])));
        props.setProperty("hitlifetime",
                String.valueOf((NumberFormat.getNumberInstance().parse(Experiment.lifetime.getText())).longValue()
                        * (long) Experiment.conversion[Experiment.lifetime_units.getSelectedIndex()]));
        props.setProperty("numparticipants", Integer.toString(f.parse(Experiment.participants.getText()).intValue()));
        props.setProperty("sandbox", (String) Experiment.sandbox.getSelectedItem());
        return props;
    }

    public void saveParameters() throws ParseException, IOException, ValidationException {
        Properties props = getParameters();
        FileWriter writer = new FileWriter(MturkLibrary.PARAMS);
        props.store(writer, "");
        writer.close();
    }

    public void updateProperties(Record r) throws ParseException, IOException, ValidationException {
        Properties props = getParameters();
        for (String s : props.stringPropertyNames()) {
            r.library.props.setProperty(s, (String) props.get(s));
        }
    }

    private void printSurvey() {
        System.err.println("printSurvey not yet implemented.");
    }

    public static Library getBackendLibClass() {
        switch (backendType) {
            case MTURK:
                return new MturkLibrary();
            case LOCALHOST:
                return new LocalLibrary();
            default:
                return new LocalLibrary();
        }
    }

    private void openPreviewHTML() throws InterruptedException {
        if (!(Experiment.csvLabel.getSelectedItem()==null || ((String) Experiment.csvLabel.getSelectedItem()).equals(""))) {
            String csv = (String) Experiment.csvLabel.getSelectedItem();
            String htmlFileName = "NOT SET";
            try{
                Survey survey;
                Record record;
                if (cachedSurveys.containsKey(csv)) {
                    survey = cachedSurveys.get(csv);
                    if (MturkResponseManager.existsRecordForSurvey(survey))
                        record = MturkResponseManager.getRecord(survey);
                    else record = new Record(survey, getBackendLibClass(), BackendType.LOCALHOST);
                } else {
                    record = Experiment.makeSurvey(BackendType.LOCALHOST);
                    survey = record.survey;
                    cachedSurveys.put(csv, survey);
                }
                //survey.randomize();
                Experiment.loadParameters(record);
                if (!MturkResponseManager.existsRecordForSurvey(survey))
                    MturkResponseManager.putRecord(survey, record);
                System.out.println("Server thread running");
                HTML.spitHTMLToFile(HTML.getHTMLString(survey, new system.localhost.generators.HTML()), survey);
                String[] pieces = record.getHtmlFileName().split(Pattern.quote(Library.fileSep));
                htmlFileName = system.localhost.generators.HTML.prefix + "/logs/" + pieces[pieces.length - 1];
                SurveyMan.LOGGER.info(String.format("Attempting to open file (%s)", htmlFileName));
                Desktop.getDesktop().browse(new URI(htmlFileName));
            } catch (IOException io) {
                Experiment.updateStatusLabel(String.format("IO Exception when opening file %s", htmlFileName));
                io.printStackTrace();
                SurveyMan.LOGGER.fatal(io);
            } catch (SurveyException se) {
                Experiment.updateStatusLabel(se.getMessage());
                cachedSurveys.remove(csv);
                SurveyMan.LOGGER.warn(se);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void openViewHIT() throws IOException, SurveyException {
        try {
            Survey selectedSurvey = cachedSurveys.get(Experiment.csvLabel.getSelectedItem());
            Record record = ResponseManager.getRecord(selectedSurvey);
            String hitURL = "";
            switch (backendType) {
                case MTURK:
                    MturkTask hit = (MturkTask) record.getLastTask();
                    hitURL = new MturkSurveyPoster().makeTaskURL(hit);
                    break;
                case LOCALHOST:
                    Task task = record.getLastTask();
                    hitURL = new LocalSurveyPoster().makeTaskURL(task);
                    break;
            }
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
            Experiment.updateStatusLabel(ex.getLocalizedMessage());
            SurveyMan.LOGGER.warn(ex);
        }
    }

    public static void addThisThread(Survey survey, ThreadBoolTuple thread) {
        synchronized (threadMap) {
            if (threadMap.containsKey(survey))
                threadMap.get(survey).add(thread);
            else {
                threadMap.put(survey, new ArrayList<ThreadBoolTuple>());
                threadMap.get(survey).add(thread);
            }
        }
    }

    public static void removeThisThread(Survey survey, ThreadBoolTuple threadData) {
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
                ThreadBoolTuple threadData = new ThreadBoolTuple(this, interrupt);
                Survey survey = record.survey;
                ExperimentAction.addThisThread(survey, threadData);
                while (!interrupt.getInterrupt()) {
                    try{
                        Runner.run(record, interrupt, backendType);
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
                cachedSurveys.remove(survey.source);
            }
        };
    }

    public Thread makeNotifier(final Thread runner, final Survey survey, final BackendType backend){
        return new Thread() {
            public void run() {
                Map<String, Task> hitsNotified = new HashMap<String, Task>();
                Experiment.updateStatusLabel(String.format("Sending Survey %s to %s...", survey.sourceName, backend));
                long waitTime = 1000;
                Record record = null;
                while(runner.isAlive()) {
                    try{
                        while (!ResponseManager.existsTaskForRecord(survey)) {
                            try {
                                sleep(waitTime);
                            } catch (InterruptedException e) {
                                SurveyMan.LOGGER.warn(e);
                            }
                        }
                        record = ResponseManager.getRecord(survey);
                        if (record==null)
                            break;
                        Task hit = record.getLastTask();
                        if (! hitsNotified.containsKey(hit.getTaskId())) {
                            hitsNotified.put(hit.getTaskId(), hit);
                            Experiment.updateStatusLabel(String.format("Most recent Task %s for survey %s. To view, press 'View HIT'."
                                    , hit.getTaskId()
                                    , survey.sourceName)
                            );
                        } else waitTime = waitTime*(long)1.5;

                        record = ResponseManager.getRecord(survey);

                    } catch (AccessKeyException ake) {
                        Experiment.updateStatusLabel(String.format("Access key issue : %s. Deleting access keys in your surveyman home folder. Please restart this program.", ake.getMessage()));
                        (new File(MturkLibrary.CONFIG)).delete();
                        SurveyMan.LOGGER.fatal(ake);
                        System.exit(-1);
                    } catch (IOException io) {
                        SurveyMan.LOGGER.warn(io);
                    } catch (NullPointerException npe){
                        npe.printStackTrace();
                    } catch (SurveyException se) {
                        SurveyMan.LOGGER.warn(se);
                    }
                }
                try {
                    if (ResponseManager.getRecord(survey)!=null && record.qc.complete(record.responses, record.library.props))
                        Experiment.updateStatusLabel(String.format("Survey completed with %d responses. See %s for output."
                                , record.responses.size()
                                , record.outputFileName));
                    else Experiment.updateStatusLabel(String.format("Survey terminated prematurely with %d responses, %s shy of the objective. See %s for output."
                            , record.responses.size()
                            , record.library.props.getProperty("numparticipants")
                            , record.outputFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SurveyException e) {
                    e.printStackTrace();
                }
            }
        };
    }


    public void sendSurvey(BackendType backend) {

        final Survey survey;
        final Record record;
        final Thread worker, notifier, writer, getter;

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
                        record.backendType = backend;
                    } else {
                        record = Experiment.makeSurvey(backend);
                        survey = record.survey;
                        cachedSurveys.put(csv, survey);
                        ResponseManager.putRecord(survey, record);
                    }
                }
            } else {
                survey=null; record=null;
            }

            if (record != null)
                updateProperties(record);

            Runner.BoxedBool interrupt = new Runner.BoxedBool(false);
            worker = makeRunner(record, interrupt);
            notifier = makeNotifier(worker, survey, backend);
            getter = new Runner().makeResponseGetter(survey, interrupt, backend);
            writer = Runner.makeWriter(survey, interrupt);

            if (survey!=null) {
                Experiment.loadParameters(record);
                worker.start();
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
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }
}

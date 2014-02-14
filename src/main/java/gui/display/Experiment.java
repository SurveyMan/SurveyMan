package gui.display;

import csv.CSVLexer;
import csv.CSVParser;
import gui.ExperimentActions;
import gui.GUIActions;
import gui.SurveyMan;
import gui.actionmanager.ExperimentAction;
import gui.actionmanager.HITAction;
import gui.actionmanager.StatusAction;
import survey.Survey;
import survey.SurveyException;
import system.mturk.MturkLibrary;
import system.Record;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

public class Experiment {

    final static String[] units = {"seconds", "minutes", "hours", "days"};
    final static int[] conversion = {1,60,3600,86400};
    final static String[] bools = new String[]{"true", "false"};
    final public static String[] seps = new String[]{",","\\t",";",":"};

    public static JMenuBar menuBar;
    public static int expMenuIndex = 1;

    public static JTextPane statusLabel = new JTextPane();
    public static StyledDocument doc = statusLabel.getStyledDocument();
    public static SimpleAttributeSet sas = new SimpleAttributeSet();
    public static JComboBox csvLabel = new JComboBox();

    public static JButton selectCSV = new JButton("Select CSV.");
    public static JButton previewCSV = new JButton("Preview CSV.");
    public static JButton viewResults = new JButton("View Results file.");
    public static JButton splashLoadFromURL = new JButton("Read file or URL from textbox.");
    public static JButton splashLoadFromFile = new JButton("Choose file.");
    public static JButton send_mturk = new JButton("Send Survey to Mechanical Turk.");
    public static JButton send_local = new JButton("Send Survey to local server.");
    public static JButton print = new JButton("Print Survey.");
    public static JButton previewHTML = new JButton("Preview HIT.");
    public static JButton dumpParams = new JButton("Save parameters.");
    public static JButton viewHIT = new JButton("View HIT.");

    public static JTextArea title, description, kwds, splashPage;
    public static JFormattedTextField reward = new JFormattedTextField(NumberFormat.getCurrencyInstance(Locale.US));
    public static JFormattedTextField duration = new JFormattedTextField(NumberFormat.getNumberInstance());
    public static JComboBox duration_units = new JComboBox(units);
    public static JFormattedTextField lifetime = new JFormattedTextField(NumberFormat.getNumberInstance());
    public static JComboBox lifetime_units = new JComboBox(units);
    public static JFormattedTextField participants = new JFormattedTextField(NumberFormat.getIntegerInstance());
    public static JComboBox sandbox = new JComboBox(bools);
    public static JComboBox fieldSep = new JComboBox(seps);

    private static void setActionListeners() {
        splashLoadFromURL.addActionListener(new ExperimentAction(GUIActions.LOAD_SPLASH_FROM_URL));
        splashLoadFromFile.addActionListener(new ExperimentAction(GUIActions.LOAD_SPLASH_FROM_FILE));
        selectCSV.addActionListener(new ExperimentAction(GUIActions.SELECT_CSV));
        previewCSV.addActionListener(new ExperimentAction(GUIActions.PREVIEW_CSV));
        viewResults.addActionListener(new ExperimentAction(GUIActions.VIEW_RESULTS));
        send_mturk.addActionListener(new ExperimentAction(GUIActions.SEND_MTURK));
        send_local.addActionListener(new ExperimentAction(GUIActions.SEND_LOCAL));
        print.addActionListener(new ExperimentAction(GUIActions.PRINT_SURVEY));
        previewHTML.addActionListener(new ExperimentAction(GUIActions.PREVIEW_HIT));
        dumpParams.addActionListener(new ExperimentAction(GUIActions.DUMP_PARAMS));
        viewHIT.addActionListener(new ExperimentAction(GUIActions.VIEW_HIT));
        duration_units.addActionListener(new ExperimentAction(GUIActions.UPDATE_FORMATTER_DURATION));
    }

    public static void loadParameters(Record record) {
        Properties props = record.library.props;
        props.setProperty("title", title.getText());
        props.setProperty("description", description.getText());
        props.setProperty("keywords", kwds.getText());
        props.setProperty("splashpage", splashPage.getText());
        try{
            props.setProperty("reward"
                    , String.valueOf((NumberFormat.getCurrencyInstance().parse(reward.getText())).doubleValue()));
            props.setProperty("assignmentduration"
                    , String.valueOf((NumberFormat.getNumberInstance().parse(duration.getText())).longValue()
                    * ((long) conversion[duration_units.getSelectedIndex()])));
            props.setProperty("hitlifetime"
                    , String.valueOf((NumberFormat.getNumberInstance().parse(lifetime.getText())).longValue()
                    * (long)conversion[lifetime_units.getSelectedIndex()]));
            props.setProperty("numparticipants"
                    , participants.getText());
        } catch (ParseException pe){
            SurveyMan.LOGGER.warn(pe);
        }
        props.setProperty("sandbox", bools[sandbox.getSelectedIndex()]);
    }

    public static Record makeSurvey() throws SurveyException, IOException{
        Record record = null;
        try {
            String fieldsep = seps[fieldSep.getSelectedIndex()];
            CSVParser csvParser = new CSVParser(new CSVLexer((String) csvLabel.getSelectedItem(), fieldsep));
            Survey survey = csvParser.parse();
            record = new Record(survey, ExperimentAction.getBackendLibClass());
            record.library.props.setProperty("fieldsep", fieldsep);
            loadParameters(record);
        } catch (NoSuchMethodException e) {
            SurveyMan.LOGGER.warn(e);
        } catch (IllegalAccessException e) {
            SurveyMan.LOGGER.warn(e);
        } catch (InvocationTargetException e) {
            SurveyMan.LOGGER.warn(e);
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return record;
    }

    public static JComponent makeStatusPanel() {
        Dimension size = new Dimension(Display.width, Display.height/6);
        statusLabel.setBorder(BorderFactory.createEmptyBorder());
        statusLabel.setBackground(Color.WHITE);
        statusLabel.setMaximumSize(new Dimension(Display.width, Display.height*10));
        JScrollPane retval = new JScrollPane(statusLabel);
        retval.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        retval.setMaximumSize(size);
        retval.setPreferredSize(size);
        return retval;
    }

    public static void updateStatusLabel(String msg) {
        try{
            doc.insertString(doc.getLength(), msg+"\r\n", sas);
        }catch(BadLocationException ble){
            SurveyMan.LOGGER.warn(ble);
        }
        statusLabel.repaint();
    }
    
    public static JMenuBar makeMenuBar() {
        JMenuBar menu = new JMenuBar();

        JMenu hitManagement = new JMenu();
        hitManagement.setText("HIT Management");
        menu.add(hitManagement);

        JMenuItem expire = new JMenuItem();
        expire.addActionListener(new HITAction(ExperimentActions.HITS_EXPIRE));
        expire.setText("Expire Old HITs");
        hitManagement.add(expire);

        JMenuItem delete = new JMenuItem();
        delete.addActionListener(new HITAction(ExperimentActions.HITS_DELETE));
        delete.setText("Delete Expired HITs");
        hitManagement.add(delete);

        JMenuItem listLiveHITs = new JMenuItem();
        listLiveHITs.addActionListener(new HITAction(ExperimentActions.HITS_LIST_LIVE));
        listLiveHITs.setText("List live HITs");
        hitManagement.add(listLiveHITs);

        JMenu getExperimentStatus = new JMenu("Experiment Options");

        JMenu cancel = new JMenu("Cancel Experiment");
        cancel.addMenuListener(new StatusAction(ExperimentActions.CANCEL_RUNNING, cancel));
        getExperimentStatus.add(cancel);

        JMenu stopNSave = new JMenu("Stop Experiment & Save For Later");
        stopNSave.addMenuListener(new StatusAction(ExperimentActions.STOP_SAVE, stopNSave));
        getExperimentStatus.add(stopNSave);

        JMenu runUnfinished = new JMenu("Run Unfinished Experiment");
        runUnfinished.addMenuListener(new StatusAction(ExperimentActions.RUN_UNFINISHED, runUnfinished));
        getExperimentStatus.add(runUnfinished);

//        JMenu rerun = new JMenu("Re-run Old Experiment");
//        rerun.addMenuListener(new StatusAction(ExperimentActions.RERUN, rerun));
//        getExperimentStatus.add(rerun);

        JMenu status = new JMenu("Get Experiment Status");
        status.addMenuListener(new StatusAction(ExperimentActions.STATUS, status));
        getExperimentStatus.add(status);

        menu.add(getExperimentStatus, expMenuIndex);

        return menu;
    }

    public static JPanel select_experiment() {

        JPanel content = new JPanel(new BorderLayout());
        Properties props = new Properties();
        try {
            props.load(new FileReader(MturkLibrary.PARAMS));
        } catch (IOException e) {
            SurveyMan.LOGGER.fatal(String.format("Parameters file (%s) not found", MturkLibrary.PARAMS));
            System.exit(-1);
        }

        menuBar = makeMenuBar();
        Display.frame.setJMenuBar(menuBar);
        Display.frame.setVisible(true);

        JPanel param_panel = new JPanel(new GridLayout(0,3));

        param_panel.add(new JLabel("Title"));
        title = new JTextArea(props.getProperty("title"));
        JScrollPane titlePane = new JScrollPane(title);
        param_panel.add(titlePane);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Description"));
        description = new JTextArea(props.getProperty("description"));
        JScrollPane descriptionPane = new JScrollPane(description);
        param_panel.add(descriptionPane);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Keywords (separate with commas)"));
        kwds = new JTextArea(props.getProperty("keywords"));
        JScrollPane kwdsPane = new JScrollPane(kwds);
        param_panel.add(kwdsPane);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Splash Page (preview)"));
        splashPage = new JTextArea(props.getProperty("splashpage"));
        JScrollPane splashPane = new JScrollPane(splashPage);
        splashPane.createVerticalScrollBar();
        param_panel.add(splashPane);
        JPanel opts = new JPanel(new GridLayout(2,1));
        opts.add(splashLoadFromURL);
        opts.add(splashLoadFromFile);
        param_panel.add(opts);

        param_panel.add(new JLabel("Reward"));
        reward.setValue(Double.parseDouble(props.getProperty("reward", "0")));
        param_panel.add(reward);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Assignment Duration"));
        duration.setValue(Double.parseDouble(props.getProperty("assignmentduration", "60")));
        param_panel.add(duration);
        param_panel.add(duration_units);

        param_panel.add(new JLabel("HIT Lifetime"));
        lifetime.setValue(Double.parseDouble(props.getProperty("hitlifetime", "3600")));
        param_panel.add(lifetime);
        param_panel.add(lifetime_units);

        param_panel.add(new JLabel("Number of Participants Desired"));
        participants.setValue(Integer.parseInt(props.getProperty("numparticipants", "2")));
        param_panel.add(participants);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Sandbox"));
        sandbox.setSelectedIndex(Arrays.asList(bools).indexOf(props.getProperty("sandbox", "true")));
        param_panel.add(sandbox);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Field separator"));
        fieldSep.setSelectedIndex(Arrays.asList(seps).indexOf(props.getProperty("separator", ",")));
        param_panel.add(fieldSep);
        param_panel.add(new JPanel());

        // choose the csv to run
        param_panel.add(new JLabel("CSV to post"));
        param_panel.add(csvLabel);
        JPanel moreOpts = new JPanel(new GridLayout(2,1));
        JPanel topPane = new JPanel(new GridLayout(1,2));

        topPane.add(selectCSV);
        topPane.add(previewCSV);
        moreOpts.add(topPane);
        moreOpts.add(viewResults);
        param_panel.add(moreOpts);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(10,10,10,10));

        contentPanel.add(param_panel, BorderLayout.CENTER);

        JPanel thingsToDo = new JPanel(new GridLayout(3,2));
        thingsToDo.add(previewHTML);
        thingsToDo.add(viewHIT);
        thingsToDo.add(dumpParams);
        thingsToDo.add(send_mturk);
        thingsToDo.add(print);
        thingsToDo.add(send_local);
        contentPanel.add(thingsToDo, BorderLayout.SOUTH);

        content.add(contentPanel, BorderLayout.CENTER);
        content.add(makeStatusPanel(), BorderLayout.SOUTH);

        return content;
    }

    public static void run() {
        setActionListeners();
        Display.run(select_experiment());
    }
}


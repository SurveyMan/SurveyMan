import csv.CSVParser;
import survey.Survey;
import system.Library;
import system.Runner;
import system.mturk.MturkLibrary;
import system.mturk.ResponseManager;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Formatter;
import java.util.Locale;

/**
 * I would like to acknowledge StackOverflow and the liberal copying I employed to make this Swing crap work.
 */

public class SurveyMan extends JPanel implements ActionListener{

    public static String csv = "";
    public static JLabel csvLabel = new JLabel(csv);
    public JButton selectCSV = new JButton("Select CSV.");
    public JFrame frame = new JFrame("SurveyMan");
    public JPanel content = new JPanel(new BorderLayout());
    public JButton next1 = new JButton("Next");
    public JButton next2 = new JButton("Next");
    public JButton next3 = new JButton("Next");
    public JButton send = new JButton("Send Survey to Mechanical Turk.");

    String[] units = {"seconds", "minutes", "hours", "days"};
    int[] conversion = {1,60,3600,86400};
    String[] bools = new String[]{"true", "false"};
    String[] seps = new String[]{",","\\t",";",":"};


    JTextArea title = new JTextArea(Library.props.getProperty("title"));
    JTextArea description = new JTextArea(Library.props.getProperty("description"));
    JTextArea kwds = new JTextArea(Library.props.getProperty("keywords"));
    JFormattedTextField reward = new JFormattedTextField(NumberFormat.getCurrencyInstance(Locale.US));
    JFormattedTextField duration = new JFormattedTextField(NumberFormat.getNumberInstance());
    JComboBox duration_units = new JComboBox(units);
    JFormattedTextField approve = new JFormattedTextField(NumberFormat.getNumberInstance());
    JComboBox approve_units = new JComboBox(units);
    JFormattedTextField lifetime = new JFormattedTextField(NumberFormat.getNumberInstance());
    JComboBox lifetime_units = new JComboBox(units);
    JFormattedTextField participants = new JFormattedTextField(NumberFormat.getIntegerInstance());
    JComboBox sandbox = new JComboBox(bools);
    JComboBox fieldSep = new JComboBox(seps);


    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getActionCommand().equals(send.getActionCommand())) {
            Library.init();
            Library.props.setProperty("title", title.getText());
            Library.props.setProperty("description", description.getText());
            Library.props.setProperty("keywords", kwds.getText());
            Library.props.setProperty("reward", reward.getText());
            try{
                Library.props.setProperty("assignmentduration", String.valueOf((NumberFormat.getNumberInstance().parse(duration.getText())).doubleValue()
                        * ((double) conversion[duration_units.getSelectedIndex()])));
                Library.props.setProperty("autoapprovedelay", String.valueOf((NumberFormat.getNumberInstance().parse(approve.getText())).doubleValue()
                        * ((double)conversion[approve_units.getSelectedIndex()])));
                Library.props.setProperty("hitlifetime", String.valueOf((NumberFormat.getNumberInstance().parse(lifetime.getText())).doubleValue()
                        * (double)conversion[lifetime_units.getSelectedIndex()]));
                         Library.props.setProperty("numparticipants", participants.getText());
            } catch (ParseException pe){
               pe.printStackTrace();
            }
            Library.props.setProperty("sandbox", bools[sandbox.getSelectedIndex()]);
            MturkLibrary.init();
            try {
                Survey survey = CSVParser.parse(csv, seps[fieldSep.getSelectedIndex()]);
                Thread runner = Runner.run(survey);
                while (true) {
                    Runner.writeResponses(survey);
                    if (! (runner.isAlive() && ResponseManager.hasJobs())) break;
                    try {
                        Thread.sleep(Runner.waitTime);
                    } catch (InterruptedException ie) {}
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else if (actionEvent.getActionCommand().equals(next1.getActionCommand())) {
            // get keys
            content = new JPanel(new BorderLayout());
            frame.setContentPane(setup_frame2(content));
            frame.setVisible(true);
            frame.getContentPane().setPreferredSize(new Dimension(800, 600));
            frame.pack();
            try {
                Desktop.getDesktop().browse(new URI("https://console.aws.amazon.com/iam/home?#security_credential"));
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (URISyntaxException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else if (actionEvent.getActionCommand().equals(next2.getActionCommand())) {
            // move metadata
            content = new JPanel(new BorderLayout());
            frame.setContentPane(setup_frame3(content));
            frame.setVisible(true);
            frame.getContentPane().setPreferredSize(new Dimension(800, 600));
            frame.pack();
        } else if (actionEvent.getActionCommand().equals(next3.getActionCommand())) {
            // choose experiment to run
            content = new JPanel(new BorderLayout());
            frame.setContentPane(select_experiment(content));
            frame.setVisible(true);
            frame.getContentPane().setPreferredSize(new Dimension(800, 600));
            frame.pack();
        } else if (actionEvent.getActionCommand().equals(selectCSV.getActionCommand())) {
            final JFileChooser fc = new JFileChooser();
            fc.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    if (actionEvent.getActionCommand().equals(JFileChooser.APPROVE_SELECTION))
                        SurveyMan.csv = fc.getSelectedFile().getAbsolutePath();
                }
            });
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.showOpenDialog(selectCSV);
            // redisplay
            csvLabel.setText(csv);
        }
    }

    private JPanel select_experiment(JPanel content) {
        // button to select file
        // post parameters to edit
        Library.init();
        JPanel param_panel = new JPanel(new GridLayout(0,3));

        param_panel.add(new JLabel("Title"));
        title.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JScrollPane titlePane = new JScrollPane(title);
        param_panel.add(titlePane);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Description"));
        description.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JScrollPane descriptionPane = new JScrollPane(description);
        param_panel.add(descriptionPane);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Keywords (separate with commas)"));
        kwds.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(10,10,10,10)));
        JScrollPane kwdsPane = new JScrollPane(kwds);
        param_panel.add(kwdsPane);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Reward"));
        reward.setValue(Double.parseDouble(Library.props.getProperty("reward")));
        param_panel.add(reward);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Assignment Duration"));
        duration.setValue(Double.parseDouble(Library.props.getProperty("assignmentduration")));
        param_panel.add(duration);
        param_panel.add(duration_units);

        param_panel.add(new JLabel("Auto-Approve Delay"));
        approve.setValue(Double.parseDouble(Library.props.getProperty("autoapprovaldelay")));
        param_panel.add(approve);
        param_panel.add(approve_units);

        param_panel.add(new JLabel("HIT Lifetime"));
        lifetime.setValue(Double.parseDouble(Library.props.getProperty("hitlifetime")));
        param_panel.add(lifetime);
        param_panel.add(lifetime_units);

        param_panel.add(new JLabel("Number of Participants Desired"));
        participants.setValue(Integer.parseInt(Library.props.getProperty("numparticipants")));
        param_panel.add(participants);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Sandbox"));
        param_panel.add(sandbox);
        param_panel.add(new JPanel());

        param_panel.add(new JLabel("Field separator"));
        param_panel.add(fieldSep);
        param_panel.add(new JPanel());

        // choose the csv to run
        param_panel.add(new JLabel("CSV to post"));
        param_panel.add(csvLabel);
        selectCSV.addActionListener(this);
        param_panel.add(selectCSV);

        JPanel dummy = new JPanel();
        dummy.setPreferredSize(new Dimension(20,600));
        content.add(dummy, BorderLayout.WEST);
        dummy = new JPanel();
        dummy.setPreferredSize(new Dimension(20, 600));
        content.add(dummy, BorderLayout.EAST);
        content.add(param_panel, BorderLayout.CENTER);

        send.addActionListener(this);
        content.add(send, BorderLayout.SOUTH);

        return content;
    }

    private void setup_frame1(JPanel content) {

        JLabel note = new JLabel("<html>" +
                "It appears you haven't yet configured your Surveyman directory." +
                "<br>Your metadata, access keys, and output will appear in this folder." +
                "<br>" +
                "<br>Your Surveyman home directory is located at :" +
                "<br>"+Library.DIR +
                "<br>Now you will need to download your keys to this directory.<br>" +
                "<br><b>WARNING:</b> You will need both AWS and Amazon Mechanical Turk accounts to continue." +
                "<br>Register at https://aws.amazon.com and https://www.mturk.com." +
                "<br>Click the Next button to continue." +
                "</html>");
        note.setVerticalAlignment(JLabel.CENTER);
        note.setHorizontalAlignment(JLabel.CENTER);
        content.add(note, BorderLayout.CENTER);

        next1.addActionListener(this);
        content.add(next1, BorderLayout.SOUTH);
        frame.setContentPane(content);
    }

    private JPanel setup_frame2(JPanel content) {

        JPanel stuff = new JPanel(new BorderLayout());

        JLabel note = new JLabel("<html>" +
                "Now you will need to add your keys to the Surveyman folder. " +
                "<br>Navigate to:" +
                "<br><br>https://console.aws.amazon.com/iam/home?#security_credential" +
                "<br>and create new keys. If available, download them. If the download " +
                "<br>option is not available, save them in a file with the following format:" +
                "<p>access_key=&lt;your_access_key&gt;</p>" +
                "<p>secret_key=&lt;your_secret_key&gt;</p>" +
                "<br>When you're ready, select the location of this file." +
                "</html>");
        note.setVerticalAlignment(JLabel.CENTER);
        note.setHorizontalAlignment(JLabel.CENTER);
        stuff.add(note, BorderLayout.CENTER);

        final JButton button = new JButton("Choose the keys file.");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final JFileChooser fc = new JFileChooser();
                fc.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent) {
                        if (actionEvent.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                            File keyFile = new File(fc.getSelectedFile().getAbsolutePath());
                            keyFile.renameTo(new File(Library.CONFIG));
                        }
                    }
                });
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.showOpenDialog(button);
            }
        });
        stuff.add(button, BorderLayout.SOUTH);
        content.add(stuff,BorderLayout.CENTER);

        next2.addActionListener(this);
        content.add(next2, BorderLayout.SOUTH);
        return content;
    }

    private JPanel setup_frame3(JPanel content) {
        // move metadata and skeletons to teh surveyman folder
        File metadata = new File(".metadata");
        File params = new File("params.properties");

        if (!metadata.isDirectory()) {

            JPanel stuff = new JPanel(new BorderLayout());

            JLabel note = new JLabel("<html>" +
                    "It appears that you are not running surveyman from the unzipped surveyman folder." +
                    "<br>Please tell us where to find the surveyman install folder so we can copy data " +
                    "<br>for setup. This folder is the result of unzipping surveyman.zip. If you cannot " +
                    "<br>find surveyman.zip, download a fresh copy from http://cs.umass.edu/~etosch/surveyman.zip"
            );
            stuff.add(note, BorderLayout.CENTER);

            final JButton button = new JButton("Find surveyman install folder.");
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    final JFileChooser fc = new JFileChooser();
                    fc.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            boolean notFound = true;
                            while (notFound){
                                if (actionEvent.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                                    String surveymanInstallFolder = fc.getSelectedFile().getAbsolutePath();
                                    File metadata = new File(surveymanInstallFolder+Library.fileSep+".metadata");
                                    File params = new File(surveymanInstallFolder+Library.fileSep+"params.properties");
                                    if (!metadata.isDirectory()) {
                                        JOptionPane.showMessageDialog(null,
                                                surveymanInstallFolder+" does not contain the directory .metadata. " +
                                                "Please choose another folder.");
                                        continue;
                                    }
                                    if (!params.isFile()) {
                                        JOptionPane.showMessageDialog(null,
                                                surveymanInstallFolder+" does not contain the directory .metadata. " +
                                                "Please choose another folder.");
                                        continue;
                                    }
                                    notFound = false;
                                    metadata.renameTo(new File(Library.DIR+Library.fileSep+".metadata"));
                                    params.renameTo(new File(Library.PARAMS));
                                }
                            }
                        }
                    });
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fc.showOpenDialog(button);
                }
            });
            stuff.add(button, BorderLayout.SOUTH);
            content.add(stuff, BorderLayout.CENTER);
        } else {
            metadata.renameTo(new File(Library.DIR+Library.fileSep+".metadata"));
            params.renameTo(new File(Library.PARAMS));
            JLabel note = new JLabel("<html>" +
                    "Click next to continue." +
                    "</html>");
            content.add(note, BorderLayout.CENTER);
        }

        next3.addActionListener(this);
        content.add(next3, BorderLayout.SOUTH);
        return content;
    }

    public void setup(){
        // set up UI
        setup_frame1(content);
        // try creating new directory
        (new File(Library.DIR)).mkdir();
        // load web page
    }


    public static void main(String[] args) {
        SurveyMan sm = new SurveyMan();
        sm.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        sm.frame.setMenuBar(new MenuBar());
        sm.frame.setContentPane(sm.content);
        sm.frame.getContentPane().setPreferredSize(new Dimension(800, 600));
        sm.frame.pack();
        sm.frame.setLocationRelativeTo(null);
        sm.frame.setVisible(true);
        sm.next1.setPreferredSize(new Dimension(100, 30));
        if (!(new File(Library.DIR)).isDirectory()) {
            sm.setup();
        } else {
            sm.frame.setContentPane(sm.select_experiment(sm.content));
        }
    }
}
package gui.display;

import gui.SurveyMan;
import gui.actionmanager.*;
import gui.display.Display;
import system.Library;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Setup {

    public static JButton next = new JButton("Next");
    final public static JButton findAccessKeys = new JButton("Choose the keys file.");
    final public static JButton findInstallFolder = new JButton("Find surveyman install folder.");
    static {
        next.addActionListener(new Wizard(Display.frame));
        next.setPreferredSize(new Dimension(100, 30));
        findInstallFolder.addActionListener(new FindInstallFolderAction(findInstallFolder));
        findAccessKeys.addActionListener(new FindAccessKeysAction(findAccessKeys));
    }

    public static JPanel setup_frame1() {

        JPanel content = new JPanel(new BorderLayout());

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

        content.add(next, BorderLayout.SOUTH);

        return content;
   }

    public static JPanel setup_frame2() {

        JPanel content = new JPanel(new BorderLayout());

        // if anything goes wrong, delete the surveyman directory
        try{

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
            stuff.add(findAccessKeys, BorderLayout.SOUTH);

            content.add(stuff,BorderLayout.CENTER);
            content.add(next, BorderLayout.SOUTH);

        } catch (Exception e) {
            (new File(Library.DIR)).delete();
            SurveyMan.LOGGER.fatal(e);
        }
        return content;
    }

    public static JPanel setup_frame3() {

        JPanel content = new JPanel(new BorderLayout());
        JPanel stuff = new JPanel(new BorderLayout());

        JLabel note = new JLabel("<html>" +
                "It appears that you are not running surveyman from the unzipped surveyman folder." +
                "<br>Please tell us where to find the surveyman install folder so we can copy data " +
                "<br>for setup. This folder is the result of unzipping surveyman.zip. If you cannot " +
                "<br>find surveyman.zip, download a fresh copy from http://cs.umass.edu/~etosch/surveyman.zip"
        );
        stuff.add(note, BorderLayout.CENTER);
        stuff.add(findInstallFolder, BorderLayout.SOUTH);

        content.add(stuff, BorderLayout.CENTER);
        content.add(next, BorderLayout.SOUTH);

        return content;
    }

    public static void run() {
        Display.run(setup_frame1());
    }

}
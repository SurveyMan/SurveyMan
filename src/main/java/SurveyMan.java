import system.Library;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * I would like to acknowledge StackOverflow and the liberal copying I employed to make this Swing crap work.
 */

public class SurveyMan extends JPanel implements ActionListener{

    public JFrame frame = new JFrame("SurveyMan");
    public JPanel content = new JPanel(new BorderLayout());
    public JButton next1 = new JButton("Next");
    public JButton next2 = new JButton("Next");

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getActionCommand().equals(next1.getActionCommand())) {
            content = new JPanel(new BorderLayout());
            frame.setContentPane(setup_frame2(content));
            frame.setVisible(true);
            frame.getContentPane().setPreferredSize(new Dimension(800, 600));
            frame.pack();
        } else if (actionEvent.getActionCommand().equals(next2.getActionCommand())) {
            content = new JPanel(new BorderLayout());
            frame.setContentPane(setup_frame3(content));
        }
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
                "<br>and create new keys. If available, download them. If the download option is not available," +
                "<br>save them in a file with the following format:" +
                "<p>access_key=&lt;your_access_key&gt;</p>" +
                "<p>secret_key=&lt;your_secret_key&gt;</p>" +
                "<br>When you're ready, select the location of this file." +
                "</html>");
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
        stuff.add(new JPanel(), BorderLayout.EAST);

        try {
            Desktop.getDesktop().browse(new URI("https://console.aws.amazon.com/iam/home?#security_credential"));
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        content.add(stuff,BorderLayout.CENTER);

        next2.addActionListener(this);
        content.add(next2, BorderLayout.SOUTH);
        return content;
    }

    private JPanel setup_frame3(JPanel content) {
        return content;
    }

    public static boolean surveyman_env_var(){
        String s = System.getenv("SURVEYMAN");
        System.out.println(s);
        return false;
    }

    public void setup(){
        // set up UI
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMenuBar(new MenuBar());
        frame.setContentPane(content);
        frame.getContentPane().setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        setup_frame1(content);
        // try creating new directory
        (new File(Library.DIR)).mkdir();
        // load web page
    }


    public static void main(String[] args) {
        SurveyMan sm = new SurveyMan();
        sm.next1.setPreferredSize(new Dimension(100, 30));
        if ((new File(Library.DIR)).isDirectory()) {

        } else {
            // create surveyman folder
            sm.setup();
        }
    }
}

import system.Library;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class SurveyMan extends JPanel implements ActionListener{

    public JFrame frame = new JFrame("SurveyMan");
    public JPanel content = new JPanel(new GridLayout(3,1));
    public JButton next1 = new JButton("Next");

    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getActionCommand().equals(next1.getActionCommand())) {
            content = new JPanel(new GridLayout(3,1));
            frame.setContentPane(frame2(content));
        }
    }

    private void frame1(JPanel content) {

        JLabel note = new JLabel("<html>" +
                "It appears you haven't yet set your SURVEYMAN configuration directory." +
                "<br>Please choose a location for the configuration directory and create an environment variable named SURVEYMAN." +
                "<br>A suggestion is to create a folder called ~/.surveyman." +
                "<ul>" +
                "<li>Linux: add 'export SURVEYMAN=$HOME/.surveyman' to .bashrc in your home directory.</li>" +
                "<li>OSX: add 'export SURVEYMAN=$HOME/.surveyman' to .bash_profile in your home directory.</li>" +
                "<li>Windows: create a system environment variable called SURVEYMAN and set it equal to %HOME%\\.surveyman</li>" +
                "</ul>" +
                "</html>");
        note.setVerticalAlignment(JLabel.CENTER);
        note.setHorizontalAlignment(JLabel.CENTER);
        content.add(note);

//        final JButton button = new JButton("Choose config directory");
//        button.addActionListener(new ActionListener() {
//            @Override
//            public void actionPerformed(ActionEvent actionEvent) {
//                final JFileChooser fc = new JFileChooser();
//                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//                int retval = fc.showOpenDialog(button);
//                fc.addActionListener(new ActionListener() {
//                    @Override
//                    public void actionPerformed(ActionEvent actionEvent) {
//                        while (! actionEvent.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {}
//                        //Library.DIR = fc.getSelectedFile().getAbsolutePath();
//                        try {
//                            Runtime.getRuntime().exec(String.format("export SURVEYMAN=%s", fc.getSelectedFile().getAbsolutePath()));
//                        } catch (IOException e) {
//                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                        }
//                    }
//                });
//            }
//        });
        //content.add(button);
        next1.addActionListener(this);
        while(System.getenv().get("SURVEYMAN")==null) {
            System.out.println(System.getenv().keySet());
        }
        content.add(next1);
        frame.setContentPane(content);
    }

    private JPanel frame2(JPanel content) {
        return content;
    }

    public void run() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMenuBar(new MenuBar());
        frame.setContentPane(content);
        frame.pack();
        frame.setVisible(true);
        if (System.getenv().get("SURVEYMAN")==null) {
            frame1(content);
        }
    }

    public static boolean surveyman_env_var(){
        String s = System.getenv("SURVEYMAN");
        System.out.println(s);
        return false;
    }


    public static void main(String[] args) {
        surveyman_env_var();
        //(new SurveyMan()).run();
    }
}

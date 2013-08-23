package gui.actionmanager;

import gui.display.Display;
import gui.display.Experiment;
import gui.display.Setup;
import gui.SurveyMan;
import system.Library;
import system.mturk.MturkLibrary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Wizard implements ActionListener {

    private JFrame frame;
    private int thisPane = 0;
    public static boolean keysSet = false;
    public static boolean metadataMoved = false;
    public static boolean paramsMoved = false;

    public Wizard(JFrame frame) {
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        switch (thisPane) {
            case 0:
                getAccessKeys();
                frame.setContentPane(Setup.setup_frame2());
                frame.setVisible(true);
                frame.getContentPane().setPreferredSize(new Dimension(Display.width, Display.height));
                frame.pack();
                if (keysSet)
                    thisPane +=1;
                break;
            case 1 :
                if (moveMetadata()) {
                    Experiment.run();
                    thisPane += 2;
                } else {
                    frame.setContentPane(Setup.setup_frame3());
                    if (metadataMoved && paramsMoved)
                        thisPane += 1;
                }
                frame.getContentPane().setPreferredSize(new Dimension(Display.width, Display.height));
                frame.setVisible(true);
                frame.pack();
                break;
            case 2:
                Experiment.run();
                break;
        }
    }

    private boolean moveMetadata(){
        // if anything goes wrong, delete the surveyman directory
        try{
            // move metadata and skeletons to the surveyman folder
            File metadata = new File(".metadata");
            File params = new File("params.properties");

            if (!(metadata.isDirectory() && params.isFile())) {
                // load lib
                MturkLibrary.init();
                return false;
            } else {
                metadata.renameTo(new File(Library.DIR+Library.fileSep+".metadata"));
                metadataMoved = true;
                params.renameTo(new File(Library.PARAMS));
                paramsMoved = true;
                // load lib
                MturkLibrary.init();
                return true;
            }
        } catch (Exception e) {
            (new File(Library.DIR)).delete();
            SurveyMan.LOGGER.fatal(e);
        }
        return false;
    }

    private void getAccessKeys() {
        // make directory for access keys
        File home = new File(Library.DIR);
        if (!home.exists())
            home.mkdir();
        // prompt for keys
        if (!keysSet) {
            try {
                Desktop.getDesktop().browse(new URI("https://console.aws.amazon.com/iam/home?#security_credential"));
            } catch (IOException e) {
                SurveyMan.LOGGER.fatal(e);
            } catch (URISyntaxException e) {
                SurveyMan.LOGGER.fatal(e);
            }
        }
    }
}
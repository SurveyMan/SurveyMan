package gui.actionmanager;

import gui.display.Display;
import system.mturk.MturkLibrary;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class FindAccessKeysAction implements ActionListener {

    final JFileChooser fc = new JFileChooser();
    final JButton findAccessKeys;
    public boolean used = false;

    public FindAccessKeysAction(JButton findAccessKeys) {
        this.findAccessKeys = findAccessKeys;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        fc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String err = "";
                if (actionEvent.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                    File keyFile = fc.getSelectedFile();
                    String filename = keyFile.getAbsolutePath();
                    try{
                        Properties prop = new Properties();
                        prop.load(new FileReader(new File(filename)));
                        if (prop.getProperty("access_key")==null && prop.getProperty("AWSAccessKeyId")==null)
                            throw new IllegalArgumentException("No access key entry found.");
                        keyFile.renameTo(new File(MturkLibrary.CONFIG));
                        //JOptionPane.showMessageDialog(Display.frame, "Click Next to continue.");
                        used = true;
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    } catch (IllegalArgumentException iae) {
                        err = iae.getMessage() + "\r\n This is not a valid config file.";
                    } catch (NullPointerException npe) {
                        err = npe.getMessage() + "\r\n This is not a valid config file.";
                    } finally {
                        if (!err.equals(""))
                            JOptionPane.showMessageDialog(Display.frame, err);
                    }
                }
            }
        });
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.showOpenDialog(findAccessKeys);
    }
}

package gui.actionmanager;

import system.Library;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class FindInstallFolderAction implements ActionListener{

        final JFileChooser fc = new JFileChooser();
        final JButton findInstallFolder;

        public FindInstallFolderAction(JButton findInstallFolder) {
            this.findInstallFolder = findInstallFolder;
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            fc.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    boolean notFound = true;
                    while (notFound){
                        if (actionEvent.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                            String surveymanInstallFolder = fc.getSelectedFile().getAbsolutePath();
                            File metadata = new File(surveymanInstallFolder+ Library.fileSep+".metadata");
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
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.showOpenDialog(findInstallFolder);
        }
}

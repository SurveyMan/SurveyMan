package gui.actionmanager;

import system.Library;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class FindAccessKeysAction implements ActionListener {

    final JFileChooser fc = new JFileChooser();
    final JButton findAccessKeys;

    public FindAccessKeysAction(JButton findAccessKeys) {
        this.findAccessKeys = findAccessKeys;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
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
        fc.showOpenDialog(findAccessKeys);
    }
}

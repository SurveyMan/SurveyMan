package gui.display;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Locale;

public class Display {

    public static JFrame frame = new JFrame("SurveyMan");
    public static int width = 800;
    public static int height = 800;

    public static void run(JPanel content) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //frame.setLocationRelativeTo(null);
        frame.setContentPane(content);
        frame.getContentPane().setPreferredSize(new Dimension(Display.width, Display.height));
        frame.setVisible(true);
        frame.pack();
    }

}



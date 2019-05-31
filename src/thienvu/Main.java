package thienvu;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        Crawler crawler = new Crawler();
        Ui ui = new Ui(crawler);
        crawler.setUi(ui);

        JFrame frame = new JFrame("Web crawler");
        ui.setParent(frame);
        frame.setContentPane(ui.getUiPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
    }

}

package thienvu;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;

public class Ui {
    private JPanel uiPanel;
    private JButton btnStartStop;
    private JButton btnExit;
    private JList<String> lsErroredLinks;
    private JList<String> lsDownloadedLinks;
    private JTextField txtLinkToDownload;
    private JTextField txtMaxThread;
    private JTextField txtSaveFolder;
    private JFrame parent = null;

    void setParent(JFrame parent) {
        this.parent = parent;
    }

    public Ui(Crawler crawler) {
        btnExit.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                System.exit(0);
            }
        });
        btnStartStop.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if (btnStartStop.getText().equals("Start")) {
                    String saveFolder = txtSaveFolder.getText();

                    if (saveFolder.equals("")) {
                        JOptionPane.showMessageDialog(parent,
                                "You must provide a path to save your website.",
                                "Folder empty",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String linkToDownload = txtLinkToDownload.getText();
                    if (linkToDownload.equals("")) {
                        JOptionPane.showMessageDialog(parent,
                                "You must provide a link to download.",
                                "Link empty",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    int maxThread = -1;
                    try {
                        maxThread = Integer.parseInt(txtMaxThread.getText());
                    } catch (Exception ignored) {
                        JOptionPane.showMessageDialog(parent,
                                "You must enter a number for max thread.",
                                "Invalid max thread count",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    crawler.start(saveFolder, linkToDownload, maxThread);
                    btnStartStop.setText("Stop");
                } else {
                    crawler.stop();
                }
            }
        });
        lsDownloadedLinks.setModel(new DefaultListModel<>());
        lsErroredLinks.setModel(new DefaultListModel<>());
    }

    public JPanel getUiPanel() {
        return uiPanel;
    }

    public synchronized void updateDownloadedList(String link) {
        ((DefaultListModel<String>)lsDownloadedLinks.getModel()).addElement(link);
        System.out.printf("Model size: %s\n", ((DefaultListModel<String>)lsDownloadedLinks.getModel()).size());
    }


    public synchronized void updateErrorList(String link) {
        ((DefaultListModel<String>)lsErroredLinks.getModel()).addElement(link);
    }
}

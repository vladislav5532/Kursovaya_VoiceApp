import gui.MainWindow;
import javax.swing.*;

public class AppLauncher {
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);
        });
    }
}
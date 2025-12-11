import gui.MainWindow;
import javax.swing.*;

public class AppLauncher {
    public static void main(String[] args) {
        // Устанавливаем кодировку UTF-8
        System.setProperty("file.encoding", "UTF-8");

        // Настройка Look & Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Запуск главного окна
        SwingUtilities.invokeLater(() -> {
            MainWindow mainWindow = new MainWindow();
            mainWindow.setVisible(true);
        });
    }
}
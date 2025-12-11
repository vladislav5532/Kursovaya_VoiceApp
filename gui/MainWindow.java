package gui;

import core.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainWindow extends JFrame {
    private VoiceRecognitionService recognitionService;
    private FileManager fileManager;

    private JTextArea textArea;
    private JButton recognizeFileButton;
    private JButton saveButton;
    private JLabel statusLabel;

    public MainWindow() {
        // Устанавливаем UTF-8
        setUTF8Encoding();

        initComponents();
        setupLayout();
        setupListeners();
        initializeServices();
    }

    private void setUTF8Encoding() {
        try {
            System.setProperty("file.encoding", "UTF-8");
            // Принудительно устанавливаем UTF-8 как дефолтную кодировку
            java.lang.reflect.Field charset =
                    java.nio.charset.Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        setTitle("Голосовой блокнот v1.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        textArea = new JTextArea();
        textArea.setFont(new Font("Arial Unicode MS", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        recognizeFileButton = new JButton("📁 Загрузить и распознать аудиофайл");
        saveButton = new JButton("💾 Сохранить текст");
        statusLabel = new JLabel("Готов к работе. Используйте WAV-файлы 16000 Гц, моно");

        saveButton.setEnabled(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(recognizeFileButton);
        controlPanel.add(saveButton);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);

        add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(controlPanel, BorderLayout.NORTH);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        recognizeFileButton.addActionListener(e -> recognizeFromFile());
        saveButton.addActionListener(e -> saveNote());
    }

    private void initializeServices() {
        try {
            fileManager = new FileManager(".");
            recognitionService = new VoiceRecognitionService("model");
            statusLabel.setText("Система готова к работе (UTF-8)");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка инициализации: " + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void recognizeFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".wav");
            }

            @Override
            public String getDescription() {
                return "WAV файлы (*.wav)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File audioFile = fileChooser.getSelectedFile();

            try {
                statusLabel.setText("Обработка файла...");

                // РЕАЛЬНОЕ РАСПОЗНАВАНИЕ
                String text = recognitionService.recognizeAudioFile(audioFile);

                // Если текст пустой
                if (text == null || text.trim().isEmpty()) {
                    throw new IOException("Пустой результат");
                }

                // ДОПОЛНИТЕЛЬНОЕ ИСПРАВЛЕНИЕ КОДИРОВКИ
                text = fixTextEncoding(text);

                // Вывод в GUI
                textArea.append("[Файл: " + audioFile.getName() + "]\n");
                textArea.append(text + "\n\n");
                statusLabel.setText("Файл распознан: " + audioFile.getName());
                saveButton.setEnabled(true);

                // Показ результата в отдельном окне с UTF-8
                showResultDialog("Результат распознавания",
                        "Файл: " + audioFile.getName() + "\n\n" +
                                "Текст:\n" + text);

            } catch (Exception ex) {
                // ДЕМО-РЕЖИМ при ошибке
                ex.printStackTrace();

                String text = recognitionService.recognizeAudioFileDemo(audioFile);

                textArea.append("[ДЕМО: " + audioFile.getName() + "]\n");
                textArea.append(text + "\n\n");
                statusLabel.setText("Демо-режим: файл обработан");
                saveButton.setEnabled(true);

                JOptionPane.showMessageDialog(this,
                        "Реальное распознавание не удалось:\n" + ex.getMessage() +
                                "\n\nИспользуется демо-режим.",
                        "Демо", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private String fixTextEncoding(String text) {
        if (text == null) return "";

        // Если уже есть русские буквы - ок
        if (text.matches(".*[А-Яа-яЁё].*")) {
            return text;
        }

        // Если похоже на испорченный UTF-8 ("СБР°Р. РҐРІР°")
        if (text.contains("Р") && text.contains("В") && text.contains("С")) {
            try {
                byte[] bytes = text.getBytes("Windows-1251");
                String fixed = new String(bytes, "UTF-8");
                System.out.println("Исправлена кодировка: '" + text + "' -> '" + fixed + "'");
                return fixed;
            } catch (Exception e) {
                return text;
            }
        }

        return text;
    }

    private void showResultDialog(String title, String message) {
        JTextArea textArea = new JTextArea(message);
        textArea.setFont(new Font("Arial Unicode MS", Font.PLAIN, 12));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));

        JOptionPane.showMessageDialog(this, scrollPane, title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveNote() {
        String text = textArea.getText().trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Нет текста для сохранения",
                    "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String title = JOptionPane.showInputDialog(this,
                "Введите название заметки:",
                "Сохранение заметки", JOptionPane.QUESTION_MESSAGE);

        if (title == null || title.trim().isEmpty()) {
            title = "Заметка_" + System.currentTimeMillis();
        }

        try {
            fileManager.saveNote(text, title);
            statusLabel.setText("Заметка сохранена: " + title + ".txt");

            int choice = JOptionPane.showConfirmDialog(this,
                    "Заметка сохранена в UTF-8!\nОчистить поле?",
                    "Сохранено", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                textArea.setText("");
                saveButton.setEnabled(false);
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка сохранения: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Устанавливаем UTF-8 для всей программы
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");

        // Устанавливаем шрифт поддерживающий Unicode
        setUIFont(new javax.swing.plaf.FontUIResource("Arial Unicode MS", Font.PLAIN, 12));

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);
        });
    }

    private static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                UIManager.put(key, f);
            }
        }
    }
}
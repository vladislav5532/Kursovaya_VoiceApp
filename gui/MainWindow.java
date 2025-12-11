package gui;

import core.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainWindow extends JFrame {
    private VoiceRecognitionService recognitionService;
    private AudioRecorder audioRecorder;
    private FileManager fileManager;

    private JTextArea textArea;
    private JButton recordButton;
    private JButton stopRecordButton;
    private JButton recognizeFileButton;
    private JButton saveButton;
    private JLabel statusLabel;

    public MainWindow() {
        setUTF8Encoding();
        initComponents();
        setupLayout();
        setupListeners();
        initializeServices();
    }

    private void setUTF8Encoding() {
        try {
            System.setProperty("file.encoding", "UTF-8");
            java.lang.reflect.Field charset =
                    java.nio.charset.Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initComponents() {
        setTitle("Голосовой блокнот - Запись и распознавание");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);

        textArea = new JTextArea();
        textArea.setFont(new Font("Arial Unicode MS", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        recordButton = new JButton("🎤 Начать запись с микрофона");
        stopRecordButton = new JButton("⏹ Остановить запись");
        recognizeFileButton = new JButton("📁 Загрузить аудиофайл");
        saveButton = new JButton("💾 Сохранить текст");
        statusLabel = new JLabel("Готов к работе");

        stopRecordButton.setEnabled(false);
        saveButton.setEnabled(false);
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Панель записи
        JPanel recordPanel = new JPanel(new FlowLayout());
        recordPanel.add(recordButton);
        recordPanel.add(stopRecordButton);

        // Панель файлов
        JPanel filePanel = new JPanel(new FlowLayout());
        filePanel.add(recognizeFileButton);
        filePanel.add(saveButton);

        // Общая панель управления
        JPanel controlPanel = new JPanel(new GridLayout(2, 1));
        controlPanel.add(recordPanel);
        controlPanel.add(filePanel);

        // Статус бар
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);

        // Основная область
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(controlPanel, BorderLayout.NORTH);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        recordButton.addActionListener(e -> startRecording());
        stopRecordButton.addActionListener(e -> stopRecording());
        recognizeFileButton.addActionListener(e -> recognizeFromFile());
        saveButton.addActionListener(e -> saveNote());
    }

    private void initializeServices() {
        try {
            fileManager = new FileManager(".");
            recognitionService = new VoiceRecognitionService("model");
            audioRecorder = new AudioRecorder();

            // Настраиваем callback для распознавания в реальном времени
            recognitionService.setCallback(new VoiceRecognitionService.RecognitionCallback() {
                @Override
                public void onTextRecognized(String text) {
                    SwingUtilities.invokeLater(() -> {
                        if (!text.trim().isEmpty()) {
                            textArea.append(text + "\n");
                            saveButton.setEnabled(true);
                        }
                    });
                }

                @Override
                public void onPartialResult(String partial) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Слышу: " + partial);
                    });
                }

                @Override
                public void onError(String error) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Ошибка: " + error);
                        JOptionPane.showMessageDialog(MainWindow.this, error,
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                    });
                }

                @Override
                public void onStatus(String status) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(status);
                    });
                }
            });

            statusLabel.setText("Система готова к работе");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка инициализации: " + e.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===== ЗАПИСЬ С МИКРОФОНА =====
    private void startRecording() {
        try {
            // Запускаем запись в файл
            audioRecorder.startRecording("recordings");

            // Запускаем распознавание в реальном времени
            recognitionService.startMicrophoneRecording();

            recordButton.setEnabled(false);
            stopRecordButton.setEnabled(true);
            recognizeFileButton.setEnabled(false);
            statusLabel.setText("Идёт запись с микрофона... Говорите!");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Не удалось начать запись:\n" + e.getMessage() +
                            "\n\nПроверьте подключение микрофона или используйте файлы.",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopRecording() {
        // Останавливаем распознавание
        recognitionService.stopMicrophoneRecording();

        // Останавливаем запись в файл
        File recordedFile = audioRecorder.stopRecording();

        if (recordedFile != null) {
            statusLabel.setText("Запись сохранена: " + recordedFile.getName());
        }

        recordButton.setEnabled(true);
        stopRecordButton.setEnabled(false);
        recognizeFileButton.setEnabled(true);
        saveButton.setEnabled(true);
    }

    // ===== РАСПОЗНАВАНИЕ ИЗ ФАЙЛА =====
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

                if (text == null || text.trim().isEmpty()) {
                    throw new IOException("Пустой результат");
                }

                // Исправляем кодировку
                text = fixTextEncoding(text);

                // Вывод в GUI
                textArea.append("[Файл: " + audioFile.getName() + "]\n");
                textArea.append(text + "\n\n");
                statusLabel.setText("Файл распознан: " + audioFile.getName());
                saveButton.setEnabled(true);

                // Показ результата
                showResultDialog("Результат распознавания файла",
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

        if (text.matches(".*[А-Яа-яЁё].*")) {
            return text;
        }

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
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();
            window.setVisible(true);

            // Проверка наличия папки recordings
            File recordingsDir = new File("recordings");
            if (!recordingsDir.exists()) {
                recordingsDir.mkdir();
            }
        });
    }
}
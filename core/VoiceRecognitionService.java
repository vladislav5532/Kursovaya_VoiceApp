package core;

import org.vosk.Model;
import org.vosk.Recognizer;
import javax.sound.sampled.*;
import java.io.*;
import com.google.gson.*;

public class VoiceRecognitionService {
    private Model model;
    private Recognizer recognizer;
    private boolean isRunning = false;
    private TargetDataLine microphone;
    private RecognitionCallback callback;

    public interface RecognitionCallback {
        void onTextRecognized(String text);
        void onError(String error);
        void onStatus(String status);
        void onPartialResult(String partial);
    }

    public VoiceRecognitionService(String modelPath) throws IOException {
        System.out.println("Загрузка модели из: " + new File(modelPath).getAbsolutePath());
        this.model = new Model(modelPath);
        this.recognizer = new Recognizer(model, 16000.0f);
    }

    public void setCallback(RecognitionCallback callback) {
        this.callback = callback;
    }

    // ===== РАСПОЗНАВАНИЕ ИЗ ФАЙЛА =====
    public String recognizeAudioFile(File audioFile) throws IOException {
        AudioInputStream originalStream = null;

        try {
            System.out.println("=== ОБРАБОТКА ФАЙЛА ===");
            System.out.println("Имя: " + audioFile.getName());

            try {
                originalStream = AudioSystem.getAudioInputStream(audioFile);
            } catch (UnsupportedAudioFileException e) {
                throw new IOException("Формат файла не поддерживается. Используйте WAV файл", e);
            }

            AudioFormat originalFormat = originalStream.getFormat();
            System.out.println("Формат: " + originalFormat.getSampleRate() + " Гц, " +
                    originalFormat.getChannels() + " канал(ов)");

            AudioFormat targetFormat = new AudioFormat(16000.0f, 16, 1, true, false);
            AudioInputStream convertedStream = originalStream;

            if (!originalFormat.matches(targetFormat) &&
                    AudioSystem.isConversionSupported(targetFormat, originalFormat)) {
                convertedStream = AudioSystem.getAudioInputStream(targetFormat, originalStream);
                System.out.println("Сконвертирован в 16000 Гц, моно");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;

            recognizer = new Recognizer(model, 16000.0f);

            System.out.println("Распознавание...");
            while ((bytesRead = convertedStream.read(buffer)) >= 0) {
                if (bytesRead > 0) {
                    recognizer.acceptWaveForm(buffer, bytesRead);
                }
            }

            String result = recognizer.getFinalResult();
            System.out.println("Сырой результат Vosk: " + result);

            convertedStream.close();
            originalStream.close();

            String text = extractText(result);
            System.out.println("Извлечённый текст: " + text);

            return text;

        } catch (Exception e) {
            if (originalStream != null) {
                try { originalStream.close(); } catch (Exception ex) {}
            }
            throw new IOException("Ошибка: " + e.getMessage(), e);
        }
    }

    // ===== ЗАПИСЬ С МИКРОФОНА В РЕАЛЬНОМ ВРЕМЕНИ =====
    public void startMicrophoneRecording() {
        if (isRunning) {
            if (callback != null) callback.onError("Запись уже запущена");
            return;
        }

        isRunning = true;

        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                // Проверяем доступность микрофона
                if (!AudioSystem.isLineSupported(info)) {
                    if (callback != null) {
                        callback.onError("Микрофон не поддерживает нужный формат. Используйте другой микрофон или файлы.");
                    }
                    isRunning = false;
                    return;
                }

                microphone = (TargetDataLine) AudioSystem.getLine(info);
                microphone.open(format);
                microphone.start();

                if (callback != null) {
                    callback.onStatus("Запись началась. Говорите в микрофон...");
                }

                recognizer = new Recognizer(model, 16000.0f);
                byte[] buffer = new byte[4096];

                while (isRunning) {
                    int bytesRead = microphone.read(buffer, 0, buffer.length);

                    if (bytesRead > 0) {
                        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                            String result = recognizer.getResult();
                            String text = extractText(result);
                            if (!text.isEmpty() && callback != null) {
                                callback.onTextRecognized(text);
                            }
                        } else {
                            String partial = recognizer.getPartialResult();
                            String partialText = extractText(partial);
                            if (!partialText.isEmpty() && callback != null) {
                                callback.onPartialResult(partialText);
                            }
                        }
                    }
                }

                microphone.stop();
                microphone.close();

                String finalResult = recognizer.getFinalResult();
                String finalText = extractText(finalResult);
                if (!finalText.isEmpty() && callback != null) {
                    callback.onTextRecognized("[Конец записи] " + finalText);
                }

            } catch (Exception e) {
                if (callback != null) {
                    callback.onError("Ошибка записи: " + e.getMessage());
                }
                e.printStackTrace();
            } finally {
                isRunning = false;
            }
        }).start();
    }

    public void stopMicrophoneRecording() {
        isRunning = false;
    }

    public boolean isRecording() {
        return isRunning;
    }

    // ===== ДЕМО-РЕЖИМ =====
    public String recognizeAudioFileDemo(File audioFile) {
        System.out.println("ДЕМО-РЕЖИМ: " + audioFile.getName());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}

        String[] demoTexts = {
                "Привет, это тестовое сообщение",
                "Система распознавания речи работает",
                "Голосовой блокнот преобразует речь в текст",
                "Демонстрация работы курсового проекта",
                "Текст успешно распознан"
        };

        int index = Math.abs(audioFile.getName().hashCode()) % demoTexts.length;
        String result = demoTexts[index] + " (файл: " + audioFile.getName() + ")";

        System.out.println("ДЕМО-текст: " + result);
        return result;
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ =====
    private String extractText(String jsonResult) {
        try {
            JsonElement element = JsonParser.parseString(jsonResult);
            String text = element.getAsJsonObject()
                    .get("text")
                    .getAsString();

            return fixVoskEncoding(text);

        } catch (Exception e) {
            System.out.println("Ошибка парсинга JSON: " + e.getMessage());
            return jsonResult;
        }
    }

    private String fixVoskEncoding(String text) {
        if (text == null || text.isEmpty()) return text;

        try {
            byte[] bytes = text.getBytes("Windows-1251");
            String fixed = new String(bytes, "UTF-8");

            if (!fixed.equals(text)) {
                System.out.println("Исправлена кодировка: '" + text + "' -> '" + fixed + "'");
            }

            return fixed;

        } catch (Exception e) {
            return text;
        }
    }
}
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
    private RecognitionCallback callback;

    public interface RecognitionCallback {
        void onTextRecognized(String text);
        void onError(String error);
        void onStatus(String status);
    }

    public VoiceRecognitionService(String modelPath) throws IOException {
        System.out.println("Загрузка модели из: " + new File(modelPath).getAbsolutePath());
        this.model = new Model(modelPath);
        this.recognizer = new Recognizer(model, 16000.0f);
    }

    public void setCallback(RecognitionCallback callback) {
        this.callback = callback;
    }

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
            // Vosk возвращает UTF-8, но Windows показывает как Windows-1251
            // "раз два три" -> "СБР°Р. РҐРІР° С,СБРё"

            byte[] bytes = text.getBytes("Windows-1251"); // Получаем байты как Windows-1251
            String fixed = new String(bytes, "UTF-8");    // Переинтерпретируем как UTF-8

            if (!fixed.equals(text)) {
                System.out.println("Исправлена кодировка: '" + text + "' -> '" + fixed + "'");
            }

            return fixed;

        } catch (Exception e) {
            return text;
        }
    }

    public void startRecognition() {
        System.out.println("Запись с микрофона отключена");
        if (callback != null) {
            callback.onError("Запись с микрофона недоступна. Используйте аудиофайлы.");
        }
    }

    public void stopRecognition() {
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
}
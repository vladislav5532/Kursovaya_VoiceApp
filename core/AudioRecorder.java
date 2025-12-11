package core;

import javax.sound.sampled.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AudioRecorder {
    private TargetDataLine microphone;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private AudioFormat format;
    private File outputFile;
    private boolean isRecording = false;
    private Thread recordingThread;

    public AudioRecorder() {
        this.format = new AudioFormat(16000.0f, 16, 1, true, false);
    }

    public void startRecording(String outputDir) throws LineUnavailableException, IOException {
        if (isRecording) return;

        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        outputFile = new File(outputDir, "recording_" + timestamp + ".wav");

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Микрофон не поддерживает нужный формат");
        }

        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        isRecording = true;

        recordingThread = new Thread(() -> {
            try (AudioInputStream audioStream = new AudioInputStream(microphone)) {
                System.out.println("Запись начата: " + outputFile.getName());
                AudioSystem.write(audioStream, fileType, outputFile);
                System.out.println("Запись сохранена: " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Ошибка записи в файл: " + e.getMessage());
            }
        });

        recordingThread.start();
    }

    public File stopRecording() {
        if (!isRecording) return null;

        isRecording = false;
        microphone.stop();
        microphone.close();

        try {
            recordingThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return outputFile;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public File getCurrentFile() {
        return outputFile;
    }
}
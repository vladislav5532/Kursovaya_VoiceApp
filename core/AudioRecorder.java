package core;

import javax.sound.sampled.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AudioRecorder {
    private TargetDataLine microphone;
    private File outputFile;
    private boolean isRecording = false;

    public void startRecording(String outputDir) throws LineUnavailableException {
        if (isRecording) return;

        AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Микрофон не поддерживает формат");
        }

        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        outputFile = new File(outputDir, "recording_" + timestamp + ".wav");

        microphone = (TargetDataLine) AudioSystem.getLine(info);
        microphone.open(format);
        microphone.start();

        isRecording = true;

        new Thread(() -> {
            try (AudioInputStream audioStream = new AudioInputStream(microphone)) {
                AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public File stopRecording() {
        if (!isRecording) return null;

        isRecording = false;
        microphone.stop();
        microphone.close();

        return outputFile;
    }

    public boolean isRecording() {
        return isRecording;
    }
}
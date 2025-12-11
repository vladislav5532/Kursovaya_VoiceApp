package core;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileManager {
    private Path notesDir;
    private Path recordingsDir;

    public FileManager(String basePath) throws IOException {
        this.notesDir = Paths.get(basePath, "notes");
        this.recordingsDir = Paths.get(basePath, "recordings");

        Files.createDirectories(notesDir);
        Files.createDirectories(recordingsDir);
    }

    public void saveNote(String text, String title) throws IOException {
        if (text == null || text.trim().isEmpty()) return;

        String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String filename = (title != null && !title.isEmpty())
                ? title + ".txt"
                : "note_" + timestamp + ".txt";

        Path filePath = notesDir.resolve(filename);

        try (BufferedWriter writer = Files.newBufferedWriter(filePath,
                java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write("Дата: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
            writer.newLine();
            writer.write("=".repeat(50));
            writer.newLine();
            writer.write(text);
        }
    }

    public List<String> loadRecentNotes(int count) throws IOException {
        List<String> notes = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(notesDir, "*.txt")) {
            List<Path> files = new ArrayList<>();
            stream.forEach(files::add);

            files.sort((f1, f2) -> {
                try {
                    return Files.getLastModifiedTime(f2).compareTo(
                            Files.getLastModifiedTime(f1));
                } catch (IOException e) {
                    return 0;
                }
            });

            for (int i = 0; i < Math.min(count, files.size()); i++) {
                notes.add(Files.readString(files.get(i),
                        java.nio.charset.StandardCharsets.UTF_8));
            }
        }

        return notes;
    }

    public Path getRecordingsDir() {
        return recordingsDir;
    }

    public Path getNotesDir() {
        return notesDir;
    }
}
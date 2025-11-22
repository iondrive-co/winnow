package org.win.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.UUID;

public class UndoManager {
    private final LinkedList<UndoOperation> undoQueue;
    private final Path tempDirectory;

    public UndoManager() throws IOException {
        this.undoQueue = new LinkedList<>();
        this.tempDirectory = Files.createTempDirectory("winnow-undo-" + UUID.randomUUID());
        this.tempDirectory.toFile().deleteOnExit();

        // Add shutdown hook to ensure cleanup on abnormal termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                cleanup();
            } catch (Exception e) {
                // Best effort cleanup
            }
        }));
    }

    public void saveStateBeforeOperation(File originalFile) throws IOException {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String backupFilename = timestamp + "-" + originalFile.getName();
        Path backupPath = tempDirectory.resolve(backupFilename);

        // Copy the original file byte-for-byte to preserve quality and metadata
        Files.copy(originalFile.toPath(), backupPath, StandardCopyOption.REPLACE_EXISTING);

        UndoOperation operation = new UndoOperation(originalFile, backupPath.toFile());
        undoQueue.addFirst(operation);
    }

    public void setRenamedFileForLastOperation(File renamedFile) {
        if (!undoQueue.isEmpty()) {
            undoQueue.getFirst().renamedFile = renamedFile;
        }
    }

    public boolean canUndo() {
        return !undoQueue.isEmpty();
    }

    public File undo() throws IOException {
        if (!canUndo()) {
            throw new IllegalStateException("No operations to undo");
        }

        UndoOperation operation = undoQueue.removeFirst();

        if (!operation.backupFile.exists()) {
            throw new IOException("Backup file not found: " + operation.backupFile.getAbsolutePath());
        }

        if (operation.renamedFile != null && operation.renamedFile.exists() && !operation.renamedFile.equals(operation.originalFile)) {
            operation.renamedFile.delete();
        }

        Files.copy(operation.backupFile.toPath(), operation.originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        operation.backupFile.delete();

        return operation.originalFile;
    }

    public void cleanup() {
        for (UndoOperation operation : undoQueue) {
            if (operation.backupFile.exists()) {
                operation.backupFile.delete();
            }
        }
        undoQueue.clear();

        try {
            if (Files.exists(tempDirectory)) {
                Files.walk(tempDirectory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            System.err.println("Error cleaning up temp directory: " + e.getMessage());
        }
    }

    private static class UndoOperation {
        final File originalFile;
        final File backupFile;
        File renamedFile;

        UndoOperation(File originalFile, File backupFile) {
            this.originalFile = originalFile;
            this.backupFile = backupFile;
            this.renamedFile = null;
        }
    }
}

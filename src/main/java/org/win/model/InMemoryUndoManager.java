package org.win.model;

import javafx.scene.image.WritableImage;
import org.win.core.ImageOperations;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Undo manager that stores image snapshots in memory. Useful for browser/web builds
 * where file system access is unavailable.
 */
public class InMemoryUndoManager implements UndoSupport {

    private final Deque<UndoEntry> history = new ArrayDeque<>();

    @Override
    public void saveState(final File originalFile, final WritableImage currentImage) {
        if (currentImage == null || originalFile == null) {
            return;
        }
        final WritableImage copy = ImageOperations.copy(currentImage);
        history.addFirst(new UndoEntry(originalFile, copy));
    }

    @Override
    public void setRenamedFile(final File renamedFile) {
        if (!history.isEmpty()) {
            history.peekFirst().renamedFile = renamedFile;
        }
    }

    @Override
    public boolean canUndo() {
        return !history.isEmpty();
    }

    @Override
    public UndoSnapshot undo() {
        if (!canUndo()) {
            throw new IllegalStateException("No operations to undo");
        }

        final UndoEntry entry = history.removeFirst();

        // If the file was renamed, revert the reference back to the original
        final File targetFile = entry.originalFile;
        final WritableImage restored = ImageOperations.copy(entry.snapshot);
        return new UndoSnapshot(targetFile, restored, entry.renamedFile);
    }

    @Override
    public void cleanup() {
        history.clear();
    }

    private static final class UndoEntry {
        final File originalFile;
        final WritableImage snapshot;
        File renamedFile;

        UndoEntry(final File originalFile, final WritableImage snapshot) {
            this.originalFile = originalFile;
            this.snapshot = snapshot;
        }
    }
}

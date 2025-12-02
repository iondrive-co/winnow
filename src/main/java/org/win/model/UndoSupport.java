package org.win.model;

import javafx.scene.image.WritableImage;

import java.io.File;
import java.io.IOException;

/**
 * Abstraction for undo support so we can swap file-based vs in-memory implementations.
 */
public interface UndoSupport {

    /**
     * Capture a snapshot before an operation.
     * @param originalFile file identifier for the image
     * @param currentImage image contents to snapshot (may be ignored by file-based implementations)
     */
    void saveState(File originalFile, WritableImage currentImage) throws IOException;

    /**
     * Record that the last operation renamed the file.
     */
    void setRenamedFile(File renamedFile);

    boolean canUndo();

    UndoSnapshot undo() throws IOException;

    default void cleanup() {
        // Optional
    }
}

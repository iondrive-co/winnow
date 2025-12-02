package org.win.core;

/**
 * Interface for managing undo history of image operations.
 * Allows different implementations for desktop (file-based) and web (memory-based).
 */
public interface UndoHistory {

    /**
     * Saves the current image state before an operation.
     * The image is copied, so the original can be safely modified.
     *
     * @param image    The current image state to save
     * @param filename The current filename (for operations that track renames)
     */
    void saveState(PixelImage image, String filename);

    /**
     * Checks if there are any operations to undo.
     *
     * @return true if undo is available
     */
    boolean canUndo();

    /**
     * Restores the previous state and returns the restored image.
     *
     * @return The restored image and filename
     * @throws IllegalStateException if no operations to undo
     */
    UndoResult undo();

    /**
     * Clears all undo history and releases resources.
     */
    void clear();

    /**
     * Returns the number of states available for undo.
     */
    int size();

    /**
     * Result of an undo operation containing both image and filename.
     */
    record UndoResult(PixelImage image, String originalFilename) {}
}

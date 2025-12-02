package org.win.core;

/**
 * Unified undo facade for pixel images.
 */
public interface ImageUndo {
    void save(PixelImage image, String name);
    boolean canUndo();
    UndoEntry undo();
    void clear();

    record UndoEntry(PixelImage image, String name) {}
}

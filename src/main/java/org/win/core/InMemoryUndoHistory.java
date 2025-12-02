package org.win.core;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * In-memory implementation of UndoHistory.
 * Stores ARGB pixel copies in memory - suitable for both desktop and web.
 * For web deployment, this is the only option as file system is not available.
 * For desktop, can be used for quick operations or replaced with file-based for large images.
 */
public final class InMemoryUndoHistory implements UndoHistory {

    private static final int DEFAULT_MAX_SIZE = 10;

    private final Deque<UndoEntry> history;
    private final int maxSize;

    public InMemoryUndoHistory() {
        this(DEFAULT_MAX_SIZE);
    }

    public InMemoryUndoHistory(final int maxSize) {
        this.maxSize = maxSize;
        this.history = new ArrayDeque<>();
    }

    @Override
    public void saveState(final PixelImage image, final String filename) {
        final PixelImage copy = PixelImageOps.copy(image);
        final UndoEntry entry = new UndoEntry(copy, filename);
        history.addFirst(entry);

        // Trim to max size
        while (history.size() > maxSize) {
            history.removeLast();
        }
    }

    @Override
    public boolean canUndo() {
        return !history.isEmpty();
    }

    @Override
    public UndoResult undo() {
        if (!canUndo()) {
            throw new IllegalStateException("No operations to undo");
        }

        final UndoEntry entry = history.removeFirst();
        return new UndoResult(entry.image, entry.originalFilename);
    }

    @Override
    public void clear() {
        history.clear();
    }

    @Override
    public int size() {
        return history.size();
    }

    /**
     * Returns the maximum number of states this history will store.
     */
    public int getMaxSize() {
        return maxSize;
    }

    private static final class UndoEntry {
        final PixelImage image;
        final String originalFilename;

        UndoEntry(final PixelImage image, final String originalFilename) {
            this.image = image;
            this.originalFilename = originalFilename;
        }
    }
}

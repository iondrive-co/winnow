package org.win.core;

/**
 * In-memory ImageUndo implementation backed by InMemoryUndoHistory.
 */
public final class InMemoryImageUndo implements ImageUndo {
    private final InMemoryUndoHistory delegate;

    public InMemoryImageUndo(final int maxSize) {
        this.delegate = new InMemoryUndoHistory(maxSize);
    }

    public InMemoryImageUndo() {
        this(10);
    }

    @Override
    public void save(final PixelImage image, final String name) {
        delegate.saveState(image, name);
    }

    @Override
    public boolean canUndo() {
        return delegate.canUndo();
    }

    @Override
    public UndoEntry undo() {
        final UndoHistory.UndoResult result = delegate.undo();
        return new UndoEntry(result.image(), result.originalFilename());
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}

package org.win.model;

import javafx.scene.image.WritableImage;

import java.io.File;
import java.io.IOException;

/**
 * Adapter to reuse the existing file-based UndoManager behind the new UndoSupport API.
 */
public class UndoManagerAdapter implements UndoSupport {
    public final UndoManager delegate;

    public UndoManagerAdapter(final UndoManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public void saveState(final File originalFile, final WritableImage currentImage) throws IOException {
        delegate.saveStateBeforeOperation(originalFile);
    }

    @Override
    public void setRenamedFile(final File renamedFile) {
        delegate.setRenamedFileForLastOperation(renamedFile);
    }

    @Override
    public boolean canUndo() {
        return delegate.canUndo();
    }

    @Override
    public UndoSnapshot undo() throws IOException {
        return new UndoSnapshot(delegate.undo(), null, null);
    }

    @Override
    public void cleanup() {
        delegate.cleanup();
    }
}

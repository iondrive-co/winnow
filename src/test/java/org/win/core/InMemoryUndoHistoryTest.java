package org.win.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InMemoryUndoHistoryTest {

    @Test
    public void saveAndUndoRoundTrip() {
        final InMemoryUndoHistory history = new InMemoryUndoHistory();
        final PixelImage image = filled(4, 4, 0xFF00FF00);

        history.saveState(image, "a.png");

        final UndoHistory.UndoResult result = history.undo();
        assertThat(result.originalFilename()).isEqualTo("a.png");
        assertThat(result.image().getWidth()).isEqualTo(4);
        assertThat(result.image().getHeight()).isEqualTo(4);
        assertThat(result.image().getArgb(2, 2)).isEqualTo(0xFF00FF00);
    }

    @Test
    public void undoIsLifo() {
        final InMemoryUndoHistory history = new InMemoryUndoHistory();
        history.saveState(filled(2, 2, 1), "first");
        history.saveState(filled(2, 2, 2), "second");

        assertThat(history.undo().originalFilename()).isEqualTo("second");
        assertThat(history.undo().originalFilename()).isEqualTo("first");
        assertThat(history.canUndo()).isFalse();
    }

    @Test
    public void maxSizeTrimsOldest() {
        final InMemoryUndoHistory history = new InMemoryUndoHistory(2);
        history.saveState(filled(1, 1, 1), "one");
        history.saveState(filled(1, 1, 2), "two");
        history.saveState(filled(1, 1, 3), "three");

        assertThat(history.size()).isEqualTo(2);
        assertThat(history.undo().originalFilename()).isEqualTo("three");
        assertThat(history.undo().originalFilename()).isEqualTo("two");
    }

    @Test
    public void undoOnEmptyThrows() {
        final InMemoryUndoHistory history = new InMemoryUndoHistory();
        assertThatThrownBy(history::undo).isInstanceOf(IllegalStateException.class);
    }

    private static PixelImage filled(final int w, final int h, final int argb) {
        final PixelImage image = new PixelImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setArgb(x, y, argb);
            }
        }
        return image;
    }
}

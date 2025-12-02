package org.win.web;

import org.junit.Test;
import org.win.core.InteractionController;
import org.win.core.PixelImage;
import org.win.core.SelectionAdjuster;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures the shortcut/zoom semantics used by the browser adapter match the desktop expectations.
 */
public class WebSelectionShortcutParityTest {

    @Test
    public void wheelZoomsClampToDesktopLimits() {
        final InteractionController controller = new InteractionController(new PixelImage(100, 100));

        controller.zoom(100);
        assertThat(controller.zoom()).isEqualTo(InteractionController.MAX_ZOOM);

        controller.zoom(0.001);
        assertThat(controller.zoom()).isEqualTo(InteractionController.MIN_ZOOM);
    }

    @Test
    public void selectionShortcutsMirrorDesktop() {
        final InteractionController controller = new InteractionController(new PixelImage(200, 200));
        controller.selection().setRegion(50, 50, 100, 100);

        assertThat(SelectionAdjuster.applyShortcut(68, true, false, 10, controller.selection())).isTrue(); // Ctrl+D expand right
        assertThat(controller.selection().getBottomRightX()).isEqualTo(110);

        assertThat(SelectionAdjuster.applyShortcut(68, true, true, 10, controller.selection())).isTrue(); // Ctrl+Shift+D expand left
        assertThat(controller.selection().getTopLeftX()).isEqualTo(40);

        assertThat(SelectionAdjuster.applyShortcut(65, true, true, 10, controller.selection())).isTrue(); // Ctrl+Shift+A reduce left
        assertThat(controller.selection().getTopLeftX()).isEqualTo(50);

        assertThat(SelectionAdjuster.applyShortcut(83, true, true, 20, controller.selection())).isTrue(); // Ctrl+Shift+S expand up
        assertThat(controller.selection().getTopLeftY()).isEqualTo(30);

        assertThat(SelectionAdjuster.applyShortcut(87, true, true, 15, controller.selection())).isTrue(); // Ctrl+Shift+W reduce top
        assertThat(controller.selection().getTopLeftY()).isEqualTo(45);
    }
}

package org.win.web;

import org.junit.Test;
import org.win.core.InteractionController;
import org.win.core.PixelImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against collapsing the selection to 1x1 during drags.
 */
public class WebSelectionStabilityTest {

    @Test
    public void dragKeepsNonZeroSelection() {
        final InteractionController controller = new InteractionController(new PixelImage(400, 300));
        controller.centerSelection();

        controller.selection().setRegion(100, 100, 120, 120);
        controller.dragSelection(130, 140);

        assertThat(controller.selection().getWidthInt()).isGreaterThan(1);
        assertThat(controller.selection().getHeightInt()).isGreaterThan(1);
    }
}

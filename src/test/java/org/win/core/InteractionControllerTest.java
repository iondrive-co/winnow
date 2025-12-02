package org.win.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InteractionControllerTest {

    @Test
    public void setSelectionSizeClampsWithinImage() {
        final InteractionController controller = new InteractionController(new PixelImage(200, 100));
        controller.selection().setRegion(10, 10, 60, 40);

        controller.setSelectionSize(50, 30);
        assertThat(controller.selection().getWidthInt()).isEqualTo(50);
        assertThat(controller.selection().getHeightInt()).isEqualTo(30);

        controller.setSelectionSize(500, 500);
        assertThat(controller.selection().getWidthInt()).isEqualTo(190); // 200 - left 10
        assertThat(controller.selection().getHeightInt()).isEqualTo(90); // 100 - top 10
    }
}

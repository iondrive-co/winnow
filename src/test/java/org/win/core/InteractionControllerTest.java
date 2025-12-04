package org.win.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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

    @Test
    public void zoomToFitScalesLargeImageDown() {
        final InteractionController controller = new InteractionController(new PixelImage(2000, 1500));
        controller.zoomToFit(900, 700);

        // Image should scale down to fit within 900x700
        // Scale factor should be min(900/2000, 700/1500) = min(0.45, 0.467) = 0.45
        assertThat(controller.zoom()).isCloseTo(0.45, within(0.01));
    }

    @Test
    public void zoomToFitDoesNotScaleSmallImageUp() {
        final InteractionController controller = new InteractionController(new PixelImage(400, 300));
        controller.zoomToFit(900, 700);

        // Image is smaller than viewport, should stay at zoom 1.0 (no upscaling)
        assertThat(controller.zoom()).isEqualTo(1.0);
    }

    @Test
    public void zoomToFitHandlesWideImage() {
        final InteractionController controller = new InteractionController(new PixelImage(1800, 400));
        controller.zoomToFit(900, 700);

        // Wide image: scale factor = min(900/1800, 700/400) = min(0.5, 1.75) = 0.5
        assertThat(controller.zoom()).isCloseTo(0.5, within(0.01));
    }

    @Test
    public void zoomToFitHandlesTallImage() {
        final InteractionController controller = new InteractionController(new PixelImage(400, 1400));
        controller.zoomToFit(900, 700);

        // Tall image: scale factor = min(900/400, 700/1400) = min(2.25, 0.5) = 0.5
        assertThat(controller.zoom()).isCloseTo(0.5, within(0.01));
    }

    @Test
    public void selectionCoordinatesRemainInImageSpaceAfterZoom() {
        final InteractionController controller = new InteractionController(new PixelImage(1000, 800));
        controller.selection().setRegion(100, 100, 900, 700);

        // Verify initial selection in image space
        assertThat(controller.selection().getTopLeftX()).isEqualTo(100.0);
        assertThat(controller.selection().getTopLeftY()).isEqualTo(100.0);
        assertThat(controller.selection().getWidth()).isEqualTo(800.0);
        assertThat(controller.selection().getHeight()).isEqualTo(600.0);

        // Zoom out
        controller.zoom(0.5);

        // Selection coordinates should remain in image space (unchanged)
        assertThat(controller.selection().getTopLeftX()).isEqualTo(100.0);
        assertThat(controller.selection().getTopLeftY()).isEqualTo(100.0);
        assertThat(controller.selection().getWidth()).isEqualTo(800.0);
        assertThat(controller.selection().getHeight()).isEqualTo(600.0);

        // Zoom in
        controller.zoom(4.0);

        // Selection coordinates should still be in image space
        assertThat(controller.selection().getTopLeftX()).isEqualTo(100.0);
        assertThat(controller.selection().getTopLeftY()).isEqualTo(100.0);
        assertThat(controller.selection().getWidth()).isEqualTo(800.0);
        assertThat(controller.selection().getHeight()).isEqualTo(600.0);
    }

    @Test
    public void calculateScreenPositionOfSelectionAtZoom1() {
        final InteractionController controller = new InteractionController(new PixelImage(800, 600));
        controller.selection().setRegion(0, 0, 800, 600);

        final double zoom = controller.zoom(); // 1.0
        final int viewportWidth = 900;
        final int viewportHeight = 700;

        // Calculate what the screen position should be
        final int scaledWidth = (int) (800 * zoom);
        final int scaledHeight = (int) (600 * zoom);
        final double offsetX = Math.max(0, (viewportWidth - scaledWidth) / 2.0);
        final double offsetY = Math.max(0, (viewportHeight - scaledHeight) / 2.0);

        // Selection at (0,0) in image space should be at offset in screen space
        final double screenLeft = controller.selection().getTopLeftX() * zoom + offsetX;
        final double screenTop = controller.selection().getTopLeftY() * zoom + offsetY;

        assertThat(screenLeft).isEqualTo(50.0); // (900 - 800) / 2
        assertThat(screenTop).isEqualTo(50.0);  // (700 - 600) / 2
    }

    @Test
    public void calculateScreenPositionOfSelectionAtZoomOut() {
        final InteractionController controller = new InteractionController(new PixelImage(800, 600));
        controller.selection().setRegion(0, 0, 800, 600);
        controller.zoom(0.5); // Zoom out to 0.5

        final double zoom = controller.zoom();
        final int viewportWidth = 900;
        final int viewportHeight = 700;

        // Calculate screen position
        final int scaledWidth = (int) (800 * zoom); // 400
        final int scaledHeight = (int) (600 * zoom); // 300
        final double offsetX = Math.max(0, (viewportWidth - scaledWidth) / 2.0);
        final double offsetY = Math.max(0, (viewportHeight - scaledHeight) / 2.0);

        // Selection at (0,0) in image space
        final double screenLeft = controller.selection().getTopLeftX() * zoom + offsetX;
        final double screenTop = controller.selection().getTopLeftY() * zoom + offsetY;

        assertThat(screenLeft).isEqualTo(250.0); // (900 - 400) / 2
        assertThat(screenTop).isEqualTo(200.0);  // (700 - 300) / 2
    }

    @Test
    public void calculateScreenPositionOfSelectionAtZoomIn() {
        final InteractionController controller = new InteractionController(new PixelImage(800, 600));
        controller.selection().setRegion(0, 0, 800, 600);
        controller.zoom(2.0); // Zoom in to 2.0

        final double zoom = controller.zoom();
        final int viewportWidth = 900;
        final int viewportHeight = 700;

        // Calculate screen position
        final int scaledWidth = (int) (800 * zoom); // 1600
        final int scaledHeight = (int) (600 * zoom); // 1200
        final double offsetX = Math.max(0, (viewportWidth - scaledWidth) / 2.0);
        final double offsetY = Math.max(0, (viewportHeight - scaledHeight) / 2.0);

        // When zoomed in beyond viewport, offset should be 0
        assertThat(offsetX).isEqualTo(0.0);
        assertThat(offsetY).isEqualTo(0.0);

        // Selection at (0,0) in image space should be at (0,0) in screen space
        final double screenLeft = controller.selection().getTopLeftX() * zoom + offsetX;
        final double screenTop = controller.selection().getTopLeftY() * zoom + offsetY;

        assertThat(screenLeft).isEqualTo(0.0);
        assertThat(screenTop).isEqualTo(0.0);
    }
}

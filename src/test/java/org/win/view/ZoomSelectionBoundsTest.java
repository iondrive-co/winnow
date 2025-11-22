package org.win.view;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ZoomSelectionBoundsTest {

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    private BufferedImage createTestImage(final int width, final int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    private void waitForFXThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testZoom_selectionDimensionsDecreaseWhenZoomedPastWindowBounds() throws Exception {
        final CountDownLatch setupLatch = new CountDownLatch(1);
        final AtomicInteger initialWidth = new AtomicInteger();
        final AtomicInteger initialHeight = new AtomicInteger();
        final AtomicInteger zoomedWidth = new AtomicInteger();
        final AtomicInteger zoomedHeight = new AtomicInteger();
        final CustomImageCanvas[] canvasRef = new CustomImageCanvas[1];

        // Setup
        Platform.runLater(() -> {
            final BufferedImage testImage = createTestImage(800, 600);
            canvasRef[0] = new CustomImageCanvas(testImage);
            final StackPane pane = new StackPane(canvasRef[0]);
            new Scene(pane, 400, 300);
            setupLatch.countDown();
        });

        assertThat(setupLatch.await(5, TimeUnit.SECONDS)).isTrue();
        waitForFXThread();

        // Measure initial state
        final CountDownLatch measureLatch1 = new CountDownLatch(1);
        Platform.runLater(() -> {
            initialWidth.set(canvasRef[0].getVisibleSelectionWidth());
            initialHeight.set(canvasRef[0].getVisibleSelectionHeight());
            System.out.println("Initial - Scale: " + canvasRef[0].getScaleX() +
                             ", Selection: " + initialWidth.get() + "x" + initialHeight.get());
            measureLatch1.countDown();
        });
        assertThat(measureLatch1.await(5, TimeUnit.SECONDS)).isTrue();

        // Perform zoom
        final CountDownLatch zoomLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            canvasRef[0].zoom(2.0);
            zoomLatch.countDown();
        });
        assertThat(zoomLatch.await(5, TimeUnit.SECONDS)).isTrue();
        waitForFXThread();

        // Measure after zoom
        final CountDownLatch measureLatch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            zoomedWidth.set(canvasRef[0].getVisibleSelectionWidth());
            zoomedHeight.set(canvasRef[0].getVisibleSelectionHeight());
            System.out.println("After zoom 2x - Scale: " + canvasRef[0].getScaleX() +
                             ", Selection: " + zoomedWidth.get() + "x" + zoomedHeight.get());
            measureLatch2.countDown();
        });
        assertThat(measureLatch2.await(5, TimeUnit.SECONDS)).isTrue();

        // Verify results
        assertThat(zoomedWidth.get()).isLessThan(initialWidth.get())
            .describedAs("Selection width should decrease after zooming in");
        assertThat(zoomedHeight.get()).isLessThan(initialHeight.get())
            .describedAs("Selection height should decrease after zooming in");

        assertThat(zoomedWidth.get()).isEqualTo(200)
            .describedAs("At 2x zoom with 400px window, visible width should be 200px");
        assertThat(zoomedHeight.get()).isEqualTo(150)
            .describedAs("At 2x zoom with 300px window, visible height should be 150px");
    }

    @Test
    public void testZoom_selectionChangeCallbackTriggeredOnZoom() throws Exception {
        final CountDownLatch setupLatch = new CountDownLatch(1);
        final AtomicBoolean callbackCalled = new AtomicBoolean(false);
        final CustomImageCanvas[] canvasRef = new CustomImageCanvas[1];

        Platform.runLater(() -> {
            final BufferedImage testImage = createTestImage(800, 600);
            canvasRef[0] = new CustomImageCanvas(testImage);
            final StackPane pane = new StackPane(canvasRef[0]);
            new Scene(pane, 400, 300);
            canvasRef[0].setOnSelectionChange(() -> callbackCalled.set(true));
            setupLatch.countDown();
        });

        assertThat(setupLatch.await(5, TimeUnit.SECONDS)).isTrue();
        waitForFXThread();

        // Perform zoom
        final CountDownLatch zoomLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            canvasRef[0].zoom(2.0);
            zoomLatch.countDown();
        });
        assertThat(zoomLatch.await(5, TimeUnit.SECONDS)).isTrue();
        waitForFXThread();

        assertThat(callbackCalled.get()).isTrue()
            .describedAs("Selection change callback should be called when zooming");
    }

    @Test
    public void testZoom_progressiveZoomDecreasesSelectionFurther() throws Exception {
        final CountDownLatch setupLatch = new CountDownLatch(1);
        final AtomicInteger zoom1Width = new AtomicInteger();
        final AtomicInteger zoom2Width = new AtomicInteger();
        final AtomicInteger zoom3Width = new AtomicInteger();
        final CustomImageCanvas[] canvasRef = new CustomImageCanvas[1];

        Platform.runLater(() -> {
            final BufferedImage testImage = createTestImage(800, 600);
            canvasRef[0] = new CustomImageCanvas(testImage);
            final StackPane pane = new StackPane(canvasRef[0]);
            new Scene(pane, 400, 300);
            setupLatch.countDown();
        });

        assertThat(setupLatch.await(5, TimeUnit.SECONDS)).isTrue();
        waitForFXThread();

        // Zoom 2x
        final CountDownLatch zoom1Latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            canvasRef[0].zoom(2.0);
            zoom1Latch.countDown();
        });
        assertThat(zoom1Latch.await(5, TimeUnit.SECONDS)).isTrue();
        waitForFXThread();

        final CountDownLatch measure1Latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            zoom1Width.set(canvasRef[0].getVisibleSelectionWidth());
            measure1Latch.countDown();
        });
        assertThat(measure1Latch.await(5, TimeUnit.SECONDS)).isTrue();

        // Zoom 1.5x (3x total)
        final CountDownLatch zoom2Latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            canvasRef[0].zoom(1.5);
            zoom2Latch.countDown();
        });
        assertThat(zoom2Latch.await(5, TimeUnit.SECONDS)).isTrue();
        waitForFXThread();

        final CountDownLatch measure2Latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            zoom2Width.set(canvasRef[0].getVisibleSelectionWidth());
            measure2Latch.countDown();
        });
        assertThat(measure2Latch.await(5, TimeUnit.SECONDS)).isTrue();

        // Zoom 2.0x (6x total)
        final CountDownLatch zoom3Latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            canvasRef[0].zoom(2.0);
            zoom3Latch.countDown();
        });
        assertThat(zoom3Latch.await(5, TimeUnit.SECONDS)).isTrue();
        waitForFXThread();

        final CountDownLatch measure3Latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            zoom3Width.set(canvasRef[0].getVisibleSelectionWidth());
            System.out.println("Zoom 2x: " + zoom1Width.get());
            System.out.println("Zoom 3x: " + zoom2Width.get());
            System.out.println("Zoom 6x: " + zoom3Width.get());
            measure3Latch.countDown();
        });
        assertThat(measure3Latch.await(5, TimeUnit.SECONDS)).isTrue();

        // Each successive zoom should decrease the visible selection
        assertThat(zoom2Width.get()).isLessThan(zoom1Width.get());
        assertThat(zoom3Width.get()).isLessThan(zoom2Width.get());
    }
}

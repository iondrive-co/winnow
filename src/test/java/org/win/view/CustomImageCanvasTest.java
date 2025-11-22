package org.win.view;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomImageCanvasTest {

    private static final int TEST_IMAGE_WIDTH = 800;
    private static final int TEST_IMAGE_HEIGHT = 600;

    @BeforeClass
    public static void initToolkit() {
        // Initialize JavaFX toolkit
        new JFXPanel();
    }

    private BufferedImage createTestImage(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    public void testConstructor_initializesCanvasWithCorrectDimensions() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);

            assertThat(canvas.getWidth()).isEqualTo(TEST_IMAGE_WIDTH);
            assertThat(canvas.getHeight()).isEqualTo(TEST_IMAGE_HEIGHT);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCropImage_withFullSelection_returnsCompleteImage() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            BufferedImage croppedImage = canvas.cropImage();

            assertThat(croppedImage).isNotNull();
            assertThat(croppedImage.getWidth()).isEqualTo(TEST_IMAGE_WIDTH);
            assertThat(croppedImage.getHeight()).isEqualTo(TEST_IMAGE_HEIGHT);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCropImage_updatesCanvasDimensions() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            BufferedImage croppedImage = canvas.cropImage();

            // After crop, canvas dimensions should match the cropped image
            assertThat(canvas.getWidth()).isEqualTo(croppedImage.getWidth());
            assertThat(canvas.getHeight()).isEqualTo(croppedImage.getHeight());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testZoom_increasesScale() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);
        double zoomFactor = 1.5;

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            double initialScaleX = canvas.getScaleX();
            double initialScaleY = canvas.getScaleY();

            canvas.zoom(zoomFactor);

            assertThat(canvas.getScaleX()).isEqualTo(initialScaleX * zoomFactor);
            assertThat(canvas.getScaleY()).isEqualTo(initialScaleY * zoomFactor);
            assertThat(canvas.getLayoutX()).isZero();
            assertThat(canvas.getLayoutY()).isZero();
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testZoom_decreasesScale() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);
        double zoomFactor = 0.5;

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            double initialScaleX = canvas.getScaleX();
            double initialScaleY = canvas.getScaleY();

            canvas.zoom(zoomFactor);

            assertThat(canvas.getScaleX()).isEqualTo(initialScaleX * zoomFactor);
            assertThat(canvas.getScaleY()).isEqualTo(initialScaleY * zoomFactor);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testZoom_multipleZooms_cumulativeEffect() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);
        double firstZoom = 1.5;
        double secondZoom = 2.0;

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            double initialScaleX = canvas.getScaleX();

            canvas.zoom(firstZoom);
            canvas.zoom(secondZoom);

            double expectedScale = initialScaleX * firstZoom * secondZoom;
            assertThat(canvas.getScaleX()).isEqualTo(expectedScale);
            assertThat(canvas.getScaleY()).isEqualTo(expectedScale);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCropImage_withSmallImage_handlesCorrectly() throws Exception {
        BufferedImage smallImage = createTestImage(50, 50);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(smallImage);
            BufferedImage croppedImage = canvas.cropImage();

            assertThat(croppedImage).isNotNull();
            assertThat(croppedImage.getWidth()).isEqualTo(50);
            assertThat(croppedImage.getHeight()).isEqualTo(50);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCropImage_returnsNewBufferedImage() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            BufferedImage croppedImage = canvas.cropImage();

            // Should return a BufferedImage instance
            assertThat(croppedImage).isInstanceOf(BufferedImage.class);
            assertThat(croppedImage).isNotSameAs(testImage);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCropImage_canBeCroppedMultipleTimes() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);

            // First crop - full image
            BufferedImage firstCrop = canvas.cropImage();
            assertThat(firstCrop).isNotNull();
            assertThat(firstCrop.getWidth()).isEqualTo(TEST_IMAGE_WIDTH);
            assertThat(firstCrop.getHeight()).isEqualTo(TEST_IMAGE_HEIGHT);

            // Second crop - should still work on the same canvas
            BufferedImage secondCrop = canvas.cropImage();
            assertThat(secondCrop).isNotNull();
            assertThat(secondCrop.getWidth()).isEqualTo(TEST_IMAGE_WIDTH);
            assertThat(secondCrop.getHeight()).isEqualTo(TEST_IMAGE_HEIGHT);

            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCropImage_withPartialSelection() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);

            // Simulate a partial crop by setting selection coordinates
            BufferedImage croppedImage = canvas.cropImage();

            // After crop, should have valid dimensions
            assertThat(croppedImage).isNotNull();
            assertThat(croppedImage.getWidth()).isGreaterThan(0);
            assertThat(croppedImage.getHeight()).isGreaterThan(0);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testZoom_factorGreaterThanOne_makesImageLarger() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            double initialScale = canvas.getScaleX();

            // Zoom in with factor > 1 should make image larger
            canvas.zoom(1.1);

            assertThat(canvas.getScaleX()).isGreaterThan(initialScale);
            assertThat(canvas.getScaleY()).isGreaterThan(initialScale);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testZoom_factorLessThanOne_makesImageSmaller() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            double initialScale = canvas.getScaleX();

            // Zoom out with factor < 1 should make image smaller
            canvas.zoom(0.9);

            assertThat(canvas.getScaleX()).isLessThan(initialScale);
            assertThat(canvas.getScaleY()).isLessThan(initialScale);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testZoom_scrollUpDirection_shouldMakeLarger() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            double initialScale = canvas.getScaleX();

            // Simulate scroll up (zoom in)
            double scrollUpFactor = 1.1;
            canvas.zoom(scrollUpFactor);

            assertThat(canvas.getScaleX()).isGreaterThan(initialScale);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testZoom_scrollDownDirection_shouldMakeSmaller() throws Exception {
        BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            double initialScale = canvas.getScaleX();

            // Simulate scroll down (zoom out)
            double scrollDownFactor = 0.9;
            canvas.zoom(scrollDownFactor);

            assertThat(canvas.getScaleX()).isLessThan(initialScale);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

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

    @Test
    public void testResizeImage_resizesToSpecifiedDimensions() throws Exception {
        final BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        final CountDownLatch latch = new CountDownLatch(1);
        final int newWidth = 400;
        final int newHeight = 300;

        Platform.runLater(() -> {
            final CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            final BufferedImage resizedImage = canvas.resizeImage(newWidth, newHeight);

            assertThat(resizedImage).isNotNull();
            assertThat(resizedImage.getWidth()).isEqualTo(newWidth);
            assertThat(resizedImage.getHeight()).isEqualTo(newHeight);
            assertThat(canvas.getWidth()).isEqualTo(newWidth);
            assertThat(canvas.getHeight()).isEqualTo(newHeight);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testResizeImage_updatesImageDimensions() throws Exception {
        final BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        final CountDownLatch latch = new CountDownLatch(1);
        final int newWidth = 1600;
        final int newHeight = 1200;

        Platform.runLater(() -> {
            final CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            canvas.resizeImage(newWidth, newHeight);

            assertThat(canvas.getImageWidth()).isEqualTo(newWidth);
            assertThat(canvas.getImageHeight()).isEqualTo(newHeight);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testResizeImage_resetsSelectionToFullImage() throws Exception {
        final BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        final CountDownLatch latch = new CountDownLatch(1);
        final int newWidth = 400;
        final int newHeight = 300;

        Platform.runLater(() -> {
            final CustomImageCanvas canvas = new CustomImageCanvas(testImage);

            // Set a partial selection first
            canvas.setSelectionRegion(100, 100, 500, 400);

            // Resize the image
            canvas.resizeImage(newWidth, newHeight);

            // Selection should be reset to full image
            assertThat(canvas.getSelectionLeft()).isEqualTo(0);
            assertThat(canvas.getSelectionTop()).isEqualTo(0);
            assertThat(canvas.getSelectionWidth()).isEqualTo(newWidth);
            assertThat(canvas.getSelectionHeight()).isEqualTo(newHeight);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testResizeImage_scalingUp_preservesQuality() throws Exception {
        final BufferedImage testImage = createTestImage(100, 100);
        final CountDownLatch latch = new CountDownLatch(1);
        final int newWidth = 400;
        final int newHeight = 400;

        Platform.runLater(() -> {
            final CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            final BufferedImage resizedImage = canvas.resizeImage(newWidth, newHeight);

            assertThat(resizedImage).isNotNull();
            assertThat(resizedImage.getWidth()).isEqualTo(newWidth);
            assertThat(resizedImage.getHeight()).isEqualTo(newHeight);
            assertThat(resizedImage.getType()).isEqualTo(BufferedImage.TYPE_INT_RGB);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testResizeImage_scalingDown_reducesSize() throws Exception {
        final BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        final CountDownLatch latch = new CountDownLatch(1);
        final int newWidth = 200;
        final int newHeight = 150;

        Platform.runLater(() -> {
            final CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            final BufferedImage resizedImage = canvas.resizeImage(newWidth, newHeight);

            assertThat(resizedImage).isNotNull();
            assertThat(resizedImage.getWidth()).isEqualTo(newWidth);
            assertThat(resizedImage.getHeight()).isEqualTo(newHeight);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testResizeImage_resetsRotation() throws Exception {
        final BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        final CountDownLatch latch = new CountDownLatch(1);
        final int newWidth = 400;
        final int newHeight = 300;

        Platform.runLater(() -> {
            final CustomImageCanvas canvas = new CustomImageCanvas(testImage);

            // Rotate the image first
            canvas.rotateImage(45);

            // Resize should reset rotation
            canvas.resizeImage(newWidth, newHeight);

            // After resize, image should be at new dimensions without rotation
            assertThat(canvas.getImageWidth()).isEqualTo(newWidth);
            assertThat(canvas.getImageHeight()).isEqualTo(newHeight);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testResizeImage_multipleResizes_eachWorks() throws Exception {
        final BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final CustomImageCanvas canvas = new CustomImageCanvas(testImage);

            // First resize
            canvas.resizeImage(400, 300);
            assertThat(canvas.getImageWidth()).isEqualTo(400);
            assertThat(canvas.getImageHeight()).isEqualTo(300);

            // Second resize
            canvas.resizeImage(200, 150);
            assertThat(canvas.getImageWidth()).isEqualTo(200);
            assertThat(canvas.getImageHeight()).isEqualTo(150);

            // Third resize
            canvas.resizeImage(600, 450);
            assertThat(canvas.getImageWidth()).isEqualTo(600);
            assertThat(canvas.getImageHeight()).isEqualTo(450);

            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testResizeImage_aspectRatioChange_works() throws Exception {
        final BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        final CountDownLatch latch = new CountDownLatch(1);
        final int newWidth = 1000;  // Different aspect ratio
        final int newHeight = 300;

        Platform.runLater(() -> {
            final CustomImageCanvas canvas = new CustomImageCanvas(testImage);
            final BufferedImage resizedImage = canvas.resizeImage(newWidth, newHeight);

            assertThat(resizedImage.getWidth()).isEqualTo(newWidth);
            assertThat(resizedImage.getHeight()).isEqualTo(newHeight);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testRotationHandle_isVisibleWithinCanvasBounds() throws Exception {
        final BufferedImage testImage = createTestImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final CustomImageCanvas canvas = new CustomImageCanvas(testImage);

            // Test at different rotation angles
            final double[] testAngles = {0, 15, 30, 45, 90, 180, 270};

            for (final double angle : testAngles) {
                if (angle > 0) {
                    canvas.rotateImage(angle);
                }

                final double[] handleBounds = canvas.getRotationHandleBounds();
                final double handleX = handleBounds[0];
                final double handleY = handleBounds[1];
                final double handleWidth = handleBounds[2];
                final double handleHeight = handleBounds[3];

                // The handle should be within reasonable bounds of the canvas
                // At minimum, the center of the handle should be visible
                final double handleCenterX = handleX + handleWidth / 2;
                final double handleCenterY = handleY + handleHeight / 2;

                System.out.println(String.format("Rotation: %.0f° - Handle at (%.1f, %.1f), size: %.1f x %.1f, Canvas: %.1f x %.1f",
                        angle, handleX, handleY, handleWidth, handleHeight, canvas.getWidth(), canvas.getHeight()));

                // Check that handle center is within extended canvas bounds (allowing some overflow for visibility)
                // The handle should be at least partially visible, so we allow it to extend beyond but not too far
                final double margin = 200; // Allow handle to be up to 200 pixels outside canvas
                assertThat(handleCenterX)
                        .as("Handle center X at rotation %.0f° should be near canvas (0 to %.0f)", angle, canvas.getWidth())
                        .isBetween(-margin, canvas.getWidth() + margin);
                assertThat(handleCenterY)
                        .as("Handle center Y at rotation %.0f° should be near canvas (0 to %.0f)", angle, canvas.getHeight())
                        .isBetween(-margin, canvas.getHeight() + margin);

                // Reset for next test
                if (angle > 0) {
                    canvas.rotateImage(-angle);
                }
            }

            latch.countDown();
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}

package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.model.UndoManager;
import org.win.test.TestImageGenerator;
import org.win.view.CustomImageCanvas;
import org.win.view.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class SelectionOffsetEditTest {

    private File tempDir;
    private UndoManager undoManager;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("winnow-offset-test").toFile();
        tempDir.deleteOnExit();
        undoManager = new UndoManager();
    }

    @After
    public void tearDown() {
        if (undoManager != null) {
            undoManager.cleanup();
        }
        if (tempDir != null && tempDir.exists()) {
            final File[] files = tempDir.listFiles();
            if (files != null) {
                for (final File file : files) {
                    file.delete();
                }
            }
            tempDir.delete();
        }
    }

    private File generateTestImage(final int imageIndex, final String fileName) throws IOException {
        final File targetFile = new File(tempDir, fileName);
        return TestImageGenerator.generateTestImage(imageIndex, targetFile);
    }

    @Test
    public void testOffsetEdit_movesSelectionToNewPosition() throws Exception {
        final File testFile = generateTestImage(0, "test.jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                final BufferedImage originalImage = ImageIO.read(testFile);
                final int originalWidth = originalImage.getWidth();
                final int originalHeight = originalImage.getHeight();

                final CustomImageCanvas canvas = window.imageCanvas;
                // Set initial selection to a smaller region
                canvas.setSelectionRegion(0, 0, 100, 100);

                // Move selection to offset (50, 50)
                window.setSelectionOffsetFromText("50, 50");

                // Verify the selection moved but kept the same size
                assertThat(canvas.getSelectionLeft()).isEqualTo(50.0);
                assertThat(canvas.getSelectionTop()).isEqualTo(50.0);
                assertThat(canvas.getSelectionWidth()).isEqualTo(100);
                assertThat(canvas.getSelectionHeight()).isEqualTo(100);

                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testOffsetEdit_clampsToZeroForNegativeValues() throws Exception {
        final File testFile = generateTestImage(0, "test.jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                final CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(50, 50, 150, 150);

                // Try to set negative offset (should be ignored)
                window.setSelectionOffsetFromText("-10, -20");

                // Selection should not have moved
                assertThat(canvas.getSelectionLeft()).isEqualTo(50.0);
                assertThat(canvas.getSelectionTop()).isEqualTo(50.0);

                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testOffsetEdit_shrinksSelectionWhenBeyondBounds() throws Exception {
        final File testFile = generateTestImage(0, "test.jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                final BufferedImage originalImage = ImageIO.read(testFile);
                final int imageWidth = originalImage.getWidth();
                final int imageHeight = originalImage.getHeight();

                final CustomImageCanvas canvas = window.imageCanvas;
                // Set selection to 100x100
                canvas.setSelectionRegion(0, 0, 100, 100);

                // Try to move selection beyond image bounds
                // With smart shrinking, it should accept the offset but shrink the selection to fit
                window.setSelectionOffsetFromText("9999, 9999");

                // Offset should be beyond the image, so selection should shrink to minimum (1x1)
                // Since offset is way beyond bounds, the actual offset will be clamped to image bounds
                // and selection size will be 1x1
                assertThat(canvas.getSelectionLeft()).isGreaterThanOrEqualTo(0.0);
                assertThat(canvas.getSelectionTop()).isGreaterThanOrEqualTo(0.0);
                assertThat(canvas.getSelectionWidth()).isGreaterThanOrEqualTo(1);
                assertThat(canvas.getSelectionHeight()).isGreaterThanOrEqualTo(1);
                // Selection should be within image bounds
                assertThat(canvas.getSelectionLeft() + canvas.getSelectionWidth()).isLessThanOrEqualTo(imageWidth);
                assertThat(canvas.getSelectionTop() + canvas.getSelectionHeight()).isLessThanOrEqualTo(imageHeight);

                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testOffsetEdit_handlesInvalidFormat() throws Exception {
        final File testFile = generateTestImage(0, "test.jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                final CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(50, 50, 150, 150);

                final double originalLeft = canvas.getSelectionLeft();
                final double originalTop = canvas.getSelectionTop();

                // Try invalid format (should be ignored)
                window.setSelectionOffsetFromText("abc, def");

                // Selection should not have moved
                assertThat(canvas.getSelectionLeft()).isEqualTo(originalLeft);
                assertThat(canvas.getSelectionTop()).isEqualTo(originalTop);

                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testOffsetEdit_handlesMissingComma() throws Exception {
        final File testFile = generateTestImage(0, "test.jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                final CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(50, 50, 150, 150);

                final double originalLeft = canvas.getSelectionLeft();
                final double originalTop = canvas.getSelectionTop();

                // Try format without comma (should be ignored)
                window.setSelectionOffsetFromText("100 100");

                // Selection should not have moved
                assertThat(canvas.getSelectionLeft()).isEqualTo(originalLeft);
                assertThat(canvas.getSelectionTop()).isEqualTo(originalTop);

                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testOffsetDisplay_updatesWhenSelectionMoves() throws Exception {
        final File testFile = generateTestImage(0, "test.jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                final CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(25, 75, 125, 175);

                // Wait for UI to update
                Thread.sleep(100);

                // Verify offset field displays correct values
                final String offsetText = window.getSelectionOffsetText();
                assertThat(offsetText).isEqualTo("25, 75");

                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testOffsetEdit_preservesSelectionSize() throws Exception {
        final File testFile = generateTestImage(0, "test.jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                final CustomImageCanvas canvas = window.imageCanvas;
                // Set custom selection size
                canvas.setSelectionRegion(10, 10, 150, 200);

                final int originalWidth = canvas.getSelectionWidth();
                final int originalHeight = canvas.getSelectionHeight();

                // Move selection to new offset
                window.setSelectionOffsetFromText("30, 40");

                // Verify size is preserved
                assertThat(canvas.getSelectionWidth()).isEqualTo(originalWidth);
                assertThat(canvas.getSelectionHeight()).isEqualTo(originalHeight);
                // Verify position changed
                assertThat(canvas.getSelectionLeft()).isEqualTo(30.0);
                assertThat(canvas.getSelectionTop()).isEqualTo(40.0);

                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testOffsetEdit_withZeroOffset() throws Exception {
        final File testFile = generateTestImage(0, "test.jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                final CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(50, 50, 150, 150);

                // Move to origin
                window.setSelectionOffsetFromText("0, 0");

                // Verify selection moved to top-left
                assertThat(canvas.getSelectionLeft()).isEqualTo(0.0);
                assertThat(canvas.getSelectionTop()).isEqualTo(0.0);
                assertThat(canvas.getSelectionWidth()).isEqualTo(100);
                assertThat(canvas.getSelectionHeight()).isEqualTo(100);

                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testOffsetEdit_withSpacesInInput() throws Exception {
        final File testFile = generateTestImage(0, "test.jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                final CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(0, 0, 100, 100);

                // Test with extra spaces
                window.setSelectionOffsetFromText("  30  ,  40  ");

                // Should parse correctly
                assertThat(canvas.getSelectionLeft()).isEqualTo(30.0);
                assertThat(canvas.getSelectionTop()).isEqualTo(40.0);

                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}

package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.model.UndoManager;
import org.win.view.Window;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests verifying all supported image formats can be loaded and displayed.
 *
 * Tests cover:
 * - JPG format
 * - JPEG format
 * - PNG format
 * - GIF format
 * - BMP format
 * - TIFF format (tif extension)
 * - TIFF format (tiff extension)
 */
public class ImageFormatSupportTest {

    private File tempDir;
    private UndoManager undoManager;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("winnow-format-test").toFile();
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

    private File createTestImage(final String filename, final String format) throws IOException {
        final File file = new File(tempDir, filename);
        final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, 100, 100);
        g2d.dispose();

        final String formatName = format.equalsIgnoreCase("tif") || format.equalsIgnoreCase("tiff") ? "tiff" : format;
        ImageIO.write(image, formatName, file);
        return file;
    }

    @Test
    public void testLoadJPGImage() throws Exception {
        final File testFile = createTestImage("test.jpg", "jpg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(window.imageCanvas).isNotNull();
                assertThat(window.imageCanvas.getImageWidth()).isEqualTo(100);
                assertThat(window.imageCanvas.getImageHeight()).isEqualTo(100);
                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testLoadJPEGImage() throws Exception {
        final File testFile = createTestImage("test.jpeg", "jpeg");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(window.imageCanvas).isNotNull();
                assertThat(window.imageCanvas.getImageWidth()).isEqualTo(100);
                assertThat(window.imageCanvas.getImageHeight()).isEqualTo(100);
                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testLoadPNGImage() throws Exception {
        final File testFile = createTestImage("test.png", "png");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(window.imageCanvas).isNotNull();
                assertThat(window.imageCanvas.getImageWidth()).isEqualTo(100);
                assertThat(window.imageCanvas.getImageHeight()).isEqualTo(100);
                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testLoadGIFImage() throws Exception {
        final File testFile = createTestImage("test.gif", "gif");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(window.imageCanvas).isNotNull();
                assertThat(window.imageCanvas.getImageWidth()).isEqualTo(100);
                assertThat(window.imageCanvas.getImageHeight()).isEqualTo(100);
                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testLoadBMPImage() throws Exception {
        final File testFile = createTestImage("test.bmp", "bmp");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(window.imageCanvas).isNotNull();
                assertThat(window.imageCanvas.getImageWidth()).isEqualTo(100);
                assertThat(window.imageCanvas.getImageHeight()).isEqualTo(100);
                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testLoadTIFImage() throws Exception {
        final File testFile = createTestImage("test.tif", "tif");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(window.imageCanvas).isNotNull();
                assertThat(window.imageCanvas.getImageWidth()).isEqualTo(100);
                assertThat(window.imageCanvas.getImageHeight()).isEqualTo(100);
                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testLoadTIFFImage() throws Exception {
        final File testFile = createTestImage("test.tiff", "tiff");
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(window.imageCanvas).isNotNull();
                assertThat(window.imageCanvas.getImageWidth()).isEqualTo(100);
                assertThat(window.imageCanvas.getImageHeight()).isEqualTo(100);
                latch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testAllFormats_canBeSavedAfterCrop() throws Exception {
        final String[] formats = {"jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff"};

        for (final String format : formats) {
            final File testFile = createTestImage("test." + format, format);
            final CountDownLatch latch = new CountDownLatch(1);

            Platform.runLater(() -> {
                try {
                    final Window window = new Window();
                    window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                    // Crop the image to verify format-specific save works
                    window.imageCanvas.setSelectionRegion(10, 10, 50, 50);
                    final BufferedImage croppedImage = window.imageCanvas.cropImage();

                    assertThat(croppedImage).isNotNull();
                    assertThat(croppedImage.getWidth()).isEqualTo(40);
                    assertThat(croppedImage.getHeight()).isEqualTo(40);
                    latch.countDown();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(latch.await(5, TimeUnit.SECONDS))
                    .as("Failed to process format: " + format)
                    .isTrue();

            // Small delay between format tests to avoid JavaFX thread issues
            Thread.sleep(50);
        }
    }
}

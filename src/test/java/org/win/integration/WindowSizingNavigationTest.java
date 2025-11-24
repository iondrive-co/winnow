package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.model.UndoManager;
import org.win.test.TestImageGenerator;
import org.win.view.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify that window sizing works correctly when navigating between images of different sizes.
 */
public final class WindowSizingNavigationTest {

    private static Path tempDir;
    private UndoManager undoManager;
    private Stage imageStage;
    private Window window;

    @BeforeClass
    public static void initJavaFX() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("window-sizing-test");

        // Create test images of different sizes
        TestImageGenerator.generateTestImage(0, tempDir.resolve("image0.jpg").toFile()); // 400x300
        TestImageGenerator.generateTestImage(1, tempDir.resolve("image1.jpg").toFile()); // 600x400
        TestImageGenerator.generateTestImage(2, tempDir.resolve("image2.jpg").toFile()); // 800x600
        TestImageGenerator.generateTestImage(6, tempDir.resolve("image3.jpg").toFile()); // 500x500

        undoManager = new UndoManager();
    }

    @After
    public void tearDown() {
        if (undoManager != null) {
            undoManager.cleanup();
        }

        if (tempDir != null) {
            try {
                Files.walk(tempDir)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (final IOException e) {
                                // Ignore
                            }
                        });
            } catch (final IOException e) {
                // Ignore
            }
        }

        // Close windows on JavaFX thread
        if (window != null) {
            final CountDownLatch closeLatch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    window.closeControlWindow();
                    if (imageStage != null) {
                        imageStage.close();
                    }
                } finally {
                    closeLatch.countDown();
                }
            });

            try {
                closeLatch.await(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    public void testWindowResizesWhenNavigatingBetweenDifferentSizedImages() throws Exception {
        final File[] images = {
            tempDir.resolve("image0.jpg").toFile(),
            tempDir.resolve("image1.jpg").toFile(),
            tempDir.resolve("image2.jpg").toFile(),
            tempDir.resolve("image3.jpg").toFile()
        };

        final CountDownLatch setupLatch = new CountDownLatch(1);
        final AtomicReference<Double> firstImageWidth = new AtomicReference<>();
        final AtomicReference<Double> firstImageHeight = new AtomicReference<>();

        // Display first image
        Platform.runLater(() -> {
            imageStage = new Stage();
            window = new Window();
            final Scene scene = window.displayFile(imageStage, images[0], 1, images.length, null, undoManager, null, null, null);
            imageStage.setScene(scene);
            imageStage.show();
            setupLatch.countDown();
        });

        setupLatch.await(5, TimeUnit.SECONDS);

        // Wait for window to be fully sized (using multiple runLater for timing)
        final CountDownLatch firstSizeLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        firstImageWidth.set(imageStage.getWidth());
                        firstImageHeight.set(imageStage.getHeight());
                        firstSizeLatch.countDown();
                    });
                });
            });
        });

        firstSizeLatch.await(5, TimeUnit.SECONDS);

        // Verify first image dimensions are valid
        assertThat(firstImageWidth.get()).isGreaterThan(0);
        assertThat(firstImageHeight.get()).isGreaterThan(0);

        // Navigate to second image (different size)
        final CountDownLatch secondImageLatch = new CountDownLatch(1);
        final AtomicReference<Double> secondImageWidth = new AtomicReference<>();
        final AtomicReference<Double> secondImageHeight = new AtomicReference<>();

        Platform.runLater(() -> {
            final Scene scene = window.displayFile(imageStage, images[1], 2, images.length, null, undoManager, null, null, null);
            imageStage.setScene(scene);
            secondImageLatch.countDown();
        });

        secondImageLatch.await(5, TimeUnit.SECONDS);

        // Wait for window to resize (using multiple runLater for timing)
        final CountDownLatch secondSizeLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        secondImageWidth.set(imageStage.getWidth());
                        secondImageHeight.set(imageStage.getHeight());
                        secondSizeLatch.countDown();
                    });
                });
            });
        });

        secondSizeLatch.await(5, TimeUnit.SECONDS);

        // Verify second image dimensions are valid and different from first
        assertThat(secondImageWidth.get()).isGreaterThan(0);
        assertThat(secondImageHeight.get()).isGreaterThan(0);

        // Images have different sizes, so window should have resized
        // First image is 400x300, second is 600x400
        assertThat(secondImageWidth.get()).isNotEqualTo(firstImageWidth.get());
    }

}

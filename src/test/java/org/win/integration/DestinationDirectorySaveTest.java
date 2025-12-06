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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to verify that the destination directory save functionality works correctly.
 */
public final class DestinationDirectorySaveTest {

    private Path sourceDir;
    private Path destDir;
    private UndoManager undoManager;
    private Stage imageStage;
    private Window window;

    @BeforeClass
    public static void initJavaFX() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        sourceDir = Files.createTempDirectory("source-dir");
        destDir = Files.createTempDirectory("dest-dir");

        // Create a test image in source directory
        TestImageGenerator.generateTestImage(0, sourceDir.resolve("test-image.jpg").toFile());

        undoManager = new UndoManager();
    }

    @After
    public void tearDown() {
        if (undoManager != null) {
            undoManager.cleanup();
        }

        cleanupDirectory(sourceDir);
        cleanupDirectory(destDir);

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

    private void cleanupDirectory(final Path dir) {
        if (dir != null) {
            try {
                Files.walk(dir)
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
    }

    @Test
    public void testDestinationDirectorySaveDisplaysCorrectly() throws Exception {
        final File testFile = sourceDir.resolve("test-image.jpg").toFile();

        // Step 1: Display the image with control window
        final CountDownLatch setupLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                imageStage = new Stage();
                window = new Window();
                final Scene scene = window.displayFile(imageStage, testFile, 1, 1, null, undoManager, null, null, null);
                imageStage.setScene(scene);
                imageStage.show();
                setupLatch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertThat(setupLatch.await(10, TimeUnit.SECONDS)).isTrue();

        // Step 2: Wait for control window initialization
        final CountDownLatch controlLatch1 = new CountDownLatch(1);
        Platform.runLater(() -> controlLatch1.countDown());
        assertThat(controlLatch1.await(10, TimeUnit.SECONDS)).isTrue();

        final CountDownLatch controlLatch2 = new CountDownLatch(1);
        Platform.runLater(() -> controlLatch2.countDown());
        assertThat(controlLatch2.await(10, TimeUnit.SECONDS)).isTrue();

        final CountDownLatch controlLatch3 = new CountDownLatch(1);
        Platform.runLater(() -> controlLatch3.countDown());
        assertThat(controlLatch3.await(10, TimeUnit.SECONDS)).isTrue();

        // Step 3: Verify components
        final CountDownLatch verifyLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            // Verify destination directory UI components are created
            assertThat(window.getDestinationDirectoryLabel()).isNotNull();
            assertThat(window.getSaveImageButton()).isNotNull();

            // Verify destination directory defaults to source directory
            assertThat(window.getDestinationDirectory()).isEqualTo(testFile.getParentFile());

            verifyLatch.countDown();
        });
        assertThat(verifyLatch.await(10, TimeUnit.SECONDS)).isTrue();

        // Keep window visible for a brief moment to ensure it displays correctly
        Thread.sleep(500);
    }

}

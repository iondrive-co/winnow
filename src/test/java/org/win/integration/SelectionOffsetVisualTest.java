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
import org.win.view.CustomImageCanvas;
import org.win.view.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Visual integration test for selection offset editing.
 * Displays the actual window on screen to verify the offset field is visible
 * and positioned correctly between image size and selection size.
 */
public class SelectionOffsetVisualTest {

    private File tempDir;
    private UndoManager undoManager;
    private Stage imageStage;
    private Window window;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("winnow-offset-visual-test").toFile();
        tempDir.deleteOnExit();
        undoManager = new UndoManager();
    }

    @After
    public void tearDown() {
        final CountDownLatch closeLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            if (window != null) {
                window.closeControlWindow();
            }
            if (imageStage != null && imageStage.isShowing()) {
                imageStage.close();
            }
            closeLatch.countDown();
        });

        try {
            closeLatch.await(2, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

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

    private File createTestImage() throws IOException {
        final File file = new File(tempDir, "test.jpg");
        return TestImageGenerator.generateTestImage(0, file);
    }

    private void waitForFXThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testOffsetField_visibleAndFunctional_inControlWindow() throws Exception {
        final File testFile = createTestImage();
        final CountDownLatch setupLatch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                imageStage = new Stage();
                window = new Window();
                final Scene scene = window.displayFile(imageStage, testFile, 1, 1, null, undoManager, null, null, null, null, null, null);

                imageStage.setScene(scene);
                imageStage.setTitle("Offset Visual Test - Image Window");
                imageStage.show();

                setupLatch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(setupLatch.await(5, TimeUnit.SECONDS)).isTrue();

        // Wait for control window to be positioned
        waitForFXThread();
        waitForFXThread();

        final CountDownLatch testLatch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Stage controlStage = window.getControlStage();
                assertThat(controlStage).isNotNull();
                assertThat(controlStage.isShowing()).isTrue();

                final CustomImageCanvas canvas = window.imageCanvas;

                // Verify initial offset is 0,0 (full image selected)
                String offsetText = window.getSelectionOffsetText();
                assertThat(offsetText).isEqualTo("0, 0");

                // Set a custom selection region
                canvas.setSelectionRegion(50, 75, 200, 225);

                // Wait for display update
                Thread.sleep(200);

                // Verify offset field updated
                offsetText = window.getSelectionOffsetText();
                assertThat(offsetText).isEqualTo("50, 75");

                // Test editing offset to move selection
                window.setSelectionOffsetFromText("100, 120");

                // Wait for update
                Thread.sleep(200);

                // Verify selection moved
                assertThat(canvas.getSelectionLeft()).isEqualTo(100.0);
                assertThat(canvas.getSelectionTop()).isEqualTo(120.0);
                // Size should be preserved
                assertThat(canvas.getSelectionWidth()).isEqualTo(150);
                assertThat(canvas.getSelectionHeight()).isEqualTo(150);

                // Keep window open briefly for visual verification
                Thread.sleep(1000);

                testLatch.countDown();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(testLatch.await(10, TimeUnit.SECONDS)).isTrue();
    }

}

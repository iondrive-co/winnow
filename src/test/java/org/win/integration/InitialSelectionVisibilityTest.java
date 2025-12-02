package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.model.UndoManager;
import org.win.test.TestImageGenerator;
import org.win.view.Window;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies large images auto-fit on first load and the selection is visible immediately.
 */
public class InitialSelectionVisibilityTest {
    private Stage imageStage;
    private Window window;
    private File tempDir;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @After
    public void tearDown() {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            if (window != null) {
                window.closeControlWindow();
            }
            if (imageStage != null) {
                imageStage.close();
            }
            latch.countDown();
        });
        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (tempDir != null) {
            final File[] files = tempDir.listFiles();
            if (files != null) {
                for (final File file : files) {
                    file.delete();
                }
            }
            tempDir.delete();
        }
    }

    @Test
    public void largeImageFitsAndSelectionShows() throws Exception {
        tempDir = Files.createTempDirectory("initial-fit").toFile();
        final File imageFile = new File(tempDir, "huge.jpg");
        TestImageGenerator.generateTestImage(2, imageFile, 2400, 1600);

        final CountDownLatch latch = new CountDownLatch(1);
        final UndoManager undoManager = new UndoManager();

        Platform.runLater(() -> {
            imageStage = new Stage();
            window = new Window();
            final Scene scene = window.displayFile(imageStage, imageFile, 1, 1, null, undoManager, null, null, null);
            imageStage.setScene(scene);
            imageStage.show();
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // Allow layout pass
        final CountDownLatch verifyLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            assertThat(window.imageCanvas.getScaleX()).isLessThan(1.0);
            assertThat(window.imageCanvas.getVisibleSelectionWidth()).isGreaterThan(0);
            assertThat(window.imageCanvas.getVisibleSelectionHeight()).isGreaterThan(0);
            assertThat(window.imageCanvas.getSelectionWidth()).isEqualTo(window.imageCanvas.getImageWidth());
            verifyLatch.countDown();
        });
        assertThat(verifyLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

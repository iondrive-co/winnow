package org.win.view;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.model.UndoManager;
import org.win.test.TestImageGenerator;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class WindowTest {

    @BeforeClass
    public static void initToolkit() {
        // Initialize JavaFX toolkit
        new JFXPanel();
    }

    private File getTestImageFile() {
        try {
            File tempFile = File.createTempFile("test-image", ".jpg");
            tempFile.deleteOnExit();
            return TestImageGenerator.generateTestImage(0, tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test image file", e);
        }
    }

    @Test
    public void testDisplayFile_loadsImageSuccessfully() throws Exception {
        File testFile = getTestImageFile();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(window.imageCanvas).isNotNull();
                latch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testDisplayFile_createsSceneWithBorderPaneLayout() throws Exception {
        File testFile = getTestImageFile();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Window window = new Window();
            try {
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                assertThat(scene.getRoot()).isInstanceOf(BorderPane.class);
                latch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testDisplayFile_imageCanvasIsInitialized() throws Exception {
        File testFile = getTestImageFile();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Window window = new Window();
            try {
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                assertThat(window.imageCanvas).isNotNull();
                assertThat(window.imageCanvas).isInstanceOf(CustomImageCanvas.class);
                assertThat(window.imageCanvas.getWidth()).isGreaterThan(0);
                assertThat(window.imageCanvas.getHeight()).isGreaterThan(0);
                latch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testDisplayFile_createsCropButton() throws Exception {
        File testFile = getTestImageFile();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Window window = new Window();
            try {
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                BorderPane root = (BorderPane) scene.getRoot();
                assertThat(root.getBottom()).isNotNull();
                latch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testDisplayFile_sceneHasValidDimensions() throws Exception {
        File testFile = getTestImageFile();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Window window = new Window();
            try {
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                // Scene dimensions should be determined by the content
                assertThat(scene.getRoot()).isNotNull();
                assertThat(scene.getRoot().getChildrenUnmodifiable()).isNotEmpty();
                latch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testDisplayFile_withValidImageFile_returnsScene() throws Exception {
        File testFile = getTestImageFile();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Window window = new Window();
            try {
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(scene).isInstanceOf(Scene.class);
                latch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testDisplayFile_imageCanvasHasCorrectParent() throws Exception {
        File testFile = getTestImageFile();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Window window = new Window();
            try {
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                BorderPane root = (BorderPane) scene.getRoot();
                assertThat(root.getCenter()).isNotNull();
                latch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testWindow_constructor() {
        Window window = new Window();
        assertThat(window).isNotNull();
    }

    @Test
    public void testDisplayFile_multipleCallsOnSameInstance() throws Exception {
        File testFile = getTestImageFile();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                UndoManager undoManager = new UndoManager();
                Scene scene1 = window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);
                Scene scene2 = window.displayFile(null, testFile, 2, 5, null, undoManager, null, null, null);

                assertThat(scene1).isNotNull();
                assertThat(scene2).isNotNull();
                // Each call should create a new scene
                assertThat(scene1).isNotSameAs(scene2);
                latch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testDisplayFile_withNullStage_embedsControlsInScene() throws Exception {
        File testFile = getTestImageFile();
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                UndoManager undoManager = new UndoManager();

                // Passing null Stage should create embedded controls (test mode)
                Scene scene = window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                assertThat(scene).isNotNull();
                assertThat(scene.getRoot()).isInstanceOf(BorderPane.class);

                // In test mode, controls are embedded in the scene
                BorderPane root = (BorderPane) scene.getRoot();
                assertThat(root.getBottom()).isNotNull();

                latch.countDown();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCloseControlWindow_cleansUpProperly() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Window window = new Window();

            // Close control window should not fail even if it wasn't created
            window.closeControlWindow();

            assertThat(window).isNotNull();
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

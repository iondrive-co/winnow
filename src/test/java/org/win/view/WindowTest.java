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

    @Test
    public void testEditableSelectionDimensions_updatesSelectionWithFixedTopLeft() throws Exception {
        final File testFile = getTestImageFile();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final UndoManager undoManager = new UndoManager();
                window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                // Get initial selection position
                final double initialLeft = window.imageCanvas.getSelectionLeft();
                final double initialTop = window.imageCanvas.getSelectionTop();
                final int initialWidth = window.imageCanvas.getSelectionWidth();
                final int initialHeight = window.imageCanvas.getSelectionHeight();

                // Set a smaller selection first
                window.imageCanvas.setSelectionRegion(50, 60, 250, 310);

                final double left = window.imageCanvas.getSelectionLeft();
                final double top = window.imageCanvas.getSelectionTop();

                // Simulate user editing the dimension field
                window.setSelectionDimensionsFromText("150 x 180");

                // Verify the selection dimensions changed
                assertThat(window.imageCanvas.getSelectionWidth()).isEqualTo(150);
                assertThat(window.imageCanvas.getSelectionHeight()).isEqualTo(180);

                // Verify the top-left corner stayed fixed
                assertThat(window.imageCanvas.getSelectionLeft()).isEqualTo(left);
                assertThat(window.imageCanvas.getSelectionTop()).isEqualTo(top);

                latch.countDown();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testEditableSelectionDimensions_limitsToImageBounds() throws Exception {
        final File testFile = getTestImageFile();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final UndoManager undoManager = new UndoManager();
                window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                final int imageWidth = window.imageCanvas.getImageWidth();
                final int imageHeight = window.imageCanvas.getImageHeight();

                // Set selection near bottom-right corner
                window.imageCanvas.setSelectionRegion(imageWidth - 100, imageHeight - 100, imageWidth, imageHeight);

                // Try to set dimensions that would exceed image bounds
                window.setSelectionDimensionsFromText("500 x 500");

                // Verify dimensions were limited to image bounds
                assertThat(window.imageCanvas.getSelectionWidth()).isLessThanOrEqualTo(100);
                assertThat(window.imageCanvas.getSelectionHeight()).isLessThanOrEqualTo(100);

                latch.countDown();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testEditableSelectionDimensions_handlesInvalidInput() throws Exception {
        final File testFile = getTestImageFile();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final UndoManager undoManager = new UndoManager();
                window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                final int originalWidth = window.imageCanvas.getSelectionWidth();
                final int originalHeight = window.imageCanvas.getSelectionHeight();

                // Try various invalid inputs
                window.setSelectionDimensionsFromText("invalid");

                // Verify dimensions unchanged
                assertThat(window.imageCanvas.getSelectionWidth()).isEqualTo(originalWidth);
                assertThat(window.imageCanvas.getSelectionHeight()).isEqualTo(originalHeight);

                // Try negative dimensions
                window.setSelectionDimensionsFromText("-100 x -200");

                assertThat(window.imageCanvas.getSelectionWidth()).isEqualTo(originalWidth);
                assertThat(window.imageCanvas.getSelectionHeight()).isEqualTo(originalHeight);

                latch.countDown();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testEditableSelectionDimensions_noNegativeValuesWhenZoomed() throws Exception {
        final File testFile = getTestImageFile();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final UndoManager undoManager = new UndoManager();
                window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                // Set selection
                window.imageCanvas.setSelectionRegion(50, 60, 250, 310);

                // Get the displayed dimensions before editing
                final String displayedBefore = window.getSelectionDimensionText();

                // Verify no negative dimensions are displayed
                assertThat(displayedBefore).doesNotContain("-");

                // Start editing by typing a partial value
                window.setSelectionDimensionsFromText("5");

                // Verify the field doesn't show negative values
                final String duringEdit = window.getSelectionDimensionText();
                assertThat(duringEdit).doesNotContain("-");

                // Complete the edit with valid dimensions
                window.setSelectionDimensionsFromText("100 x 120");

                // Verify dimensions updated correctly
                assertThat(window.imageCanvas.getSelectionWidth()).isEqualTo(100);
                assertThat(window.imageCanvas.getSelectionHeight()).isEqualTo(120);

                // Verify displayed dimensions don't contain negative values
                final String displayedAfter = window.getSelectionDimensionText();
                assertThat(displayedAfter).doesNotContain("-");
                assertThat(displayedAfter).isEqualTo("100 x 120");

                latch.countDown();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testDestinationDirectory_defaultsToSourceDirectory() throws Exception {
        final File testFile = getTestImageFile();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final UndoManager undoManager = new UndoManager();
                window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                // Verify destination directory defaults to source directory
                assertThat(window.getDestinationDirectory()).isEqualTo(testFile.getParentFile());
                latch.countDown();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testSaveToDestination_savesImageToDestinationDirectory() throws Exception {
        final File testFile = getTestImageFile();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final UndoManager undoManager = new UndoManager();
                window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                // Create a destination directory
                final File destDir = File.createTempFile("dest-dir", "");
                destDir.delete();
                destDir.mkdir();
                destDir.deleteOnExit();

                // Set destination directory
                window.setDestinationDirectory(destDir);

                // Save image to destination
                window.saveToDestination();

                // Verify file was saved to destination
                final File[] files = destDir.listFiles();
                assertThat(files).isNotNull();
                assertThat(files).hasSizeGreaterThan(0);

                latch.countDown();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testSaveToDestination_whenSameDirectorySameName_doesNothing() throws Exception {
        final File testFile = getTestImageFile();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final UndoManager undoManager = new UndoManager();
                window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                final long lastModified = testFile.lastModified();

                // Wait a bit to ensure modification time would change if file was written
                Thread.sleep(100);

                // Save with same directory and same name
                window.saveToDestination();

                // Verify file was not modified (no save occurred)
                assertThat(testFile.lastModified()).isEqualTo(lastModified);

                latch.countDown();
            } catch (final IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testSaveImageButton_isCreated() throws Exception {
        final File testFile = getTestImageFile();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final UndoManager undoManager = new UndoManager();
                window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                assertThat(window.getSaveImageButton()).isNotNull();
                latch.countDown();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testDestinationDirectoryLabel_isCreated() throws Exception {
        final File testFile = getTestImageFile();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                final Window window = new Window();
                final UndoManager undoManager = new UndoManager();
                window.displayFile(null, testFile, 1, 5, null, undoManager, null, null, null);

                assertThat(window.getDestinationDirectoryLabel()).isNotNull();
                latch.countDown();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

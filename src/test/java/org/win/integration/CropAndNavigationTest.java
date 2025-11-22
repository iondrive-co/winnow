package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class CropAndNavigationTest {

    private File tempDir;
    private List<File> imageFiles;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        // Create a temporary directory for test files
        tempDir = Files.createTempDirectory("winnow-crop-test").toFile();
        tempDir.deleteOnExit();

        imageFiles = new ArrayList<>();

        // Generate test images
        generateTestImage(0, "1-image0.jpg");
        generateTestImage(1, "2-image1.jpg");
    }

    private void generateTestImage(int imageIndex, String fileName) throws IOException {
        File targetFile = new File(tempDir, fileName);
        TestImageGenerator.generateTestImage(imageIndex, targetFile);
        imageFiles.add(targetFile);
    }

    @After
    public void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            File[] files = tempDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            tempDir.delete();
        }
    }

    @Test
    public void testCrop_createsSmaller_croppedImage() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        File testFile = imageFiles.get(0);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 2, null, undoManager, null, null, null);

                // Get original image dimensions
                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                // Create a crop selection (50x50 pixels in the center)
                CustomImageCanvas canvas = window.imageCanvas;
                int cropX = originalWidth / 2 - 25;
                int cropY = originalHeight / 2 - 25;

                // Set crop rectangle using canvas method
                canvas.setSelectionRegion(cropX, cropY, cropX + 50, cropY + 50);

                // Get the crop button and trigger crop
                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button cropButton = (Button) bottomBar.getChildren().get(1);
                cropButton.fire();

                // Wait for crop to complete
                Thread.sleep(200);

                // Verify cropped image is smaller
                BufferedImage croppedImage = ImageIO.read(testFile);
                assertThat(croppedImage.getWidth()).isLessThan(originalWidth);
                assertThat(croppedImage.getHeight()).isLessThan(originalHeight);
                assertThat(croppedImage.getWidth()).isEqualTo(50);
                assertThat(croppedImage.getHeight()).isEqualTo(50);

                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCrop_usesFilenameFromTextField() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        File testFile = imageFiles.get(0);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 2, null, undoManager, null, null, null);

                // Get the text field and crop button
                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button cropButton = (Button) bottomBar.getChildren().get(1);
                // After adding directory label: undo, crop, dimensionDisplay, back, position, forward, fileInfoBox(VBox)
                VBox fileInfoBox = (VBox) bottomBar.getChildren().get(6);
                HBox filenameBox = (HBox) fileInfoBox.getChildren().get(1);
                TextField filenameField = (TextField) filenameBox.getChildren().get(0);

                // Change filename in text field
                String newFilename = "renamed-strawberry";
                filenameField.setText(newFilename);

                // Create a simple crop
                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 60, 60);

                // Trigger crop
                cropButton.fire();

                // Wait for crop to complete
                Thread.sleep(200);

                // Verify new file exists with the name from text field
                File renamedFile = new File(tempDir, "renamed-strawberry.jpg");
                assertThat(renamedFile.exists()).isTrue();
                assertThat(renamedFile.getName()).isEqualTo("renamed-strawberry.jpg");

                // Verify old file was deleted
                assertThat(testFile.exists()).isFalse();

                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testNavigation_afterRename_worksCorrectly() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        File firstFile = imageFiles.get(0);
        File secondFile = imageFiles.get(1);

        AtomicReference<File> updatedFirstFile = new AtomicReference<>(firstFile);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                UndoManager undoManager = new UndoManager();

                // Display first file and rename it
                Scene scene = window.displayFile(null, firstFile, 1, 2, (oldFile, newFile) -> {
                    updatedFirstFile.set(newFile);
                    // Update our local file list (simulating what Main does)
                    int index = imageFiles.indexOf(oldFile);
                    if (index >= 0) {
                        imageFiles.set(index, newFile);
                    }
                }, undoManager, null, null, null);

                // Get UI elements
                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button cropButton = (Button) bottomBar.getChildren().get(1);
                // After adding directory label: undo, crop, dimensionDisplay, back, position, forward, fileInfoBox(VBox)
                VBox fileInfoBox = (VBox) bottomBar.getChildren().get(6);
                HBox filenameBox = (HBox) fileInfoBox.getChildren().get(1);
                TextField filenameField = (TextField) filenameBox.getChildren().get(0);

                // Rename the first file
                filenameField.setText("cropped-first");

                // Create a simple crop
                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 60, 60);

                // Trigger crop
                cropButton.fire();

                // Wait for crop to complete
                Thread.sleep(200);

                // Verify first file was renamed
                File renamedFirstFile = new File(tempDir, "cropped-first.jpg");
                assertThat(renamedFirstFile.exists()).isTrue();
                assertThat(firstFile.exists()).isFalse();

                // Navigate to second file
                Scene scene2 = window.displayFile(null, secondFile, 2, 2, null, undoManager, null, null, null);
                assertThat(scene2).isNotNull();

                // Wait a bit
                Thread.sleep(100);

                // Navigate BACK to first file (using the renamed file)
                Scene scene3 = window.displayFile(null, updatedFirstFile.get(), 1, 2, null, undoManager, null, null, null);
                assertThat(scene3).isNotNull();

                // Verify we can load the renamed file without errors
                assertThat(window.imageCanvas).isNotNull();
                assertThat(window.imageCanvas.getWidth()).isGreaterThan(0);

                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCrop_preservesFileExtension_whenRenaming() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        File testFile = imageFiles.get(0);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                UndoManager undoManager = new UndoManager();
                Scene scene = window.displayFile(null, testFile, 1, 2, null, undoManager, null, null, null);

                // Get UI elements
                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button cropButton = (Button) bottomBar.getChildren().get(1);
                // After adding directory label: undo, crop, dimensionDisplay, back, position, forward, fileInfoBox(VBox)
                VBox fileInfoBox = (VBox) bottomBar.getChildren().get(6);
                HBox filenameBox = (HBox) fileInfoBox.getChildren().get(1);
                TextField filenameField = (TextField) filenameBox.getChildren().get(0);

                // Change filename (without extension)
                filenameField.setText("new-name-without-extension");

                // Create a simple crop
                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 60, 60);

                // Trigger crop
                cropButton.fire();

                // Wait for crop to complete
                Thread.sleep(200);

                // Verify file has .jpg extension
                File renamedFile = new File(tempDir, "new-name-without-extension.jpg");
                assertThat(renamedFile.exists()).isTrue();
                assertThat(renamedFile.getName()).endsWith(".jpg");

                latch.countDown();
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}

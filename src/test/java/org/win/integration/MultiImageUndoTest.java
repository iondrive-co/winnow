package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import org.win.view.PredictiveFilenameEditor;
import org.win.view.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiImageUndoTest {

    private File tempDir;
    private Map<Integer, UndoManager> undoManagers;
    private Map<Integer, File> imageFiles;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("winnow-multi-undo-test").toFile();
        tempDir.deleteOnExit();
        undoManagers = new HashMap<>();
        imageFiles = new HashMap<>();
    }

    @After
    public void tearDown() {
        for (UndoManager undoManager : undoManagers.values()) {
            if (undoManager != null) {
                undoManager.cleanup();
            }
        }
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

    private File generateTestImage(int imageIndex, String fileName) throws IOException {
        File targetFile = new File(tempDir, fileName);
        return TestImageGenerator.generateTestImage(imageIndex, targetFile);
    }

    private UndoManager getOrCreateUndoManager(int position) {
        return undoManagers.computeIfAbsent(position, k -> {
            try {
                return new UndoManager();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testMultipleUndosOnSingleImage() throws Exception {
        File testFile = generateTestImage(0, "image1.jpg");
        imageFiles.put(0, testFile);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                UndoManager undoManager = getOrCreateUndoManager(0);

                // Display file
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                // Get original dimensions
                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button undoButton = (Button) bottomBar.getChildren().get(0);
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                // First crop
                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 100, 100);
                cropButton.fire();
                Thread.sleep(100);

                BufferedImage afterFirstCrop = ImageIO.read(testFile);
                int firstCropWidth = afterFirstCrop.getWidth();
                int firstCropHeight = afterFirstCrop.getHeight();
                assertThat(firstCropWidth).isLessThan(originalWidth);
                assertThat(undoButton.isDisabled()).isFalse();

                // Second crop
                canvas.setSelectionRegion(5, 5, 50, 50);
                cropButton.fire();
                Thread.sleep(100);

                BufferedImage afterSecondCrop = ImageIO.read(testFile);
                assertThat(afterSecondCrop.getWidth()).isLessThan(firstCropWidth);

                // Third crop
                canvas.setSelectionRegion(5, 5, 30, 30);
                cropButton.fire();
                Thread.sleep(100);

                BufferedImage afterThirdCrop = ImageIO.read(testFile);
                int thirdCropWidth = afterThirdCrop.getWidth();

                // Undo third crop
                undoButton.fire();
                Thread.sleep(100);
                BufferedImage afterFirstUndo = ImageIO.read(testFile);
                assertThat(afterFirstUndo.getWidth()).isGreaterThan(thirdCropWidth);

                // Undo second crop
                undoButton.fire();
                Thread.sleep(100);
                BufferedImage afterSecondUndo = ImageIO.read(testFile);
                assertThat(afterSecondUndo.getWidth()).isEqualTo(firstCropWidth);

                // Undo first crop
                undoButton.fire();
                Thread.sleep(100);
                BufferedImage afterThirdUndo = ImageIO.read(testFile);
                assertThat(afterThirdUndo.getWidth()).isEqualTo(originalWidth);
                assertThat(afterThirdUndo.getHeight()).isEqualTo(originalHeight);

                // No more undos available
                assertThat(undoButton.isDisabled()).isTrue();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUndoQueuesScopedToImages() throws Exception {
        File image1 = generateTestImage(0, "image1.jpg");
        File image2 = generateTestImage(1, "image2.jpg");
        imageFiles.put(0, image1);
        imageFiles.put(1, image2);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();

                // Work on image 1
                UndoManager undoManager1 = getOrCreateUndoManager(0);
                Scene scene1 = window.displayFile(null, image1, 1, 2, null, undoManager1, null, null, null);

                BufferedImage originalImage1 = ImageIO.read(image1);
                int originalWidth1 = originalImage1.getWidth();

                BorderPane root1 = (BorderPane) scene1.getRoot();
                HBox bottomBar1 = (HBox) root1.getBottom();
                Button cropButton1 = (Button) bottomBar1.getChildren().get(1);

                CustomImageCanvas canvas1 = window.imageCanvas;
                canvas1.setSelectionRegion(10, 10, 100, 100);
                cropButton1.fire();
                Thread.sleep(100);

                BufferedImage croppedImage1 = ImageIO.read(image1);
                int croppedWidth1 = croppedImage1.getWidth();
                assertThat(croppedWidth1).isLessThan(originalWidth1);

                // Switch to image 2
                UndoManager undoManager2 = getOrCreateUndoManager(1);
                Scene scene2 = window.displayFile(null, image2, 2, 2, null, undoManager2, null, null, null);

                BufferedImage originalImage2 = ImageIO.read(image2);
                int originalWidth2 = originalImage2.getWidth();

                BorderPane root2 = (BorderPane) scene2.getRoot();
                HBox bottomBar2 = (HBox) root2.getBottom();
                Button undoButton2 = (Button) bottomBar2.getChildren().get(0);
                Button cropButton2 = (Button) bottomBar2.getChildren().get(1);

                // Undo button for image 2 should be disabled (no operations yet)
                assertThat(undoButton2.isDisabled()).isTrue();

                CustomImageCanvas canvas2 = window.imageCanvas;
                canvas2.setSelectionRegion(20, 20, 120, 120);
                cropButton2.fire();
                Thread.sleep(100);

                BufferedImage croppedImage2 = ImageIO.read(image2);
                int croppedWidth2 = croppedImage2.getWidth();
                assertThat(croppedWidth2).isLessThan(originalWidth2);
                assertThat(undoButton2.isDisabled()).isFalse();

                // Go back to image 1 and undo
                Scene scene1Again = window.displayFile(null, image1, 1, 2, null, undoManager1, null, null, null);
                BorderPane root1Again = (BorderPane) scene1Again.getRoot();
                HBox bottomBar1Again = (HBox) root1Again.getBottom();
                Button undoButton1 = (Button) bottomBar1Again.getChildren().get(0);

                assertThat(undoButton1.isDisabled()).isFalse();
                undoButton1.fire();
                Thread.sleep(100);

                // Image 1 should be restored to original
                BufferedImage restoredImage1 = ImageIO.read(image1);
                assertThat(restoredImage1.getWidth()).isEqualTo(originalWidth1);

                // Image 2 should still be cropped
                BufferedImage stillCroppedImage2 = ImageIO.read(image2);
                assertThat(stillCroppedImage2.getWidth()).isEqualTo(croppedWidth2);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testNavigationBetweenImagesWithUndos() throws Exception {
        File image1 = generateTestImage(0, "image1.jpg");
        File image2 = generateTestImage(1, "image2.jpg");
        imageFiles.put(0, image1);
        imageFiles.put(1, image2);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                AtomicInteger currentIndex = new AtomicInteger(0);

                // Start with image 1
                Scene scene = window.displayFile(null, image1, 1, 2, null, getOrCreateUndoManager(0), null, null, null);
                BufferedImage orig1 = ImageIO.read(image1);
                int origWidth1 = orig1.getWidth();

                // Crop image 1
                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button cropButton = (Button) bottomBar.getChildren().get(1);
                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 100, 100);
                cropButton.fire();
                Thread.sleep(100);

                int crop1Width = ImageIO.read(image1).getWidth();
                assertThat(crop1Width).isLessThan(origWidth1);

                // Navigate to image 2
                currentIndex.set(1);
                scene = window.displayFile(null, image2, 2, 2, null, getOrCreateUndoManager(1), null, null, null);
                BufferedImage orig2 = ImageIO.read(image2);
                int origWidth2 = orig2.getWidth();

                // Crop image 2
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                cropButton = (Button) bottomBar.getChildren().get(1);
                canvas = window.imageCanvas;
                canvas.setSelectionRegion(15, 15, 110, 110);
                cropButton.fire();
                Thread.sleep(100);

                int crop2Width = ImageIO.read(image2).getWidth();
                assertThat(crop2Width).isLessThan(origWidth2);

                // Crop image 2 again
                canvas.setSelectionRegion(5, 5, 50, 50);
                cropButton.fire();
                Thread.sleep(100);

                int crop2SecondWidth = ImageIO.read(image2).getWidth();
                assertThat(crop2SecondWidth).isLessThan(crop2Width);

                // Navigate back to image 1
                currentIndex.set(0);
                scene = window.displayFile(null, image1, 1, 2, null, getOrCreateUndoManager(0), null, null, null);
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                Button undoButton = (Button) bottomBar.getChildren().get(0);

                // Undo on image 1
                assertThat(undoButton.isDisabled()).isFalse();
                undoButton.fire();
                Thread.sleep(100);

                // Image 1 should be restored
                assertThat(ImageIO.read(image1).getWidth()).isEqualTo(origWidth1);
                // Image 2 should still be double-cropped
                assertThat(ImageIO.read(image2).getWidth()).isEqualTo(crop2SecondWidth);

                // Navigate back to image 2
                currentIndex.set(1);
                scene = window.displayFile(null, image2, 2, 2, null, getOrCreateUndoManager(1), null, null, null);
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                undoButton = (Button) bottomBar.getChildren().get(0);

                // Undo first time on image 2
                assertThat(undoButton.isDisabled()).isFalse();
                undoButton.fire();
                Thread.sleep(100);
                assertThat(ImageIO.read(image2).getWidth()).isEqualTo(crop2Width);

                // Undo second time on image 2
                undoButton.fire();
                Thread.sleep(100);
                assertThat(ImageIO.read(image2).getWidth()).isEqualTo(origWidth2);

                assertThat(undoButton.isDisabled()).isTrue();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(25, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUndoWithRenamesAcrossMultipleImages() throws Exception {
        File image1 = generateTestImage(0, "image1.jpg");
        File image2 = generateTestImage(1, "image2.jpg");
        imageFiles.put(0, image1);
        imageFiles.put(1, image2);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();

                // Crop and rename image 1
                UndoManager undoManager1 = getOrCreateUndoManager(0);
                Scene scene1 = window.displayFile(null, image1, 1, 2, null, undoManager1, null, null, null);

                BorderPane root1 = (BorderPane) scene1.getRoot();
                HBox bottomBar1 = (HBox) root1.getBottom();
                Button cropButton1 = (Button) bottomBar1.getChildren().get(1);
                // After adding directory label: undo, crop, dimensionDisplay, back, position, forward, scrollPane(contains fileInfoBox)
                javafx.scene.control.ScrollPane scrollPane1 = (javafx.scene.control.ScrollPane) bottomBar1.getChildren().get(6);
                VBox fileInfoBox1 = (VBox) scrollPane1.getContent();
                PredictiveFilenameEditor filenameEditor1 = (PredictiveFilenameEditor) fileInfoBox1.getChildren().get(1);
                @SuppressWarnings("unchecked")
                ComboBox<String> firstComboBox1 = (ComboBox<String>) filenameEditor1.getChildren().get(0);

                firstComboBox1.setValue("renamed-image");
                CustomImageCanvas canvas1 = window.imageCanvas;
                canvas1.setSelectionRegion(10, 10, 80, 80);
                cropButton1.fire();
                Thread.sleep(100);

                File renamedImage1 = new File(tempDir, "renamed-image1.jpg");
                assertThat(renamedImage1.exists()).isTrue();
                assertThat(image1.exists()).isFalse();

                // Crop and rename image 2
                UndoManager undoManager2 = getOrCreateUndoManager(1);
                Scene scene2 = window.displayFile(null, image2, 2, 2, null, undoManager2, null, null, null);

                BorderPane root2 = (BorderPane) scene2.getRoot();
                HBox bottomBar2 = (HBox) root2.getBottom();
                Button cropButton2 = (Button) bottomBar2.getChildren().get(1);
                // After adding directory label: undo, crop, dimensionDisplay, back, position, forward, scrollPane(contains fileInfoBox)
                javafx.scene.control.ScrollPane scrollPane2 = (javafx.scene.control.ScrollPane) bottomBar2.getChildren().get(6);
                VBox fileInfoBox2 = (VBox) scrollPane2.getContent();
                PredictiveFilenameEditor filenameEditor2 = (PredictiveFilenameEditor) fileInfoBox2.getChildren().get(1);
                @SuppressWarnings("unchecked")
                ComboBox<String> firstComboBox2 = (ComboBox<String>) filenameEditor2.getChildren().get(0);

                firstComboBox2.setValue("renamed-image");
                CustomImageCanvas canvas2 = window.imageCanvas;
                canvas2.setSelectionRegion(20, 20, 100, 100);
                cropButton2.fire();
                Thread.sleep(100);

                File renamedImage2 = new File(tempDir, "renamed-image2.jpg");
                assertThat(renamedImage2.exists()).isTrue();
                assertThat(image2.exists()).isFalse();

                // Undo image 1
                Scene scene1Again = window.displayFile(null, renamedImage1, 1, 2, null, undoManager1, null, null, null);
                BorderPane root1Again = (BorderPane) scene1Again.getRoot();
                HBox bottomBar1Again = (HBox) root1Again.getBottom();
                Button undoButton1 = (Button) bottomBar1Again.getChildren().get(0);

                undoButton1.fire();
                Thread.sleep(100);

                // Image 1 should be restored with original name
                assertThat(image1.exists()).isTrue();
                assertThat(renamedImage1.exists()).isFalse();

                // Image 2 should still be renamed and cropped
                assertThat(renamedImage2.exists()).isTrue();
                assertThat(image2.exists()).isFalse();

                // Undo image 2
                Scene scene2Again = window.displayFile(null, renamedImage2, 2, 2, null, undoManager2, null, null, null);
                BorderPane root2Again = (BorderPane) scene2Again.getRoot();
                HBox bottomBar2Again = (HBox) root2Again.getBottom();
                Button undoButton2 = (Button) bottomBar2Again.getChildren().get(0);

                undoButton2.fire();
                Thread.sleep(100);

                // Image 2 should be restored with original name
                assertThat(image2.exists()).isTrue();
                assertThat(renamedImage2.exists()).isFalse();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUndoQueueIndependenceWithComplexScenario() throws Exception {
        File image1 = generateTestImage(0, "image1.jpg");
        File image2 = generateTestImage(1, "image2.jpg");
        File image3 = generateTestImage(2, "image3.jpg");
        imageFiles.put(0, image1);
        imageFiles.put(1, image2);
        imageFiles.put(2, image3);
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();

                // Crop image 1 twice
                UndoManager undoManager1 = getOrCreateUndoManager(0);
                Scene scene = window.displayFile(null, image1, 1, 3, null, undoManager1, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;
                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                canvas.setSelectionRegion(10, 10, 90, 90);
                cropButton.fire();
                Thread.sleep(50);
                canvas.setSelectionRegion(5, 5, 50, 50);
                cropButton.fire();
                Thread.sleep(50);

                // Crop image 2 once
                UndoManager undoManager2 = getOrCreateUndoManager(1);
                scene = window.displayFile(null, image2, 2, 3, null, undoManager2, null, null, null);
                canvas = window.imageCanvas;
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                cropButton = (Button) bottomBar.getChildren().get(1);

                canvas.setSelectionRegion(15, 15, 100, 100);
                cropButton.fire();
                Thread.sleep(50);

                // Crop image 3 three times
                UndoManager undoManager3 = getOrCreateUndoManager(2);
                scene = window.displayFile(null, image3, 3, 3, null, undoManager3, null, null, null);
                canvas = window.imageCanvas;
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                cropButton = (Button) bottomBar.getChildren().get(1);

                canvas.setSelectionRegion(10, 10, 100, 100);
                cropButton.fire();
                Thread.sleep(50);
                canvas.setSelectionRegion(5, 5, 60, 60);
                cropButton.fire();
                Thread.sleep(50);
                canvas.setSelectionRegion(5, 5, 40, 40);
                cropButton.fire();
                Thread.sleep(50);

                // Verify undo counts
                assertThat(undoManager1.canUndo()).isTrue();
                assertThat(undoManager2.canUndo()).isTrue();
                assertThat(undoManager3.canUndo()).isTrue();

                // Undo once on each image
                scene = window.displayFile(null, image1, 1, 3, null, undoManager1, null, null, null);
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                Button undoButton1 = (Button) bottomBar.getChildren().get(0);
                undoButton1.fire();
                Thread.sleep(50);

                scene = window.displayFile(null, image2, 2, 3, null, undoManager2, null, null, null);
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                Button undoButton2 = (Button) bottomBar.getChildren().get(0);
                undoButton2.fire();
                Thread.sleep(50);

                scene = window.displayFile(null, image3, 3, 3, null, undoManager3, null, null, null);
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                Button undoButton3 = (Button) bottomBar.getChildren().get(0);
                undoButton3.fire();
                Thread.sleep(50);

                // Image 1 should have 1 undo left, image 2 none, image 3 two
                assertThat(undoManager1.canUndo()).isTrue();
                assertThat(undoManager2.canUndo()).isFalse();
                assertThat(undoManager3.canUndo()).isTrue();

                // Undo remaining on image 1
                scene = window.displayFile(null, image1, 1, 3, null, undoManager1, null, null, null);
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                undoButton1 = (Button) bottomBar.getChildren().get(0);
                undoButton1.fire();
                Thread.sleep(50);
                assertThat(undoManager1.canUndo()).isFalse();

                // Undo remaining on image 3
                scene = window.displayFile(null, image3, 3, 3, null, undoManager3, null, null, null);
                root = (BorderPane) scene.getRoot();
                bottomBar = (HBox) root.getBottom();
                undoButton3 = (Button) bottomBar.getChildren().get(0);
                undoButton3.fire();
                Thread.sleep(50);
                assertThat(undoManager3.canUndo()).isTrue();
                undoButton3.fire();
                Thread.sleep(50);
                assertThat(undoManager3.canUndo()).isFalse();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
    }
}

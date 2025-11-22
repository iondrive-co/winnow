package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class UndoIntegrationTest {

    private File tempDir;
    private UndoManager undoManager;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("winnow-undo-test").toFile();
        tempDir.deleteOnExit();
        undoManager = new UndoManager();
    }

    @After
    public void tearDown() {
        if (undoManager != null) {
            undoManager.cleanup();
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

    @Test
    public void testUndoButton_initiallyDisabled() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button undoButton = (Button) bottomBar.getChildren().get(0);

                assertThat(undoButton.getText()).isEqualTo("Undo");
                assertThat(undoButton.isDisabled()).isTrue();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUndoButton_enabledAfterCrop() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button undoButton = (Button) bottomBar.getChildren().get(0);
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                assertThat(undoButton.isDisabled()).isTrue();

                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 60, 60);

                cropButton.fire();
                Thread.sleep(200);

                assertThat(undoButton.isDisabled()).isFalse();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUndo_restoresOriginalImageSize() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                AtomicBoolean undoCalled = new AtomicBoolean(false);

                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, (restoredFile) -> {
                    undoCalled.set(true);
                }, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button undoButton = (Button) bottomBar.getChildren().get(0);
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 60, 60);

                cropButton.fire();
                Thread.sleep(200);

                BufferedImage croppedImage = ImageIO.read(testFile);
                assertThat(croppedImage.getWidth()).isLessThan(originalWidth);
                assertThat(croppedImage.getHeight()).isLessThan(originalHeight);

                undoButton.fire();
                Thread.sleep(200);

                BufferedImage restoredImage = ImageIO.read(testFile);
                assertThat(restoredImage.getWidth()).isEqualTo(originalWidth);
                assertThat(restoredImage.getHeight()).isEqualTo(originalHeight);
                assertThat(undoCalled.get()).isTrue();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testMultipleCropsAndUndos() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button undoButton = (Button) bottomBar.getChildren().get(0);
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 100, 100);
                cropButton.fire();
                Thread.sleep(200);

                BufferedImage firstCrop = ImageIO.read(testFile);
                int firstCropWidth = firstCrop.getWidth();
                int firstCropHeight = firstCrop.getHeight();
                assertThat(firstCropWidth).isLessThan(originalWidth);
                assertThat(firstCropHeight).isLessThan(originalHeight);

                canvas.setSelectionRegion(5, 5, 50, 50);
                cropButton.fire();
                Thread.sleep(200);

                BufferedImage secondCrop = ImageIO.read(testFile);
                assertThat(secondCrop.getWidth()).isLessThan(firstCropWidth);
                assertThat(secondCrop.getHeight()).isLessThan(firstCropHeight);

                undoButton.fire();
                Thread.sleep(200);

                BufferedImage afterFirstUndo = ImageIO.read(testFile);
                assertThat(afterFirstUndo.getWidth()).isEqualTo(firstCropWidth);
                assertThat(afterFirstUndo.getHeight()).isEqualTo(firstCropHeight);

                undoButton.fire();
                Thread.sleep(200);

                BufferedImage afterSecondUndo = ImageIO.read(testFile);
                assertThat(afterSecondUndo.getWidth()).isEqualTo(originalWidth);
                assertThat(afterSecondUndo.getHeight()).isEqualTo(originalHeight);

                assertThat(undoButton.isDisabled()).isTrue();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUndo_disabledAfterLastUndo() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button undoButton = (Button) bottomBar.getChildren().get(0);
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 60, 60);

                cropButton.fire();
                Thread.sleep(200);

                assertThat(undoButton.isDisabled()).isFalse();

                undoButton.fire();
                Thread.sleep(200);

                assertThat(undoButton.isDisabled()).isTrue();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUndo_afterRenameAndCrop_deletesRenamedFile() throws Exception {
        File testFile = generateTestImage(0, "original.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button undoButton = (Button) bottomBar.getChildren().get(0);
                Button cropButton = (Button) bottomBar.getChildren().get(1);
                // After adding navigation buttons, dimension display, and directory label: undo, crop, dimensionDisplay, back, position, forward, fileInfoBox(VBox)
                // fileInfoBox contains: dirLabel, filenameBox(HBox) which contains filenameField and extensionLabel
                VBox fileInfoBox = (VBox) bottomBar.getChildren().get(6);
                HBox filenameBox = (HBox) fileInfoBox.getChildren().get(1);
                javafx.scene.control.TextField filenameField = (javafx.scene.control.TextField) filenameBox.getChildren().get(0);

                filenameField.setText("renamed");

                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 60, 60);

                cropButton.fire();
                Thread.sleep(200);

                File renamedFile = new File(tempDir, "renamed.jpg");
                assertThat(renamedFile.exists()).isTrue();
                assertThat(testFile.exists()).isFalse();

                undoButton.fire();
                Thread.sleep(200);

                assertThat(testFile.exists()).isTrue();
                assertThat(renamedFile.exists()).isFalse();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUndo_afterRename_canStillAccessFileList() throws Exception {
        File testFile1 = generateTestImage(0, "file1.jpg");
        File testFile2 = generateTestImage(1, "file2.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                AtomicBoolean undoCallbackInvoked = new AtomicBoolean(false);

                Scene scene = window.displayFile(null, testFile1, 1, 2, null, undoManager, (restoredFile) -> {
                    undoCallbackInvoked.set(true);
                }, null, null);

                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button undoButton = (Button) bottomBar.getChildren().get(0);
                Button cropButton = (Button) bottomBar.getChildren().get(1);
                // After adding navigation buttons, dimension display, and directory label: undo, crop, dimensionDisplay, back, position, forward, fileInfoBox(VBox)
                // fileInfoBox contains: dirLabel, filenameBox(HBox) which contains filenameField and extensionLabel
                VBox fileInfoBox = (VBox) bottomBar.getChildren().get(6);
                HBox filenameBox = (HBox) fileInfoBox.getChildren().get(1);
                javafx.scene.control.TextField filenameField = (javafx.scene.control.TextField) filenameBox.getChildren().get(0);

                filenameField.setText("renamed-file1");

                CustomImageCanvas canvas = window.imageCanvas;
                canvas.setSelectionRegion(10, 10, 60, 60);

                cropButton.fire();
                Thread.sleep(200);

                File renamedFile = new File(tempDir, "renamed-file1.jpg");
                assertThat(renamedFile.exists()).isTrue();
                assertThat(testFile1.exists()).isFalse();

                undoButton.fire();
                Thread.sleep(200);

                assertThat(testFile1.exists()).isTrue();
                assertThat(renamedFile.exists()).isFalse();
                assertThat(undoCallbackInvoked.get()).isTrue();

                assertThat(testFile2.exists()).as("Second file should still exist").isTrue();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}

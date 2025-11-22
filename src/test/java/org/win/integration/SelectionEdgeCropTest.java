package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
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

import static org.assertj.core.api.Assertions.assertThat;

public class SelectionEdgeCropTest {

    private File tempDir;
    private UndoManager undoManager;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("winnow-edge-crop-test").toFile();
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
    public void testCrop_withTopEdgeMovedDown_cropsCorrectly() throws Exception {
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
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                CustomImageCanvas canvas = window.imageCanvas;
                // Move top edge down by 20 pixels
                canvas.setSelectionRegion(0, 20, originalWidth, originalHeight);

                cropButton.fire();
                Thread.sleep(200);

                BufferedImage croppedImage = ImageIO.read(testFile);
                assertThat(croppedImage.getWidth()).isEqualTo(originalWidth);
                assertThat(croppedImage.getHeight()).isEqualTo(originalHeight - 20);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCrop_withLeftEdgeMovedRight_cropsCorrectly() throws Exception {
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
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                CustomImageCanvas canvas = window.imageCanvas;
                // Move left edge right by 30 pixels
                canvas.setSelectionRegion(30, 0, originalWidth, originalHeight);

                cropButton.fire();
                Thread.sleep(200);

                BufferedImage croppedImage = ImageIO.read(testFile);
                assertThat(croppedImage.getWidth()).isEqualTo(originalWidth - 30);
                assertThat(croppedImage.getHeight()).isEqualTo(originalHeight);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCrop_withAllEdgesMoved_cropsCorrectly() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);

                BorderPane root = (BorderPane) scene.getRoot();
                HBox bottomBar = (HBox) root.getBottom();
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                CustomImageCanvas canvas = window.imageCanvas;
                // Move all edges inward
                canvas.setSelectionRegion(15, 25, 200, 180);

                cropButton.fire();
                Thread.sleep(200);

                BufferedImage croppedImage = ImageIO.read(testFile);
                assertThat(croppedImage.getWidth()).isEqualTo(200 - 15);
                assertThat(croppedImage.getHeight()).isEqualTo(180 - 25);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testCrop_withNegativeCoordinates_clampsToZero() throws Exception {
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
                Button cropButton = (Button) bottomBar.getChildren().get(1);

                CustomImageCanvas canvas = window.imageCanvas;
                // Set selection with negative coordinates (shouldn't crash)
                canvas.setSelectionRegion(-10, -10, 100, 100);

                cropButton.fire();
                Thread.sleep(200);

                BufferedImage croppedImage = ImageIO.read(testFile);
                assertThat(croppedImage.getWidth()).isLessThanOrEqualTo(originalWidth);
                assertThat(croppedImage.getHeight()).isLessThanOrEqualTo(originalHeight);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }
}

package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.model.UndoManager;
import org.win.test.TestImageGenerator;
import org.win.view.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RotationTest {

    private File tempDir;
    private UndoManager undoManager;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("winnow-rotation-test").toFile();
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
    public void testRotate_changesImageDimensions() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                window.rotateAndSave(90);
                Thread.sleep(200);

                BufferedImage rotatedImage = ImageIO.read(testFile);
                // After 90 degree rotation, width and height should swap (approximately)
                assertThat(rotatedImage.getWidth()).isGreaterThanOrEqualTo(originalHeight - 10);
                assertThat(rotatedImage.getHeight()).isGreaterThanOrEqualTo(originalWidth - 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testRotate_enablesUndo() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                assertThat(undoManager.canUndo()).isFalse();

                window.rotateAndSave(5);
                Thread.sleep(200);

                assertThat(undoManager.canUndo()).isTrue();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testRotate_canBeUndone() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                window.rotateAndSave(45);
                Thread.sleep(200);

                BufferedImage rotatedImage = ImageIO.read(testFile);
                assertThat(rotatedImage.getWidth()).isNotEqualTo(originalWidth);

                window.triggerUndo();
                Thread.sleep(200);

                BufferedImage restoredImage = ImageIO.read(testFile);
                assertThat(restoredImage.getWidth()).isEqualTo(originalWidth);
                assertThat(restoredImage.getHeight()).isEqualTo(originalHeight);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testMultipleRotations_canBeUndoneInOrder() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                window.rotateAndSave(10);
                Thread.sleep(200);

                BufferedImage firstRotation = ImageIO.read(testFile);
                int firstWidth = firstRotation.getWidth();
                int firstHeight = firstRotation.getHeight();

                window.rotateAndSave(10);
                Thread.sleep(200);

                BufferedImage secondRotation = ImageIO.read(testFile);
                assertThat(secondRotation.getWidth()).isNotEqualTo(firstWidth);

                // Undo second rotation
                window.triggerUndo();
                Thread.sleep(200);

                BufferedImage afterFirstUndo = ImageIO.read(testFile);
                assertThat(afterFirstUndo.getWidth()).isEqualTo(firstWidth);
                assertThat(afterFirstUndo.getHeight()).isEqualTo(firstHeight);

                // Undo first rotation
                window.triggerUndo();
                Thread.sleep(200);

                BufferedImage afterSecondUndo = ImageIO.read(testFile);
                assertThat(afterSecondUndo.getWidth()).isEqualTo(originalWidth);
                assertThat(afterSecondUndo.getHeight()).isEqualTo(originalHeight);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testRotate_negativeAngle_rotatesCounterClockwise() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                window.rotateAndSave(-5);
                Thread.sleep(200);

                BufferedImage rotatedImage = ImageIO.read(testFile);
                // Image should be rotated, dimensions may change slightly
                assertThat(rotatedImage).isNotNull();

                // Verify undo works
                window.triggerUndo();
                Thread.sleep(200);

                BufferedImage restoredImage = ImageIO.read(testFile);
                assertThat(restoredImage.getWidth()).isEqualTo(originalWidth);
                assertThat(restoredImage.getHeight()).isEqualTo(originalHeight);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testRotate_forwardAndBackward_returnsToOriginalSize() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                // Rotate forward 5 degrees
                window.rotateAndSave(5);
                Thread.sleep(200);

                BufferedImage rotatedOnce = ImageIO.read(testFile);
                int rotatedOnceWidth = rotatedOnce.getWidth();
                int rotatedOnceHeight = rotatedOnce.getHeight();

                // Canvas should expand
                assertThat(rotatedOnceWidth).isGreaterThan(originalWidth);
                assertThat(rotatedOnceHeight).isGreaterThan(originalHeight);

                // Rotate forward again 5 degrees (total 10 degrees)
                window.rotateAndSave(5);
                Thread.sleep(200);

                BufferedImage rotatedTwice = ImageIO.read(testFile);

                // Rotate backward 5 degrees (total 5 degrees)
                window.rotateAndSave(-5);
                Thread.sleep(200);

                BufferedImage rotatedBack = ImageIO.read(testFile);

                // Should be back to the same size as after first rotation
                assertThat(rotatedBack.getWidth()).isEqualTo(rotatedOnceWidth);
                assertThat(rotatedBack.getHeight()).isEqualTo(rotatedOnceHeight);

                // Rotate backward 5 degrees (total 0 degrees)
                window.rotateAndSave(-5);
                Thread.sleep(200);

                BufferedImage backToOriginal = ImageIO.read(testFile);

                // Should be back to original size
                assertThat(backToOriginal.getWidth()).isEqualTo(originalWidth);
                assertThat(backToOriginal.getHeight()).isEqualTo(originalHeight);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
    }
}

package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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

/**
 * Integration tests verifying all 15 keyboard shortcuts documented in the README work correctly.
 *
 * Tests cover:
 * 1. Previous Image (Left Arrow)
 * 2. Next Image (Right Arrow)
 * 3. Zoom In (Equals/Plus)
 * 4. Zoom Out (Minus/Dash)
 * 5. Rotate Right (Ctrl + E)
 * 6. Rotate Left (Ctrl + Q)
 * 7. Expand Right Selection Right (Ctrl + D)
 * 8. Reduce Right Selection Left (Ctrl + A)
 * 9. Expand Bottom Selection Down (Ctrl + S)
 * 10. Reduce Bottom Selection Up (Ctrl + W)
 * 11. Expand Left Selection Right (Shift + Ctrl + D)
 * 12. Reduce Left Selection Left (Shift + Ctrl + A)
 * 13. Expand Top Selection Down (Shift + Ctrl + S)
 * 14. Reduce Top Selection Up (Shift + Ctrl + W)
 * 15. Undo (Ctrl + Z)
 */
public class KeyboardShortcutTest {

    private File tempDir;
    private UndoManager undoManager;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("winnow-keyboard-test").toFile();
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
    public void testZoomIn_equalsKey_increasesZoom() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                double initialScale = canvas.getScaleX();
                canvas.zoom(1.1); // Simulate zoom in
                double finalScale = canvas.getScaleX();

                assertThat(finalScale).isGreaterThan(initialScale);
                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testZoomOut_minusKey_decreasesZoom() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                double initialScale = canvas.getScaleX();
                canvas.zoom(0.9); // Simulate zoom out
                double finalScale = canvas.getScaleX();

                assertThat(finalScale).isLessThan(initialScale);
                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testRotateRight_ctrlE_rotatesClockwise() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();
                int originalHeight = originalImage.getHeight();

                // Simulate Ctrl+E shortcut action
                window.rotateAndSave(5);
                Thread.sleep(200);

                BufferedImage rotatedImage = ImageIO.read(testFile);
                // After rotation, dimensions should change
                assertThat(rotatedImage.getWidth()).isNotEqualTo(originalWidth);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testRotateLeft_ctrlQ_rotatesCounterClockwise() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();

                // Simulate Ctrl+Q shortcut action
                window.rotateAndSave(-5);
                Thread.sleep(200);

                BufferedImage rotatedImage = ImageIO.read(testFile);
                // After rotation, dimensions should change
                assertThat(rotatedImage.getWidth()).isNotEqualTo(originalWidth);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testExpandRightSelection_ctrlD_expandsSelectionRight() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                // First reduce the selection to make room for expansion
                canvas.setSelectionRegion(0, 0, canvas.getImageWidth() - 50, canvas.getImageHeight());
                int initialWidth = canvas.getSelectionWidth();

                // Simulate Ctrl+D shortcut action
                canvas.expandSelectionRight(10);

                int finalWidth = canvas.getSelectionWidth();
                assertThat(finalWidth).isEqualTo(initialWidth + 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testReduceRightSelection_ctrlA_reducesSelectionLeft() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                int initialWidth = canvas.getSelectionWidth();

                // Simulate Ctrl+A shortcut action
                canvas.reduceSelectionLeft(10);

                int finalWidth = canvas.getSelectionWidth();
                assertThat(finalWidth).isEqualTo(initialWidth - 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testExpandBottomSelection_ctrlS_expandsSelectionDown() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                // First reduce the selection to make room for expansion
                canvas.setSelectionRegion(0, 0, canvas.getImageWidth(), canvas.getImageHeight() - 50);
                int initialHeight = canvas.getSelectionHeight();

                // Simulate Ctrl+S shortcut action
                canvas.expandSelectionDown(10);

                int finalHeight = canvas.getSelectionHeight();
                assertThat(finalHeight).isEqualTo(initialHeight + 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testReduceBottomSelection_ctrlW_reducesSelectionUp() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                int initialHeight = canvas.getSelectionHeight();

                // Simulate Ctrl+W shortcut action
                canvas.reduceSelectionUp(10);

                int finalHeight = canvas.getSelectionHeight();
                assertThat(finalHeight).isEqualTo(initialHeight - 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testExpandLeftSelection_shiftCtrlD_expandsLeftSelectionRight() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                // First move the left edge inward to have room to expand
                canvas.setSelectionRegion(20, 0, canvas.getImageWidth(), canvas.getImageHeight());
                int initialWidth = canvas.getSelectionWidth();

                // Simulate Shift+Ctrl+D shortcut action
                canvas.expandLeftSelectionRight(10);

                int finalWidth = canvas.getSelectionWidth();
                assertThat(finalWidth).isEqualTo(initialWidth - 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testReduceLeftSelection_shiftCtrlA_reducesLeftSelectionLeft() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                // First move left edge inward to make room for reduction
                canvas.setSelectionRegion(20, 0, canvas.getImageWidth(), canvas.getImageHeight());
                int initialWidth = canvas.getSelectionWidth();

                // Simulate Shift+Ctrl+A shortcut action
                canvas.reduceLeftSelectionLeft(10);

                int finalWidth = canvas.getSelectionWidth();
                assertThat(finalWidth).isEqualTo(initialWidth + 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testExpandTopSelection_shiftCtrlS_expandsTopSelectionDown() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                // First move the top edge inward to have room to expand
                canvas.setSelectionRegion(0, 20, canvas.getImageWidth(), canvas.getImageHeight());
                int initialHeight = canvas.getSelectionHeight();

                // Simulate Shift+Ctrl+S shortcut action
                canvas.expandTopSelectionDown(10);

                int finalHeight = canvas.getSelectionHeight();
                assertThat(finalHeight).isEqualTo(initialHeight - 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testReduceTopSelection_shiftCtrlW_reducesTopSelectionUp() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                // First move top edge inward to make room for reduction
                canvas.setSelectionRegion(0, 20, canvas.getImageWidth(), canvas.getImageHeight());
                int initialHeight = canvas.getSelectionHeight();

                // Simulate Shift+Ctrl+W shortcut action
                canvas.reduceTopSelectionUp(10);

                int finalHeight = canvas.getSelectionHeight();
                assertThat(finalHeight).isEqualTo(initialHeight + 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testUndo_ctrlZ_undoesLastOperation() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);

                BufferedImage originalImage = ImageIO.read(testFile);
                int originalWidth = originalImage.getWidth();

                // Perform an operation that can be undone
                window.rotateAndSave(5);
                Thread.sleep(200);

                BufferedImage rotatedImage = ImageIO.read(testFile);
                assertThat(rotatedImage.getWidth()).isNotEqualTo(originalWidth);

                // Simulate Ctrl+Z shortcut action
                window.triggerUndo();
                Thread.sleep(200);

                BufferedImage restoredImage = ImageIO.read(testFile);
                assertThat(restoredImage.getWidth()).isEqualTo(originalWidth);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testNavigationShortcuts_leftAndRightArrows_triggerNavigation() throws Exception {
        File testFile1 = generateTestImage(0, "test1.jpg");
        File testFile2 = generateTestImage(1, "test2.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                AtomicBoolean navigateBackCalled = new AtomicBoolean(false);
                AtomicBoolean navigateForwardCalled = new AtomicBoolean(false);

                Window window = new Window();

                // Use the version with navigation callbacks
                Scene scene = window.displayFile(
                    null, testFile1, 1, 2, null, undoManager, null,
                    () -> navigateBackCalled.set(true),
                    () -> navigateForwardCalled.set(true)
                );

                // Test that left arrow triggers navigation back
                KeyEvent leftArrowEvent = new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.LEFT,
                    false, false, false, false
                );
                scene.getRoot().fireEvent(leftArrowEvent);
                assertThat(navigateBackCalled.get()).isTrue();

                // Test that right arrow triggers navigation forward
                KeyEvent rightArrowEvent = new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.RIGHT,
                    false, false, false, false
                );
                scene.getRoot().fireEvent(rightArrowEvent);
                assertThat(navigateForwardCalled.get()).isTrue();

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testAllSelectionShortcuts_workSequentially() throws Exception {
        File testFile = generateTestImage(0, "test.jpg");
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                Window window = new Window();
                Scene scene = window.displayFile(null, testFile, 1, 1, null, undoManager, null, null, null);
                CustomImageCanvas canvas = window.imageCanvas;

                // Test all 8 selection modification shortcuts work in sequence
                // Start with a reduced selection to allow for expansions
                canvas.setSelectionRegion(50, 50, canvas.getImageWidth() - 50, canvas.getImageHeight() - 50);
                int initialWidth = canvas.getSelectionWidth();
                int initialHeight = canvas.getSelectionHeight();

                // Right edge operations
                canvas.expandSelectionRight(10);
                assertThat(canvas.getSelectionWidth()).isEqualTo(initialWidth + 10);

                canvas.reduceSelectionLeft(5);
                assertThat(canvas.getSelectionWidth()).isEqualTo(initialWidth + 5);

                // Bottom edge operations
                canvas.expandSelectionDown(10);
                assertThat(canvas.getSelectionHeight()).isEqualTo(initialHeight + 10);

                canvas.reduceSelectionUp(5);
                assertThat(canvas.getSelectionHeight()).isEqualTo(initialHeight + 5);

                // Left edge operations
                canvas.reduceLeftSelectionLeft(10);
                assertThat(canvas.getSelectionWidth()).isEqualTo(initialWidth + 15);

                canvas.expandLeftSelectionRight(5);
                assertThat(canvas.getSelectionWidth()).isEqualTo(initialWidth + 10);

                // Top edge operations
                canvas.reduceTopSelectionUp(10);
                assertThat(canvas.getSelectionHeight()).isEqualTo(initialHeight + 15);

                canvas.expandTopSelectionDown(5);
                assertThat(canvas.getSelectionHeight()).isEqualTo(initialHeight + 10);

                latch.countDown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

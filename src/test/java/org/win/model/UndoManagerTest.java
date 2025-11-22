package org.win.model;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UndoManagerTest {

    private UndoManager undoManager;
    private File tempDir;
    private File testFile;
    private BufferedImage testImage;

    @Before
    public void setUp() throws IOException {
        undoManager = new UndoManager();
        tempDir = Files.createTempDirectory("winnow-test").toFile();
        tempDir.deleteOnExit();

        testFile = new File(tempDir, "test-image.jpg");
        testImage = createTestImage(100, 100, Color.RED);
        ImageIO.write(testImage, "jpg", testFile);
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

    private BufferedImage createTestImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    @Test
    public void testCanUndo_initiallyReturnsFalse() {
        assertThat(undoManager.canUndo()).isFalse();
    }

    @Test
    public void testSaveStateBeforeOperation_allowsUndo() throws IOException {
        undoManager.saveStateBeforeOperation(testFile);
        assertThat(undoManager.canUndo()).isTrue();
    }

    @Test
    public void testUndo_restoresOriginalFile() throws IOException {
        BufferedImage originalImage = ImageIO.read(testFile);
        assertThat(originalImage.getWidth()).isEqualTo(100);
        assertThat(originalImage.getHeight()).isEqualTo(100);

        undoManager.saveStateBeforeOperation(testFile);

        BufferedImage modifiedImage = createTestImage(50, 50, Color.BLUE);
        ImageIO.write(modifiedImage, "jpg", testFile);

        BufferedImage modifiedRead = ImageIO.read(testFile);
        assertThat(modifiedRead.getWidth()).isEqualTo(50);
        assertThat(modifiedRead.getHeight()).isEqualTo(50);

        undoManager.undo();

        BufferedImage restoredImage = ImageIO.read(testFile);
        assertThat(restoredImage.getWidth()).isEqualTo(100);
        assertThat(restoredImage.getHeight()).isEqualTo(100);
    }

    @Test
    public void testUndo_whenNoOperations_throwsException() {
        assertThatThrownBy(() -> undoManager.undo())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("No operations to undo");
    }

    @Test
    public void testCanUndo_afterUndo_returnsFalse() throws IOException {
        undoManager.saveStateBeforeOperation(testFile);
        assertThat(undoManager.canUndo()).isTrue();

        undoManager.undo();
        assertThat(undoManager.canUndo()).isFalse();
    }

    @Test
    public void testMultipleOperations_undoInCorrectOrder() throws IOException {
        BufferedImage image1 = createTestImage(100, 100, Color.RED);
        ImageIO.write(image1, "jpg", testFile);
        undoManager.saveStateBeforeOperation(testFile);

        BufferedImage image2 = createTestImage(80, 80, Color.GREEN);
        ImageIO.write(image2, "jpg", testFile);
        undoManager.saveStateBeforeOperation(testFile);

        BufferedImage image3 = createTestImage(60, 60, Color.BLUE);
        ImageIO.write(image3, "jpg", testFile);

        assertThat(undoManager.canUndo()).isTrue();
        undoManager.undo();

        BufferedImage afterFirstUndo = ImageIO.read(testFile);
        assertThat(afterFirstUndo.getWidth()).isEqualTo(80);
        assertThat(afterFirstUndo.getHeight()).isEqualTo(80);

        assertThat(undoManager.canUndo()).isTrue();
        undoManager.undo();

        BufferedImage afterSecondUndo = ImageIO.read(testFile);
        assertThat(afterSecondUndo.getWidth()).isEqualTo(100);
        assertThat(afterSecondUndo.getHeight()).isEqualTo(100);

        assertThat(undoManager.canUndo()).isFalse();
    }

    @Test
    public void testCleanup_removesAllBackupFiles() throws IOException {
        undoManager.saveStateBeforeOperation(testFile);
        assertThat(undoManager.canUndo()).isTrue();

        undoManager.cleanup();
        assertThat(undoManager.canUndo()).isFalse();
    }

    @Test
    public void testSaveStateBeforeOperation_withDifferentFormats() throws IOException {
        File pngFile = new File(tempDir, "test-image.png");
        BufferedImage pngImage = createTestImage(100, 100, Color.GREEN);
        ImageIO.write(pngImage, "png", pngFile);

        undoManager.saveStateBeforeOperation(pngFile);
        assertThat(undoManager.canUndo()).isTrue();

        BufferedImage modifiedImage = createTestImage(50, 50, Color.YELLOW);
        ImageIO.write(modifiedImage, "png", pngFile);

        undoManager.undo();

        BufferedImage restoredImage = ImageIO.read(pngFile);
        assertThat(restoredImage.getWidth()).isEqualTo(100);
        assertThat(restoredImage.getHeight()).isEqualTo(100);
    }

    @Test
    public void testUndo_returnsRestoredFile() throws IOException {
        undoManager.saveStateBeforeOperation(testFile);

        BufferedImage modifiedImage = createTestImage(50, 50, Color.BLUE);
        ImageIO.write(modifiedImage, "jpg", testFile);

        File restoredFile = undoManager.undo();
        assertThat(restoredFile).isEqualTo(testFile);
        assertThat(restoredFile.exists()).isTrue();
    }

    @Test
    public void testUndo_preservesExactFileSize() throws IOException {
        long originalFileSize = testFile.length();
        assertThat(originalFileSize).isGreaterThan(0);

        undoManager.saveStateBeforeOperation(testFile);

        BufferedImage modifiedImage = createTestImage(50, 50, Color.BLUE);
        ImageIO.write(modifiedImage, "jpg", testFile);

        long modifiedFileSize = testFile.length();
        assertThat(modifiedFileSize).isNotEqualTo(originalFileSize);

        undoManager.undo();

        long restoredFileSize = testFile.length();
        assertThat(restoredFileSize).isEqualTo(originalFileSize);
    }
}

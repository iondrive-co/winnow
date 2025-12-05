package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.ConfigManager;
import org.win.model.UndoManager;
import org.win.view.Window;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests person detection integration with image loading.
 */
public class RoiDetectionTest {
    private Stage imageStage;
    private Window window;
    private File tempDir;

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("roi-test").toFile();
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
    public void personDetection_loadsImageWithoutError() throws Exception {
        final File imageFile = new File(tempDir, "test_person_silhouette.jpg");

        // Create image: 800x600 white background with person-like silhouette
        // (black oval for body with smaller circle for head)
        createImageWithPersonSilhouette(imageFile, 800, 600);

        final ConfigManager config = new ConfigManager();
        final CountDownLatch latch = new CountDownLatch(1);
        final UndoManager undoManager = new UndoManager();

        Platform.runLater(() -> {
            imageStage = new Stage();
            window = new Window();
            window.setConfigManager(config);
            final Scene scene = window.displayFile(imageStage, imageFile, 1, 1, null, undoManager, null, null, null);
            imageStage.setScene(scene);
            imageStage.show();
            latch.countDown();
        });

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

        // Verify window and canvas were created successfully
        // Person detection may or may not find a person in the silhouette,
        // but it should not crash or error
        final CountDownLatch verifyLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            assertThat(window.imageCanvas).isNotNull();
            assertThat(window.imageCanvas.getImageWidth()).isEqualTo(800);
            assertThat(window.imageCanvas.getImageHeight()).isEqualTo(600);

            // Selection should be set (either to detected person or default full image)
            final int selectionWidth = window.imageCanvas.getSelectionWidth();
            final int selectionHeight = window.imageCanvas.getSelectionHeight();
            assertThat(selectionWidth).isGreaterThan(0);
            assertThat(selectionHeight).isGreaterThan(0);

            verifyLatch.countDown();
        });
        assertThat(verifyLatch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    private void createImageWithPersonSilhouette(final File outputFile, final int imgWidth, final int imgHeight) throws IOException {
        final BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = img.createGraphics();

        // Fill with white background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, imgWidth, imgHeight);

        // Draw simple person silhouette (black shapes)
        g2d.setColor(Color.BLACK);

        // Head (circle)
        final int headCenterX = imgWidth / 2;
        final int headCenterY = imgHeight / 3;
        final int headRadius = 60;
        g2d.fillOval(headCenterX - headRadius, headCenterY - headRadius, headRadius * 2, headRadius * 2);

        // Body (oval)
        final int bodyWidth = 120;
        final int bodyHeight = 200;
        final int bodyX = headCenterX - bodyWidth / 2;
        final int bodyY = headCenterY + headRadius;
        g2d.fillOval(bodyX, bodyY, bodyWidth, bodyHeight);

        g2d.dispose();

        ImageIO.write(img, "jpg", outputFile);
    }
}

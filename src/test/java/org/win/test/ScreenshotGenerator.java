package org.win.test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.win.model.UndoManager;
import org.win.view.Window;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility to launch the app with demo images and capture a screenshot for README.
 */
public final class ScreenshotGenerator extends Application {

    private static final String SCREENSHOT_FILENAME = "winnow-screenshot.png";
    private static Path tempDir;
    private static File screenshotOutput;
    private static String version;

    public static void main(final String[] args) {
        if (args.length > 0) {
            screenshotOutput = new File(args[0]);
        } else {
            screenshotOutput = new File(SCREENSHOT_FILENAME);
        }

        if (args.length > 1) {
            version = args[1];
        } else {
            version = "0.0.0";
        }

        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        // Create temp directory with short name to avoid scrollbar in control window
        final Path baseTempDir = Files.createTempDirectory("winnow-demo");
        tempDir = baseTempDir.resolve("photos");
        Files.createDirectories(tempDir);

        // Generate multiple images with SHORT filenames to avoid horizontal scrollbar
        // in the predictive filename editor
        final String[] filenames = {
            "IMG_01.jpg",
            "IMG_02.jpg",
            "IMG_03.jpg",
            "IMG_04.jpg",
            "IMG_05.jpg",
            "IMG_06.jpg",
            "IMG_07.jpg"
        };

        // Generate procedural abstract art for each file
        for (int i = 0; i < filenames.length; i++) {
            final File imageFile = new File(tempDir.toFile(), filenames[i]);
            TestImageGenerator.generateAbstractArt(i * 1000 + 42, imageFile, version);
        }

        // Launch the app with the third image (index 2)
        final File demoImage = new File(tempDir.toFile(), filenames[2]);
        final UndoManager undoManager = new UndoManager();
        final Window window = new Window();

        final Scene scene = window.displayFile(primaryStage, demoImage, 3, filenames.length, null, undoManager, null, null, null);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Winnow");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.show();

        // Apply rotation and selection, then reposition control window
        Platform.runLater(() -> {
            // Apply a small rotation to show the feature in action
            window.imageCanvas.rotateImage(15);

            Platform.runLater(() -> {
                // Set a partial selection rectangle to demonstrate cropping
                final double imageWidth = window.imageCanvas.getWidth();
                final double imageHeight = window.imageCanvas.getHeight();

                final double selectionWidth = imageWidth * 0.7;
                final double selectionHeight = imageHeight * 0.7;

                final double left = imageWidth * 0.1;
                final double top = imageHeight * 0.15;
                final double right = left + selectionWidth;
                final double bottom = top + selectionHeight;

                window.imageCanvas.setSelectionRegion(left, top, right, bottom);

                Platform.runLater(() -> {
                    // Reposition control window after rotation changes image size
                    window.repositionControlWindow();

                    Platform.runLater(() -> {
                        // Give positioning time to take effect
                        window.repositionControlWindow();
                    });
                });
            });
        });

        // Wait for both windows to be fully rendered
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        try {
                            // Additional delay to ensure control window is positioned and rendered
                            Thread.sleep(1500);

                        // Take screenshot by combining both window snapshots
                        takeScreenshot(window);

                        System.out.println("Screenshot saved to: " + screenshotOutput.getAbsolutePath());

                        // Cleanup and exit
                        window.closeControlWindow();
                        primaryStage.close();
                        cleanup(undoManager);
                        Platform.exit();
                        latch.countDown();
                    } catch (final Exception e) {
                        e.printStackTrace();
                        cleanup(undoManager);
                        Platform.exit();
                        latch.countDown();
                        }
                    });
                });
            });
        });

        // Wait for screenshot to complete
        new Thread(() -> {
            try {
                if (!latch.await(15, TimeUnit.SECONDS)) {
                    System.err.println("Screenshot generation timed out");
                    Platform.runLater(() -> {
                        cleanup(undoManager);
                        Platform.exit();
                    });
                }
                System.exit(0);
            } catch (final InterruptedException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }).start();
    }

    private void takeScreenshot(final Window window) throws IOException {
        final Stage imageStage = window.getImageStage();
        final Stage controlStage = window.getControlStage();

        // Capture snapshots of both windows
        final SnapshotParameters params = new SnapshotParameters();

        final WritableImage imageSnapshot = imageStage.getScene().snapshot(null);
        final WritableImage controlSnapshot = controlStage != null && controlStage.isShowing()
            ? controlStage.getScene().snapshot(null)
            : null;

        // Convert to BufferedImages
        final BufferedImage imageBuffer = SwingFXUtils.fromFXImage(imageSnapshot, null);
        final BufferedImage controlBuffer = controlSnapshot != null
            ? SwingFXUtils.fromFXImage(controlSnapshot, null)
            : null;

        // Combine the two images vertically
        final BufferedImage combined;
        if (controlBuffer != null) {
            final int width = Math.max(imageBuffer.getWidth(), controlBuffer.getWidth());
            final int height = imageBuffer.getHeight() + controlBuffer.getHeight();

            combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g2d = combined.createGraphics();

            // Draw image window at top
            g2d.drawImage(imageBuffer, 0, 0, null);

            // Center control window horizontally beneath image window
            final int controlX = (imageBuffer.getWidth() - controlBuffer.getWidth()) / 2;
            g2d.drawImage(controlBuffer, controlX, imageBuffer.getHeight(), null);

            g2d.dispose();
        } else {
            // If no control window, just use the image
            combined = imageBuffer;
        }

        // Save as PNG
        ImageIO.write(combined, "png", screenshotOutput);
    }

    private void cleanup(final UndoManager undoManager) {
        try {
            if (undoManager != null) {
                undoManager.cleanup();
            }
            if (tempDir != null) {
                // Delete the gallery directory and its parent temp directory
                final Path rootDir = tempDir.getParent();
                Files.walk(rootDir)
                        .sorted((a, b) -> -a.compareTo(b))
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (final IOException e) {
                                // Ignore
                            }
                        });
            }
        } catch (final Exception e) {
            // Ignore cleanup errors
        }
    }
}

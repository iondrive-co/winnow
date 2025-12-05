package org.win.test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.win.model.UndoManager;
import org.win.view.Window;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility to launch the app with demo images and capture a screenshot for README.
 */
public final class ScreenshotGenerator extends Application {

    private static final String SCREENSHOT_FILENAME = "winnow-screenshot.png";
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
        // Use the winnow.png image from docs folder as the demo image
        final File demoImage = new File("docs/winnow.png");
        if (!demoImage.exists()) {
            System.err.println("ERROR: docs/winnow.png not found!");
            Platform.exit();
            return;
        }

        final UndoManager undoManager = new UndoManager();
        final Window window = new Window();

        // Display the winnow.png image (single image, no gallery navigation)
        final Scene scene = window.displayFile(primaryStage, demoImage, 1, 1, null, undoManager, null, null, null);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Winnow");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.show();

        // Setup and take screenshot
        Platform.runLater(() -> {
            // Set a large selection rectangle - 80% of image size, centered
            final double imageWidth = window.imageCanvas.getWidth();
            final double imageHeight = window.imageCanvas.getHeight();

            final double selectionWidth = imageWidth * 0.80;
            final double selectionHeight = imageHeight * 0.75;
            final double left = (imageWidth - selectionWidth) / 2;
            final double top = (imageHeight - selectionHeight) / 2;
            final double right = left + selectionWidth;
            final double bottom = top + selectionHeight;

            window.imageCanvas.setSelectionRegion(left, top, right, bottom);

            Platform.runLater(() -> {
                // Reposition control window
                window.repositionControlWindow();

                Platform.runLater(() -> {
                    // Give positioning time to take effect
                    window.repositionControlWindow();
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
        final WritableImage imageSnapshot = imageStage.getScene().snapshot(null);
        final WritableImage controlSnapshot = controlStage != null && controlStage.isShowing()
            ? controlStage.getScene().snapshot(null)
            : null;

        // Convert to BufferedImages
        final BufferedImage imageBuffer = SwingFXUtils.fromFXImage(imageSnapshot, null);
        final BufferedImage controlBuffer = controlSnapshot != null
            ? SwingFXUtils.fromFXImage(controlSnapshot, null)
            : null;

        // Combine the two images vertically first
        final BufferedImage appScreenshot;
        if (controlBuffer != null) {
            final int width = Math.max(imageBuffer.getWidth(), controlBuffer.getWidth());
            final int height = imageBuffer.getHeight() + controlBuffer.getHeight();

            appScreenshot = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g2d = appScreenshot.createGraphics();

            // Draw image window at top
            g2d.drawImage(imageBuffer, 0, 0, null);

            // Center control window horizontally beneath image window
            final int controlX = (imageBuffer.getWidth() - controlBuffer.getWidth()) / 2;
            g2d.drawImage(controlBuffer, controlX, imageBuffer.getHeight(), null);

            g2d.dispose();
        } else {
            appScreenshot = imageBuffer;
        }

        // Create larger canvas with annotations
        final int margin = 150;
        final int annotatedWidth = appScreenshot.getWidth() + margin * 2;
        final int annotatedHeight = appScreenshot.getHeight() + margin * 2;

        final BufferedImage annotated = new BufferedImage(annotatedWidth, annotatedHeight, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = annotated.createGraphics();

        // Set high quality rendering
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // White background
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, annotatedWidth, annotatedHeight);

        // Draw the app screenshot in the center
        final int screenshotX = margin;
        final int screenshotY = margin;
        g2d.drawImage(appScreenshot, screenshotX, screenshotY, null);

        // Add version number in top-left corner
        g2d.setColor(java.awt.Color.DARK_GRAY);
        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 24));
        g2d.drawString("Winnow v" + version, 20, 40);

        // Add arrows and labels pointing to UI components
        // Rotation handle - pointing to top-right area where handle is positioned
        addAnnotation(g2d, screenshotX + 1040, screenshotY - 40, screenshotX + 1040, screenshotY + 15,
                      "Rotation handle", true);
        // Selection rectangle - pointing to top edge of selection
        addAnnotation(g2d, screenshotX + 500, screenshotY - 40, screenshotX + 500, screenshotY + 5,
                      "Selection rectangle", true);
        // Control window - shortened arrow pointing down to control panel
        addAnnotation(g2d, screenshotX + appScreenshot.getWidth() / 2, screenshotY + appScreenshot.getHeight() + 30,
                      screenshotX + appScreenshot.getWidth() / 2, screenshotY + appScreenshot.getHeight() - 20,
                      "Control window", false);

        g2d.dispose();

        // Save as PNG
        ImageIO.write(annotated, "png", screenshotOutput);
    }

    private void addAnnotation(final Graphics2D g2d, final int labelX, final int labelY,
                                final int arrowToX, final int arrowToY, final String label, final boolean above) {
        g2d.setColor(java.awt.Color.RED);
        g2d.setStroke(new java.awt.BasicStroke(2));

        // Draw arrow line
        g2d.drawLine(labelX, labelY, arrowToX, arrowToY);

        // Draw arrowhead
        final int arrowSize = 8;
        final double angle = Math.atan2(arrowToY - labelY, arrowToX - labelX);
        final int x1 = (int) (arrowToX - arrowSize * Math.cos(angle - Math.PI / 6));
        final int y1 = (int) (arrowToY - arrowSize * Math.sin(angle - Math.PI / 6));
        final int x2 = (int) (arrowToX - arrowSize * Math.cos(angle + Math.PI / 6));
        final int y2 = (int) (arrowToY - arrowSize * Math.sin(angle + Math.PI / 6));

        g2d.fillPolygon(new int[]{arrowToX, x1, x2}, new int[]{arrowToY, y1, y2}, 3);

        // Draw label
        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 16));
        final java.awt.FontMetrics fm = g2d.getFontMetrics();
        final int textWidth = fm.stringWidth(label);
        final int textX = labelX - textWidth / 2;
        final int textY = above ? labelY - 5 : labelY + fm.getHeight();

        g2d.drawString(label, textX, textY);
    }

    private void cleanup(final UndoManager undoManager) {
        try {
            if (undoManager != null) {
                undoManager.cleanup();
            }
        } catch (final Exception e) {
            // Ignore cleanup errors
        }
    }
}

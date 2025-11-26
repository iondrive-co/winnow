package org.win.test;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.win.model.UndoManager;
import org.win.view.Window;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manual visual test for large image (2048x1152).
 *
 * Run this to see the actual behavior with large images.
 * This will stay open for 30 seconds so you can inspect the window.
 */
public class ManualLargeImageTest extends Application {

    private static Path tempDir;
    private static File testFile;

    public static void main(final String[] args) throws Exception {
        // Create test image
        tempDir = Files.createTempDirectory("winnow-manual-test");
        testFile = new File(tempDir.toFile(), "large-test.jpg");
        TestImageGenerator.generateTestImage(42, testFile, 2048, 1152);

        System.out.println("=== Manual Large Image Test ===");
        System.out.println("Created test image: " + testFile.getAbsolutePath());
        System.out.println("Image size: 2048 x 1152");
        System.out.println("Window will stay open for 30 seconds...");
        System.out.println();

        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final UndoManager undoManager = new UndoManager();
        final Window window = new Window();

        final Scene scene = window.displayFile(primaryStage, testFile, 1, 1, null, undoManager, null, null, null);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Manual Test - Large Image (2048x1152)");
        primaryStage.setWidth(1200);
        primaryStage.setHeight(800);
        primaryStage.show();

        // Print diagnostic info after window is shown
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    printDiagnostics(window, primaryStage);

                    // Schedule window close after 30 seconds
                    new Thread(() -> {
                        try {
                            Thread.sleep(30000);
                            Platform.runLater(() -> {
                                System.out.println("\n=== Test Complete ===");
                                window.closeControlWindow();
                                primaryStage.close();
                                undoManager.cleanup();
                                cleanup();
                                Platform.exit();
                                System.exit(0);
                            });
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                });
            });
        });
    }

    private void printDiagnostics(final Window window, final Stage imageStage) {
        System.out.println("=== Diagnostic Information ===");
        System.out.println();

        // Canvas info
        System.out.println("Canvas:");
        System.out.println("  Size: " + window.imageCanvas.getWidth() + " x " + window.imageCanvas.getHeight());
        System.out.println("  Scale: " + window.imageCanvas.getScaleX());
        System.out.println("  Selection (actual): " +
            (int)window.imageCanvas.getSelectionLeft() + ", " +
            (int)window.imageCanvas.getSelectionTop() + " to " +
            window.imageCanvas.getSelectionWidth() + " x " +
            window.imageCanvas.getSelectionHeight());
        System.out.println("  Selection (visible): " +
            window.imageCanvas.getVisibleSelectionWidth() + " x " +
            window.imageCanvas.getVisibleSelectionHeight());
        System.out.println();

        // Image stage info
        System.out.println("Image Stage:");
        System.out.println("  Position: (" + (int)imageStage.getX() + ", " + (int)imageStage.getY() + ")");
        System.out.println("  Size: " + (int)imageStage.getWidth() + " x " + (int)imageStage.getHeight());
        System.out.println();

        // Control stage info
        final Stage controlStage = window.getControlStage();
        if (controlStage != null) {
            System.out.println("Control Stage:");
            System.out.println("  Position: (" + (int)controlStage.getX() + ", " + (int)controlStage.getY() + ")");
            System.out.println("  Size: " + (int)controlStage.getWidth() + " x " + (int)controlStage.getHeight());
            System.out.println("  Bottom edge: " + (int)(controlStage.getY() + controlStage.getHeight()));
        }
        System.out.println();

        // Screen info
        final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        System.out.println("Screen:");
        System.out.println("  Bounds: " + (int)screenBounds.getMinX() + ", " + (int)screenBounds.getMinY() +
            " to " + (int)screenBounds.getMaxX() + " x " + (int)screenBounds.getMaxY());
        System.out.println("  Size: " + (int)screenBounds.getWidth() + " x " + (int)screenBounds.getHeight());
        System.out.println();

        // Scene info
        System.out.println("Scene:");
        System.out.println("  Size: " + (int)imageStage.getScene().getWidth() + " x " + (int)imageStage.getScene().getHeight());
        System.out.println();

        System.out.println("=== What You Should See ===");
        System.out.println("1. Selection rectangle should cover the ENTIRE visible image");
        System.out.println("2. Blue corner handles should be at the EDGES of the visible viewport");
        System.out.println("3. Green move handle should be visible above the selection");
        System.out.println("4. Red rotate handle should be at top-right corner of viewport");
        System.out.println("5. Control window should be at the BOTTOM of the screen");
        System.out.println("6. You should be able to click and drag ANY of the handles");
        System.out.println();
    }

    private static void cleanup() {
        try {
            if (tempDir != null) {
                Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (final Exception e) {
                            // Ignore
                        }
                    });
            }
        } catch (final Exception e) {
            // Ignore
        }
    }
}

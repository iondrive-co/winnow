package org.win;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.win.control.InputDispatcher;
import org.win.model.UndoManager;
import org.win.view.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static javafx.scene.input.KeyCode.*;

public class Main extends Application {
    private static final double DRAG_THRESHOLD = 30;

    final Window window = new Window();
    int currentFileIndex = 0;
    private List<File> imageFiles = new ArrayList<>();
    private File imageDirectory;
    private double lastDragX = 0;
    private Map<Integer, UndoManager> undoManagers = new HashMap<>();
    private InputDispatcher dispatcher;
    private ConfigManager config;
    private Stage primaryStage;
    private long lastNavigationTime = 0;
    private static final long NAVIGATION_COOLDOWN_MS = 300;


    @Override
    public void start(final Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("Winnow");
        primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("wheat.png")));
        primaryStage.show();

        try {
            config = new ConfigManager();
        } catch (IOException e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            config = null;
        }

        // Get directory from config or prompt user
        String directoryPath = getDirectoryPath(primaryStage);
        if (directoryPath == null) {
            // User cancelled directory selection
            primaryStage.close();
            return;
        }

        loadImagesFromDirectory(directoryPath);
        currentFileIndex = loadSavedPosition();

        Scene scene = showFile(primaryStage, currentFileIndex);
        dispatcher = new InputDispatcher()
                .bindToKey(RIGHT, () -> {
                    currentFileIndex = (currentFileIndex + 1) % imageFiles.size();
                    savePosition();
                    Scene newScene = showFilePreservingSize(primaryStage, currentFileIndex);
                    setupSceneHandlers(newScene, primaryStage);
                })
                .bindToKey(LEFT, () -> {
                    currentFileIndex = Math.floorMod(currentFileIndex - 1, imageFiles.size());
                    savePosition();
                    Scene newScene = showFilePreservingSize(primaryStage, currentFileIndex);
                    setupSceneHandlers(newScene, primaryStage);
                })
                .bindToKey(EQUALS, () -> window.imageCanvas.zoom(1.1))  // Plus/Equals key = zoom in
                .bindToKey(MINUS, () -> window.imageCanvas.zoom(0.9))   // Minus key = zoom out
                // Right and Bottom edge manipulation (Ctrl only)
                .bindToKeyCombination(javafx.scene.input.KeyCode.D, true, false, false, () -> window.imageCanvas.expandSelectionRight(10))
                .bindToKeyCombination(javafx.scene.input.KeyCode.S, true, false, false, () -> window.imageCanvas.expandSelectionDown(10))
                .bindToKeyCombination(javafx.scene.input.KeyCode.A, true, false, false, () -> window.imageCanvas.reduceSelectionLeft(10))
                .bindToKeyCombination(javafx.scene.input.KeyCode.W, true, false, false, () -> window.imageCanvas.reduceSelectionUp(10))
                // Left and Top edge manipulation (Ctrl+Shift)
                .bindToKeyCombination(javafx.scene.input.KeyCode.D, true, true, false, () -> window.imageCanvas.expandLeftSelectionRight(10))
                .bindToKeyCombination(javafx.scene.input.KeyCode.A, true, true, false, () -> window.imageCanvas.reduceLeftSelectionLeft(10))
                .bindToKeyCombination(javafx.scene.input.KeyCode.S, true, true, false, () -> window.imageCanvas.expandTopSelectionDown(10))
                .bindToKeyCombination(javafx.scene.input.KeyCode.W, true, true, false, () -> window.imageCanvas.reduceTopSelectionUp(10))
                // Undo
                .bindToKeyCombination(javafx.scene.input.KeyCode.Z, true, false, false, () -> window.triggerUndo())
                // Rotation
                .bindToKeyCombination(javafx.scene.input.KeyCode.E, true, false, false, () -> window.rotateAndSave(5))
                .bindToKeyCombination(javafx.scene.input.KeyCode.Q, true, false, false, () -> window.rotateAndSave(-5))
                .zoomAction(zoomFactor -> window.imageCanvas.zoom(zoomFactor))
                .makeKeyBindings(primaryStage);

        setupSceneHandlers(scene, primaryStage);
    }

    private void setupSceneHandlers(Scene scene, Stage stage) {
        // Note: Mouse drag navigation is now handled by CustomImageCanvas, not Scene-level handlers
        dispatcher.makeSceneBindings(scene)
                .addSwipeHandler(scene, this::handleSwipe, stage);
    }

    /**
     * Gets the directory path from command line args, config file, or prompts the user.
     * @return The directory path, or null if user cancelled selection
     */
    private String getDirectoryPath(final Stage primaryStage) {
        if (config == null) {
            return null;
        }

        try {
            // Check if directory was provided as command line argument
            List<String> args = getParameters().getRaw();
            if (!args.isEmpty()) {
                String path = args.get(0);
                config.setLastDirectory(path);
                return path;
            }

            // Check if we have a saved directory
            String savedDirectory = config.getLastDirectory();
            if (savedDirectory != null && new File(savedDirectory).isDirectory()) {
                return savedDirectory;
            }

            // Prompt user to select a directory
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Image Directory");

            // Set initial directory to user's home if saved directory doesn't exist
            File homeDir = new File(System.getProperty("user.home"));
            if (homeDir.exists()) {
                directoryChooser.setInitialDirectory(homeDir);
            }

            File selectedDirectory = directoryChooser.showDialog(primaryStage);
            if (selectedDirectory != null) {
                String path = selectedDirectory.getAbsolutePath();
                config.setLastDirectory(path);
                return path;
            }

            return null; // User cancelled
        } catch (IOException e) {
            System.err.println("Failed to load/save configuration: " + e.getMessage());
            return null;
        }
    }

    private void loadImagesFromDirectory(final String path) {
        imageDirectory = Paths.get(path).toFile();

        if (!imageDirectory.exists()) {
            throw new RuntimeException("Path does not exist: " + imageDirectory.getAbsolutePath());
        }

        if (!imageDirectory.isDirectory()) {
            throw new RuntimeException("Path must be a directory: " + imageDirectory.getAbsolutePath());
        }

        File[] files = imageDirectory.listFiles();
        if (files != null) {
            List<File> sortedFiles = Arrays.asList(files);
            sortedFiles.sort((a, b) -> a.getName().compareTo(b.getName()));

            for (File file : sortedFiles) {
                if (isImageFile(file)) {
                    imageFiles.add(file);
                }
            }
        }

        if (imageFiles.isEmpty()) {
            throw new RuntimeException("No image files found in directory: " + imageDirectory.getAbsolutePath());
        }
    }

    private boolean isImageFile(final File file) {
        String filename = file.getName().toLowerCase();
        return filename.matches(".*\\.(jpg|jpeg|png|gif|bmp|tiff?)$");
    }

    private int loadSavedPosition() {
        if (config == null) {
            return 0;
        }

        int pos = config.getLastPosition();
        if (pos >= 0 && pos < imageFiles.size()) {
            return pos;
        }
        return 0;
    }

    private void savePosition() {
        if (config == null) {
            return;
        }

        try {
            config.setLastPosition(currentFileIndex);
        } catch (IOException e) {
            System.err.println("Failed to save position: " + e.getMessage());
        }
    }

    public void resetDragStart(double x) {
        lastDragX = x;
    }

    public void handleMouseDrag(double x) {
        if (imageFiles.isEmpty()) {
            return;
        }

        // Check cooldown to prevent rapid double-navigation during scene reload
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastNavigationTime < NAVIGATION_COOLDOWN_MS) {
            return;
        }

        if (Math.abs(x - lastDragX) > DRAG_THRESHOLD) {
            if (x > lastDragX) {
                // Drag right (rightward) = previous image
                currentFileIndex = Math.floorMod(currentFileIndex - 1, imageFiles.size());
            } else {
                // Drag left (leftward) = next image
                currentFileIndex = (currentFileIndex + 1) % imageFiles.size();
            }
            lastDragX = x;
            lastNavigationTime = currentTime;
            savePosition();

            // Reload the scene to show the new image
            Scene newScene = showFilePreservingSize(primaryStage, currentFileIndex);
            setupSceneHandlers(newScene, primaryStage);
        }
    }

    public void handleSwipe(double deltaX) {
        if (imageFiles.isEmpty()) {
            return;
        }
        if (deltaX > DRAG_THRESHOLD) {
            // Swipe right (rightward) = previous image
            currentFileIndex = Math.floorMod(currentFileIndex - 1, imageFiles.size());
            savePosition();
        } else if (deltaX < -DRAG_THRESHOLD) {
            // Swipe left (leftward) = next image
            currentFileIndex = (currentFileIndex + 1) % imageFiles.size();
            savePosition();
        }
    }

    private Scene showFilePreservingSize(final Stage primaryStage, final int pos) {
        double currentWidth = primaryStage.getWidth();
        double currentHeight = primaryStage.getHeight();

        Scene scene = showFile(primaryStage, pos);

        // Restore window size if it would have shrunk
        if (primaryStage.getWidth() < currentWidth) {
            primaryStage.setWidth(currentWidth);
        }
        if (primaryStage.getHeight() < currentHeight) {
            primaryStage.setHeight(currentHeight);
        }

        return scene;
    }

    private Scene showFile(final Stage primaryStage, final int pos) {
        if (imageFiles.isEmpty()) {
            throw new RuntimeException("No images loaded");
        }
        final File file = imageFiles.get(pos);
        if (!file.exists()) {
            throw new RuntimeException(file.getAbsolutePath());
        }

        // Get or create UndoManager for this image position
        UndoManager undoManager = undoManagers.computeIfAbsent(pos, k -> {
            try {
                return new UndoManager();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create UndoManager for position " + pos, e);
            }
        });

        final Scene scene = window.displayFile(primaryStage, file, pos + 1, imageFiles.size(), (oldFile, newFile) -> {
            // Update the imageFiles list when a file is renamed
            int index = imageFiles.indexOf(oldFile);
            if (index >= 0) {
                imageFiles.set(index, newFile);
            }
        }, undoManager, (restoredFile) -> {
            // Update imageFiles list if file was renamed
            // Use pos (the position when this scene was created) not currentFileIndex (which may have changed)
            File currentFile = imageFiles.get(pos);
            if (!currentFile.equals(restoredFile)) {
                imageFiles.set(pos, restoredFile);
            }
            // Update currentFileIndex to match the restored file position
            currentFileIndex = pos;

            // Reload the UI with the restored file and update dispatcher bindings
            Scene newScene = showFilePreservingSize(primaryStage, pos);
            setupSceneHandlers(newScene, primaryStage);
        }, () -> {
            // Navigate back
            currentFileIndex = Math.floorMod(currentFileIndex - 1, imageFiles.size());
            savePosition();
            Scene newScene = showFilePreservingSize(primaryStage, currentFileIndex);
            setupSceneHandlers(newScene, primaryStage);
        }, () -> {
            // Navigate forward
            currentFileIndex = (currentFileIndex + 1) % imageFiles.size();
            savePosition();
            Scene newScene = showFilePreservingSize(primaryStage, currentFileIndex);
            setupSceneHandlers(newScene, primaryStage);
        }, this::resetDragStart, this::handleMouseDrag, this::handleDirectoryChange);
        primaryStage.setScene(scene);

        // Constrain image window to screen bounds to prevent control window being pushed off screen
        javafx.geometry.Rectangle2D screenBounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        primaryStage.setMaxWidth(screenBounds.getWidth());
        primaryStage.setMaxHeight(screenBounds.getHeight() * 0.9); // Leave room for control window

        return scene;
    }

    private void handleDirectoryChange() {
        // Prompt user to select a new directory
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Image Directory");

        // Set initial directory to current image directory
        if (imageDirectory != null && imageDirectory.exists()) {
            directoryChooser.setInitialDirectory(imageDirectory);
        }

        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            try {
                String path = selectedDirectory.getAbsolutePath();

                // Update config with new directory
                if (config != null) {
                    config.setLastDirectory(path);
                    config.setLastPosition(0); // Reset position
                }

                // Clear old state
                imageFiles.clear();
                undoManagers.values().forEach(UndoManager::cleanup);
                undoManagers.clear();
                currentFileIndex = 0;

                // Load new images and show first image
                loadImagesFromDirectory(path);
                Scene newScene = showFilePreservingSize(primaryStage, 0);
                setupSceneHandlers(newScene, primaryStage);
            } catch (IOException e) {
                System.err.println("Failed to update configuration: " + e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        // Clean up all UndoManagers
        for (UndoManager undoManager : undoManagers.values()) {
            undoManager.cleanup();
        }
        undoManagers.clear();

        // Close control window
        window.closeControlWindow();
    }

    public static void main(final String[] args) {
        launch(args);
    }
}

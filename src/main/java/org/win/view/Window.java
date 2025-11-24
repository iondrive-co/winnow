package org.win.view;

import clojure.lang.IPersistentMap;
import ij.ImagePlus;
import ij.io.Opener;
import ij.process.ImageProcessor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.win.model.UndoManager;
import org.win.util.FilenameClassifier;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Window {
    public CustomImageCanvas imageCanvas;
    private Button undoButton;
    private Button cropButton;
    private File currentFile;
    private UndoManager undoManager;
    private BiConsumer<File, File> onFileRenamed;
    private FilenameEditor filenameEditor;
    private Stage controlStage;
    private Stage imageStage;
    private Label imageDimensionLabel;
    private Label selectionDimensionLabel;
    private Label directoryLabel;
    private Runnable onDirectoryChange;
    private org.win.ConfigManager configManager;

    public void setConfigManager(final org.win.ConfigManager configManager) {
        this.configManager = configManager;
    }

    // Overload for backward compatibility with tests
    public Scene displayFile(final Stage imageStage, final File file, final int currentIndex, final int totalFiles, final BiConsumer<File, File> onFileRenamed, final UndoManager undoManager, final Consumer<File> onUndo, final Runnable onNavigateBack, final Runnable onNavigateForward) {
        return displayFile(imageStage, file, currentIndex, totalFiles, onFileRenamed, undoManager, onUndo, onNavigateBack, onNavigateForward, null, null, null);
    }

    public Scene displayFile(final Stage imageStage, final File file, final int currentIndex, final int totalFiles, final BiConsumer<File, File> onFileRenamed, final UndoManager undoManager, final Consumer<File> onUndo, final Runnable onNavigateBack, final Runnable onNavigateForward, final Consumer<Double> onDragStart, final Consumer<Double> onNavigationDrag) {
        return displayFile(imageStage, file, currentIndex, totalFiles, onFileRenamed, undoManager, onUndo, onNavigateBack, onNavigateForward, onDragStart, onNavigationDrag, null);
    }

    public Scene displayFile(final Stage imageStage, final File file, final int currentIndex, final int totalFiles, final BiConsumer<File, File> onFileRenamed, final UndoManager undoManager, final Consumer<File> onUndo, final Runnable onNavigateBack, final Runnable onNavigateForward, final Consumer<Double> onDragStart, final Consumer<Double> onNavigationDrag, final Runnable onDirectoryChange) {
        this.currentFile = file;
        this.undoManager = undoManager;
        this.onFileRenamed = onFileRenamed;
        this.imageStage = imageStage;
        this.onDirectoryChange = onDirectoryChange;

        final ImagePlus imagePlus = new Opener().openImage(file.getAbsolutePath());
        final ImageProcessor imageProcessor = imagePlus.getProcessor();
        imageCanvas = new CustomImageCanvas(imageProcessor.getBufferedImage());

        imageCanvas.setOnRotationComplete(rotationAmount -> {
            try {
                saveImageOperation(imageCanvas.getCurrentImage());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        if (onDragStart != null) {
            imageCanvas.setOnDragStart(onDragStart);
        }
        if (onNavigationDrag != null) {
            imageCanvas.setOnNavigationDrag(onNavigationDrag);
        }

        imageCanvas.setOnSelectionChange(() -> updateDimensionDisplay());

        Group group = new Group(imageCanvas);
        StackPane centerPane = new StackPane(group);
        centerPane.setStyle("-fx-background-color: black;");

        // In normal mode (with Stage), create separate control window
        // In test mode (null Stage), include controls in main scene for backward compatibility
        if (imageStage != null) {
            // Production mode: separate control window
            Scene imageScene = new Scene(centerPane);
            createOrUpdateControlWindow(file, currentIndex, totalFiles, onUndo, onNavigateBack, onNavigateForward);

            // Set up focus cycling between windows (Ctrl+Tab)
            imageScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.TAB && event.isControlDown() && controlStage != null) {
                    javafx.application.Platform.runLater(() -> {
                        controlStage.requestFocus();
                        filenameEditor.requestFocus();
                    });
                    event.consume();
                }
            });

            return imageScene;
        } else {
            // Test mode: keep controls in main scene (old layout)
            return createSceneWithEmbeddedControls(centerPane, file, currentIndex, totalFiles, onUndo, onNavigateBack, onNavigateForward);
        }
    }

    private Scene createSceneWithEmbeddedControls(StackPane centerPane, File file, int currentIndex, int totalFiles, Consumer<File> onUndo, Runnable onNavigateBack, Runnable onNavigateForward) {
        // Create filename editor based on config
        this.filenameEditor = createFilenameEditor(file);
        HBox.setHgrow((HBox) filenameEditor, Priority.ALWAYS);

        this.undoButton = new Button("Undo");
        undoButton.setDisable(!undoManager.canUndo());
        undoButton.setOnAction(e -> {
            try {
                File restoredFile = undoManager.undo();
                undoButton.setDisable(!undoManager.canUndo());
                if (onUndo != null) {
                    onUndo.accept(restoredFile);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        undoButton.setStyle("-fx-font-size: 18px; -fx-padding: 15px 25px;");
        undoButton.setMinWidth(Button.USE_PREF_SIZE);

        this.cropButton = new Button("Crop Image");
        cropButton.setOnAction(e -> {
            try {
                saveImageOperation(imageCanvas.cropImage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        cropButton.setStyle("-fx-font-size: 18px; -fx-padding: 15px 25px;");
        cropButton.setMinWidth(Button.USE_PREF_SIZE);

        Button backButton = new Button("←");
        backButton.setOnAction(e -> {
            if (onNavigateBack != null) {
                onNavigateBack.run();
            }
        });
        backButton.setStyle("-fx-font-size: 18px; -fx-padding: 15px 25px;");
        backButton.setMinWidth(Button.USE_PREF_SIZE);

        Button forwardButton = new Button("→");
        forwardButton.setOnAction(e -> {
            if (onNavigateForward != null) {
                onNavigateForward.run();
            }
        });
        forwardButton.setStyle("-fx-font-size: 18px; -fx-padding: 15px 25px;");
        forwardButton.setMinWidth(Button.USE_PREF_SIZE);

        Label positionLabel = new Label(String.format("%d/%d", currentIndex, totalFiles));
        positionLabel.setStyle("-fx-font-size: 16px;");

        VBox dimensionDisplay = createDimensionDisplay();

        // Create filename section with directory above
        Label dirLabel = createDirectoryLabel(file);
        VBox fileInfoBox = new VBox(dirLabel, (HBox) filenameEditor);
        fileInfoBox.setSpacing(2);
        fileInfoBox.setAlignment(Pos.CENTER_LEFT);

        // Wrap in ScrollPane to prevent button resizing when adding fields
        ScrollPane scrollPane = new ScrollPane(fileInfoBox);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setPrefHeight(80);
        scrollPane.setMinHeight(80);
        scrollPane.setPrefViewportWidth(400);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        HBox controlBox = new HBox(undoButton, cropButton, dimensionDisplay, backButton, positionLabel, forwardButton, scrollPane);
        controlBox.setAlignment(Pos.CENTER_LEFT);
        controlBox.setSpacing(20);
        controlBox.setPadding(new Insets(15, 20, 15, 20));

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(centerPane);
        borderPane.setBottom(controlBox);

        Scene scene = new Scene(borderPane);

        // Handle keyboard navigation for test mode
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            // Don't intercept arrow keys if user is editing text
            final Node focusOwner = scene.getFocusOwner();
            if (focusOwner instanceof TextField || focusOwner instanceof ComboBox) {
                return;
            }

            if (event.getCode() == KeyCode.LEFT) {
                // Left arrow: navigate back
                if (onNavigateBack != null) {
                    onNavigateBack.run();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                // Right arrow: navigate forward
                if (onNavigateForward != null) {
                    onNavigateForward.run();
                }
                event.consume();
            }
        });

        // Set initial focus on forward button
        javafx.application.Platform.runLater(() -> {
            forwardButton.requestFocus();
        });

        return scene;
    }

    private void createOrUpdateControlWindow(final File file, final int currentIndex, final int totalFiles, final Consumer<File> onUndo, final Runnable onNavigateBack, final Runnable onNavigateForward) {
        // Create filename editor based on config
        this.filenameEditor = createFilenameEditor(file);

        this.undoButton = new Button("Undo");
        undoButton.setDisable(!undoManager.canUndo());
        undoButton.setOnAction(e -> {
            try {
                File restoredFile = undoManager.undo();
                undoButton.setDisable(!undoManager.canUndo());
                if (onUndo != null) {
                    onUndo.accept(restoredFile);
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        undoButton.setStyle("-fx-font-size: 18px; -fx-padding: 15px 25px;");
        undoButton.setMinWidth(Button.USE_PREF_SIZE);

        this.cropButton = new Button("Crop Image");
        cropButton.setOnAction(e -> {
            try {
                saveImageOperation(imageCanvas.cropImage());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        cropButton.setStyle("-fx-font-size: 18px; -fx-padding: 15px 25px;");
        cropButton.setMinWidth(Button.USE_PREF_SIZE);

        Button backButton = new Button("←");
        backButton.setOnAction(e -> {
            if (onNavigateBack != null) {
                onNavigateBack.run();
            }
        });
        backButton.setStyle("-fx-font-size: 18px; -fx-padding: 15px 25px;");
        backButton.setMinWidth(Button.USE_PREF_SIZE);

        Button forwardButton = new Button("→");
        forwardButton.setOnAction(e -> {
            if (onNavigateForward != null) {
                onNavigateForward.run();
            }
        });
        forwardButton.setStyle("-fx-font-size: 18px; -fx-padding: 15px 25px;");
        forwardButton.setMinWidth(Button.USE_PREF_SIZE);

        Label positionLabel = new Label(String.format("%d/%d", currentIndex, totalFiles));
        positionLabel.setStyle("-fx-font-size: 16px;");

        VBox dimensionDisplay = createDimensionDisplay();

        // Create filename section with directory above
        Label dirLabel = createDirectoryLabel(file);
        VBox fileInfoBox = new VBox(dirLabel, (HBox) filenameEditor);
        fileInfoBox.setSpacing(2);
        fileInfoBox.setAlignment(Pos.CENTER_LEFT);

        // Wrap in ScrollPane to prevent button resizing when adding fields
        ScrollPane scrollPane = new ScrollPane(fileInfoBox);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setPrefHeight(80);
        scrollPane.setMinHeight(80);
        scrollPane.setPrefViewportWidth(400);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        HBox controlBox = new HBox(undoButton, cropButton, dimensionDisplay, backButton, positionLabel, forwardButton, scrollPane);
        controlBox.setAlignment(Pos.CENTER_LEFT);
        controlBox.setSpacing(20);
        controlBox.setPadding(new Insets(15, 20, 15, 20));

        Scene controlScene = new Scene(controlBox);

        // Handle keyboard shortcuts
        controlScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            // Don't intercept arrow keys if user is editing text
            final Node focusOwner = controlScene.getFocusOwner();
            final boolean isEditingText = focusOwner instanceof TextField || focusOwner instanceof ComboBox;

            if (event.getCode() == KeyCode.TAB && event.isControlDown()) {
                // Ctrl+Tab: cycle back to image window
                javafx.application.Platform.runLater(() -> {
                    imageStage.requestFocus();
                    imageCanvas.requestFocus();
                });
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT && !event.isControlDown() && !isEditingText) {
                // Left arrow: navigate back
                if (onNavigateBack != null) {
                    onNavigateBack.run();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT && !event.isControlDown() && !isEditingText) {
                // Right arrow: navigate forward
                if (onNavigateForward != null) {
                    onNavigateForward.run();
                }
                event.consume();
            } else if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.D) {
                // Shift+Ctrl+D: expand left selection right
                imageCanvas.expandLeftSelectionRight(10);
                event.consume();
            } else if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.A) {
                // Shift+Ctrl+A: reduce left selection left
                imageCanvas.reduceLeftSelectionLeft(10);
                event.consume();
            } else if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.S) {
                // Shift+Ctrl+S: expand top selection down
                imageCanvas.expandTopSelectionDown(10);
                event.consume();
            } else if (event.isControlDown() && event.isShiftDown() && event.getCode() == KeyCode.W) {
                // Shift+Ctrl+W: reduce top selection up
                imageCanvas.reduceTopSelectionUp(10);
                event.consume();
            } else if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.D) {
                // Ctrl+D: expand right selection right
                imageCanvas.expandSelectionRight(10);
                event.consume();
            } else if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.A) {
                // Ctrl+A: reduce right selection left
                imageCanvas.reduceSelectionLeft(10);
                event.consume();
            } else if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.S) {
                // Ctrl+S: expand bottom selection down
                imageCanvas.expandSelectionDown(10);
                event.consume();
            } else if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.W) {
                // Ctrl+W: reduce bottom selection up
                imageCanvas.reduceSelectionUp(10);
                event.consume();
            } else if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.E) {
                // Ctrl+E: rotate right
                rotateAndSave(5);
                event.consume();
            } else if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.Q) {
                // Ctrl+Q: rotate left
                rotateAndSave(-5);
                event.consume();
            } else if (event.isControlDown() && !event.isShiftDown() && event.getCode() == KeyCode.Z) {
                // Ctrl+Z: undo
                triggerUndo();
                event.consume();
            }
        });

        if (controlStage == null) {
            // Create control stage for first time
            controlStage = new Stage();
            controlStage.setTitle("Winnow Controls");
            controlStage.setScene(controlScene);
            controlStage.setAlwaysOnTop(true);

            // Link window lifecycles - closing one closes the other
            controlStage.setOnCloseRequest(event -> {
                if (imageStage != null) {
                    imageStage.close();
                }
            });

            imageStage.setOnCloseRequest(event -> {
                if (controlStage != null) {
                    controlStage.close();
                }
            });

            controlStage.show();

            // Position below image window after both windows are shown and sized
            // Use nested runLater to ensure imageStage has finished its layout
            javafx.application.Platform.runLater(() -> {
                javafx.application.Platform.runLater(() -> {
                    positionControlWindow();
                    if (controlStage != null) {
                        controlStage.requestFocus();
                        forwardButton.requestFocus();
                    }
                });
            });
        } else {
            // Update existing control window
            controlStage.setScene(controlScene);

            // Reposition control window for new image (which may have different size)
            // and set focus on forward button
            javafx.application.Platform.runLater(() -> {
                javafx.application.Platform.runLater(() -> {
                    positionControlWindow();
                    controlStage.requestFocus();
                    forwardButton.requestFocus();
                });
            });
        }
    }

    private void positionControlWindow() {
        if (imageStage == null || controlStage == null) {
            return;
        }

        // Ensure both stages are sized to their scenes (only if they have valid sizes)
        if (imageStage.getScene() != null && imageStage.getScene().getWidth() > 0 && imageStage.getScene().getHeight() > 0 && imageStage.getWidth() > 0 && imageStage.getHeight() > 0) {
            imageStage.sizeToScene();
        }
        if (controlStage.getScene() != null && controlStage.getScene().getWidth() > 0 && controlStage.getScene().getHeight() > 0 && controlStage.getWidth() > 0 && controlStage.getHeight() > 0) {
            controlStage.sizeToScene();
        }

        double imageX = imageStage.getX();
        double imageY = imageStage.getY();
        double imageHeight = imageStage.getHeight();
        double controlHeight = controlStage.getHeight();

        // Don't position if dimensions are invalid
        if (imageHeight <= 0 || controlHeight <= 0) {
            return;
        }

        // Get screen bounds
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Position below image window (at the bottom of the image window)
        double targetY = imageY + imageHeight;

        // Check if control window would go off bottom of screen
        if (targetY + controlHeight > screenBounds.getMaxY()) {
            // Position as low as possible while keeping full window visible
            targetY = screenBounds.getMaxY() - controlHeight;
        }

        controlStage.setX(imageX);
        controlStage.setY(targetY);
    }

    public void closeControlWindow() {
        if (controlStage != null) {
            controlStage.close();
            controlStage = null;
        }
    }

    private String getImageFormat(final File file) {
        String filename = file.getName().toLowerCase();
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "jpg";
        if (filename.endsWith(".png")) return "png";
        if (filename.endsWith(".gif")) return "gif";
        if (filename.endsWith(".bmp")) return "bmp";
        if (filename.endsWith(".tiff") || filename.endsWith(".tif")) return "tiff";
        return "png";
    }

    private void saveImageOperation(RenderedImage image) throws IOException {
        undoManager.saveStateBeforeOperation(currentFile);

        final String newFilename = filenameEditor.getFilename();
        final File outputFile = new File(currentFile.getParentFile(), newFilename);
        final String format = getImageFormat(currentFile);
        ImageIO.write(image, format, outputFile);

        if (!newFilename.equals(currentFile.getName())) {
            undoManager.setRenamedFileForLastOperation(outputFile);
            currentFile.delete();
            if (onFileRenamed != null) {
                onFileRenamed.accept(currentFile, outputFile);
            }
            currentFile = outputFile;
        }

        undoButton.setDisable(!undoManager.canUndo());
    }

    public void rotateAndSave(double degrees) {
        try {
            saveImageOperation(imageCanvas.rotateImage(degrees));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void triggerUndo() {
        if (undoButton != null && !undoButton.isDisabled()) {
            undoButton.fire();
        }
    }

    private VBox createDimensionDisplay() {
        // Create labels for image and selection dimensions
        imageDimensionLabel = new Label();
        imageDimensionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: black;");

        selectionDimensionLabel = new Label();
        selectionDimensionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: blue;");

        // Update labels with current values
        updateDimensionDisplay();

        VBox dimensionBox = new VBox(imageDimensionLabel, selectionDimensionLabel);
        dimensionBox.setAlignment(Pos.CENTER_LEFT);
        dimensionBox.setSpacing(2);

        return dimensionBox;
    }

    private void updateDimensionDisplay() {
        if (imageCanvas != null && imageDimensionLabel != null && selectionDimensionLabel != null) {
            final int imageWidth = imageCanvas.getImageWidth();
            final int imageHeight = imageCanvas.getImageHeight();
            final int selectionWidth = imageCanvas.getVisibleSelectionWidth();
            final int selectionHeight = imageCanvas.getVisibleSelectionHeight();

            imageDimensionLabel.setText(imageWidth + " x " + imageHeight);
            selectionDimensionLabel.setText(selectionWidth + " x " + selectionHeight);
        }
    }

    private String getLastTwoDirectoryComponents(File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            return file.getName();
        }

        File grandparent = parent.getParentFile();
        if (grandparent == null) {
            return parent.getName();
        }

        return grandparent.getName() + File.separator + parent.getName();
    }

    private Label createDirectoryLabel(File file) {
        directoryLabel = new Label(getLastTwoDirectoryComponents(file));
        directoryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray; -fx-cursor: hand;");
        directoryLabel.setOnMouseClicked(event -> {
            if (onDirectoryChange != null) {
                onDirectoryChange.run();
            }
        });
        return directoryLabel;
    }

    public Stage getImageStage() {
        return imageStage;
    }

    public Stage getControlStage() {
        return controlStage;
    }

    private FilenameEditor createFilenameEditor(final File file) {
        // Check config to determine which editor to use
        final boolean useSimple = configManager != null && configManager.isUseSimpleFilenameEditor();

        if (useSimple) {
            return new SimpleFilenameEditor(file);
        } else {
            final IPersistentMap model = buildModelFromDirectory(file.getParentFile());
            return new PredictiveFilenameEditor(file, model);
        }
    }

    private IPersistentMap buildModelFromDirectory(final File directory) {
        if (directory == null || !directory.isDirectory()) {
            return FilenameClassifier.buildModel(Collections.emptyList());
        }

        final File[] files = directory.listFiles((dir, name) -> {
            final String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                   lowerName.endsWith(".png") || lowerName.endsWith(".gif") ||
                   lowerName.endsWith(".bmp") || lowerName.endsWith(".tiff") ||
                   lowerName.endsWith(".tif");
        });

        if (files == null || files.length == 0) {
            return FilenameClassifier.buildModel(Collections.emptyList());
        }

        final String[] filenames = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            filenames[i] = files[i].getName();
        }

        return FilenameClassifier.buildModel(Arrays.asList(filenames));
    }
}

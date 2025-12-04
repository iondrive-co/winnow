package org.win.view;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class CustomImageCanvas extends Canvas {

    private static final int SELECTION_RECTANGLE_SIZE = 30;
    private static final int SELECTION_RECTANGLE_LINE_SIZE = 2;
    private static final Color SELECTION_COLOR = Color.BLUE;
    private static final int MOVE_HANDLE_SIZE = 40;
    private static final int MOVE_HANDLE_PADDING = 10;
    private static final Color MOVE_HANDLE_COLOR = Color.rgb(0, 200, 0, 0.8); // Semi-transparent green
    private static final int ROTATION_HANDLE_SIZE = 35;
    private static final int ROTATION_HANDLE_OFFSET = 50; // Distance from corner
    private static final Color ROTATION_HANDLE_COLOR = Color.RED;
    private static final double ROTATION_INCREMENT = 5.0; // Degrees per click
    private static final double CLICK_THRESHOLD = 5.0; // Pixels - max movement to be considered a click

    private BufferedImage image;
    private BufferedImage originalImage;  // Store unrotated original for quality preservation
    private double selectionTopLeftX, selectionTopLeftY, selectionBottomRightX, selectionBottomRightY;
    private double moveHandleX, moveHandleY;  // Position of move handle
    private double prevX, prevY;
    private double mousePressX, mousePressY;  // Track initial press position for click detection
    private boolean dragging;
    private boolean rotationDragging;  // Track if currently dragging rotation (for performance)
    private Corner selectedCorner;
    private int originalWidth;
    private int originalHeight;
    private double cumulativeRotation;
    private double rotationDragStartAngle;
    private double rotationDragStartCumulative;
    private java.util.function.Consumer<Double> onRotationComplete;
    private java.util.function.Consumer<Double> onDragStart;
    private java.util.function.Consumer<Double> onNavigationDrag;
    private double navigationDragStartX;
    private Runnable onSelectionChange;
    private javafx.beans.value.ChangeListener<Number> sceneWidthListener;
    private javafx.beans.value.ChangeListener<Number> sceneHeightListener;
    private javafx.scene.Scene lastScene;
    private javafx.animation.PauseTransition redrawDebouncer;

    public CustomImageCanvas(final BufferedImage image) {
        super(image.getWidth(), image.getHeight());
        this.image = image;
        this.originalImage = copyImage(image);  // Store a copy of the original for rotation quality
        this.originalWidth = image.getWidth();
        this.originalHeight = image.getHeight();
        this.cumulativeRotation = 0.0;
        this.selectionTopLeftX = 0;
        this.selectionTopLeftY = 0;
        this.selectionBottomRightX = image.getWidth();
        this.selectionBottomRightY = image.getHeight();
        this.dragging = false;
        this.selectedCorner = Corner.NONE;

        drawImage();
        drawROI();

        // Set up scene change listener to redraw when scene has valid dimensions
        // This ensures handles are positioned correctly after zoom-to-fit and navigation
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            // Remove listeners from old scene to prevent memory leaks
            if (lastScene != null) {
                if (sceneWidthListener != null) {
                    lastScene.widthProperty().removeListener(sceneWidthListener);
                }
                if (sceneHeightListener != null) {
                    lastScene.heightProperty().removeListener(sceneHeightListener);
                }
            }

            if (newScene != null) {
                // Check if scene already has valid dimensions
                if (newScene.getWidth() > 0 && newScene.getHeight() > 0) {
                    // Scene has valid dimensions, redraw immediately
                    javafx.application.Platform.runLater(this::redraw);
                } else {
                    // Shared runnable to check and redraw whenever dimensions change
                    // Use debouncing to avoid excessive redraws during rapid dimension changes
                    final Runnable checkAndRedraw = () -> {
                        if (newScene.getWidth() > 0 && newScene.getHeight() > 0) {
                            // Debounce redraws - wait 50ms for dimensions to stabilize
                            if (redrawDebouncer != null) {
                                redrawDebouncer.stop();
                            }

                            redrawDebouncer = new javafx.animation.PauseTransition(javafx.util.Duration.millis(50));
                            redrawDebouncer.setOnFinished(e -> {
                                redraw();
                                redrawDebouncer = null;
                            });
                            redrawDebouncer.play();
                        }
                    };

                    // Listen for width changes
                    sceneWidthListener = (obs, oldWidth, newWidth) -> checkAndRedraw.run();
                    newScene.widthProperty().addListener(sceneWidthListener);

                    // Listen for height changes
                    sceneHeightListener = (obs, oldHeight, newHeight) -> checkAndRedraw.run();
                    newScene.heightProperty().addListener(sceneHeightListener);
                }
                lastScene = newScene;
            }
        });

        // Use addEventHandler instead of setOnMouseXxx to allow events to bubble to Scene
        addEventHandler(javafx.scene.input.MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
        addEventHandler(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
        addEventHandler(javafx.scene.input.MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
    }

    public BufferedImage cropImage() {
        int x = (int) Math.round(selectionTopLeftX);
        int y = (int) Math.round(selectionTopLeftY);
        int roiWidth = (int) Math.round(selectionBottomRightX - selectionTopLeftX);
        int roiHeight = (int) Math.round(selectionBottomRightY - selectionTopLeftY);

        // Clamp x and y to valid image coordinates
        x = Math.max(0, Math.min(x, image.getWidth() - 1));
        y = Math.max(0, Math.min(y, image.getHeight() - 1));

        // Ensure that the roiWidth and roiHeight are valid and do not exceed the image boundaries
        roiWidth = Math.max(1, Math.min(roiWidth, image.getWidth() - x));
        roiHeight = Math.max(1, Math.min(roiHeight, image.getHeight() - y));

        // Crop the image using the calculated coordinates and dimensions
        BufferedImage croppedImage = image.getSubimage(x, y, roiWidth, roiHeight);

        // Reset the selection to the whole image
        selectionTopLeftX = 0;
        selectionTopLeftY = 0;
        selectionBottomRightX = croppedImage.getWidth();
        selectionBottomRightY = croppedImage.getHeight();
        // Update the displayed image with the cropped image
        this.setWidth(croppedImage.getWidth());
        this.setHeight(croppedImage.getHeight());
        this.image = croppedImage;
        // Reset original image, dimensions and rotation after crop
        this.originalImage = copyImage(croppedImage);
        this.originalWidth = croppedImage.getWidth();
        this.originalHeight = croppedImage.getHeight();
        this.cumulativeRotation = 0.0;
        drawImage();
        drawROI();

        return croppedImage;
    }

    public void zoom(final double zoomFactor) {
        setScaleX(getScaleX() * zoomFactor);
        setScaleY(getScaleY() * zoomFactor);
        setLayoutX(0);
        setLayoutY(0);
        redraw();
    }

    public BufferedImage rotateImage(double degrees) {
        cumulativeRotation += degrees;
        applyRotation();
        return image;
    }

    public BufferedImage resizeImage(final int newWidth, final int newHeight) {
        // Use high-quality bicubic interpolation for resizing
        final BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        final java.awt.Graphics2D g2d = resizedImage.createGraphics();

        // High-quality rendering hints
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                            java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the scaled image
        g2d.drawImage(image, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // Update canvas and image state
        setWidth(newWidth);
        setHeight(newHeight);
        this.image = resizedImage;
        this.originalImage = copyImage(resizedImage);
        this.originalWidth = newWidth;
        this.originalHeight = newHeight;
        this.cumulativeRotation = 0.0;

        // Reset selection to full image
        selectionTopLeftX = 0;
        selectionTopLeftY = 0;
        selectionBottomRightX = newWidth;
        selectionBottomRightY = newHeight;

        drawImage();
        drawROI();

        return resizedImage;
    }

    // Methods for testing
    public void setSelectionRegion(double topLeftX, double topLeftY, double bottomRightX, double bottomRightY) {
        this.selectionTopLeftX = topLeftX;
        this.selectionTopLeftY = topLeftY;
        this.selectionBottomRightX = bottomRightX;
        this.selectionBottomRightY = bottomRightY;
        redraw();
    }

    // Keyboard selection manipulation methods - Right and Bottom edges
    public void expandSelectionRight(int pixels) {
        selectionBottomRightX = Math.min(selectionBottomRightX + pixels, image.getWidth());
        redraw();
    }

    public void expandSelectionDown(int pixels) {
        selectionBottomRightY = Math.min(selectionBottomRightY + pixels, image.getHeight());
        redraw();
    }

    public void reduceSelectionLeft(int pixels) {
        selectionBottomRightX = Math.max(selectionBottomRightX - pixels, selectionTopLeftX + 1);
        redraw();
    }

    public void reduceSelectionUp(int pixels) {
        selectionBottomRightY = Math.max(selectionBottomRightY - pixels, selectionTopLeftY + 1);
        redraw();
    }

    // Keyboard selection manipulation methods - Left and Top edges
    public void expandLeftSelectionRight(int pixels) {
        selectionTopLeftX = Math.min(selectionTopLeftX + pixels, selectionBottomRightX - 1);
        redraw();
    }

    public void reduceLeftSelectionLeft(int pixels) {
        selectionTopLeftX = Math.max(selectionTopLeftX - pixels, 0);
        redraw();
    }

    public void expandTopSelectionDown(int pixels) {
        selectionTopLeftY = Math.min(selectionTopLeftY + pixels, selectionBottomRightY - 1);
        redraw();
    }

    public void reduceTopSelectionUp(int pixels) {
        selectionTopLeftY = Math.max(selectionTopLeftY - pixels, 0);
        redraw();
    }

    private void drawImage() {
        Image fxImage = SwingFXUtils.toFXImage(image, null);
        GraphicsContext gc = getGraphicsContext2D();
        gc.drawImage(fxImage, 0, 0);
    }

    private void redraw() {
        drawImage();
        drawROI();
        notifySelectionChange();
    }

    private void notifySelectionChange() {
        if (onSelectionChange != null) {
            onSelectionChange.run();
        }
    }

    private void drawROI() {
        final double[] visibleSelection = getVisibleSelection();
        final double visibleLeft = visibleSelection[0];
        final double visibleTop = visibleSelection[1];
        final double visibleRight = visibleSelection[2];
        final double visibleBottom = visibleSelection[3];

        final GraphicsContext gc = getGraphicsContext2D();
        gc.setStroke(SELECTION_COLOR);
        // Adjust line width to account for scale so it remains constant size on screen
        final double scale = Math.max(getScaleX(), 0.1); // Prevent division by zero
        gc.setLineWidth(SELECTION_RECTANGLE_LINE_SIZE / scale);
        gc.strokeRect(visibleLeft, visibleTop, visibleRight - visibleLeft, visibleBottom - visibleTop);
        drawSelectionRectangles(visibleLeft, visibleTop, visibleRight, visibleBottom);
    }

    private void drawSelectionRectangles(final double visibleLeft, final double visibleTop, final double visibleRight, final double visibleBottom) {
        final GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(SELECTION_COLOR);

        // Adjust sizes to account for scale so they remain constant size on screen
        final double scale = Math.max(getScaleX(), 0.1); // Prevent division by zero
        final double scaledRectSize = SELECTION_RECTANGLE_SIZE / scale;
        final double scaledMoveHandleSize = MOVE_HANDLE_SIZE / scale;
        final double scaledMoveHandlePadding = MOVE_HANDLE_PADDING / scale;
        final double scaledRotationHandleSize = ROTATION_HANDLE_SIZE / scale;
        final double scaledRotationHandleOffset = ROTATION_HANDLE_OFFSET / scale;

        // Selection corners (using visible bounds) - all 4 corners are now selection drag rectangles
        // Top-left corner
        gc.fillRect(visibleLeft, visibleTop, scaledRectSize, scaledRectSize);
        // Top-right corner
        gc.fillRect(visibleRight - scaledRectSize, visibleTop, scaledRectSize, scaledRectSize);
        // Bottom-left corner
        gc.fillRect(visibleLeft, visibleBottom - scaledRectSize, scaledRectSize, scaledRectSize);
        // Bottom-right corner
        gc.fillRect(visibleRight - scaledRectSize, visibleBottom - scaledRectSize, scaledRectSize, scaledRectSize);

        // Move handle (centered above visible selection, or inside if too close to top) - 4-way arrow icon
        double moveHandleX = (visibleLeft + visibleRight) / 2 - scaledMoveHandleSize / 2;
        double moveHandleY = visibleTop - scaledMoveHandlePadding - scaledMoveHandleSize;

        final double[] visibleBounds = getVisibleCanvasBounds();
        final double canvasVisibleTop = visibleBounds[1];
        final double canvasVisibleRight = visibleBounds[2];

        // If handle would be off visible canvas, position it inside the selection at the top
        if (moveHandleY < canvasVisibleTop) {
            moveHandleY = visibleTop + scaledMoveHandlePadding;
        }

        drawMoveHandle(gc, moveHandleX, moveHandleY, scaledMoveHandleSize);

        // Rotation handle - positioned to track around the perimeter of the viewport as image rotates
        final double[] handlePos = calculateRotationHandlePosition(
                visibleBounds[0], visibleBounds[1], visibleBounds[2], visibleBounds[3],
                scaledRotationHandleSize, scale);
        drawRotationHandle(gc, handlePos[0], handlePos[1], scaledRotationHandleSize);
    }

    private void handleMousePressed(MouseEvent event) {
        // Event coordinates are already in canvas's local coordinate system (no scale conversion needed)
        final double x = event.getX();
        final double y = event.getY();

        prevX = x;
        prevY = y;
        mousePressX = x;
        mousePressY = y;
        selectedCorner = getSelectedCorner(x, y);

        // Give navigation drag precedence when Ctrl/touch is active
        if ((event.isControlDown() || event.isSynthesized()) && selectedCorner == Corner.MOVE_HANDLE) {
            selectedCorner = Corner.NONE;
        }

        if (selectedCorner == Corner.ROTATE) {
            // Start rotation drag
            double centerX = getWidth() / 2.0;
            double centerY = getHeight() / 2.0;
            rotationDragStartAngle = Math.atan2(y - centerY, x - centerX);
            rotationDragStartCumulative = cumulativeRotation;
            dragging = true;
            rotationDragging = false; // Will be set true on first drag event
            event.consume();
        } else if (selectedCorner != Corner.NONE) {
            dragging = true;
            event.consume();
        } else if (event.isControlDown() || event.isSynthesized()) {
            // Not dragging a handle - initialize navigation drag tracking
            // Only allow if Ctrl is held (mouse) OR event is synthesized (touch)
            navigationDragStartX = x;
            if (onDragStart != null) {
                onDragStart.accept(x);
            }
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (dragging) {
            // Event coordinates are already in canvas's local coordinate system
            double x = event.getX();
            double y = event.getY();
            double dx = x - prevX;
            double dy = y - prevY;

            switch (selectedCorner) {
                case ROTATE:
                    rotationDragging = true; // Enable fast rotation mode

                    // Use incremental rotation: calculate angle change from previous position
                    // This works regardless of absolute mouse position (even outside window)
                    double centerX = getWidth() / 2.0;
                    double centerY = getHeight() / 2.0;
                    double currentAngle = Math.atan2(y - centerY, x - centerX);
                    double previousAngle = Math.atan2(prevY - centerY, prevX - centerX);

                    // Calculate the shortest angular distance (handles wrapping at ±π)
                    double angleDelta = currentAngle - previousAngle;
                    if (angleDelta > Math.PI) {
                        angleDelta -= 2 * Math.PI;
                    } else if (angleDelta < -Math.PI) {
                        angleDelta += 2 * Math.PI;
                    }

                    // Add incremental rotation to cumulative total
                    cumulativeRotation += Math.toDegrees(angleDelta);

                    // Update immediately for smooth rotation (no throttling)
                    applyRotation();
                    break;
                case TOP_LEFT:
                    selectionTopLeftX += dx;
                    selectionTopLeftY += dy;
                    break;
                case TOP_RIGHT:
                    selectionBottomRightX += dx;
                    selectionTopLeftY += dy;
                    break;
                case BOTTOM_LEFT:
                    selectionTopLeftX += dx;
                    selectionBottomRightY += dy;
                    break;
                case BOTTOM_RIGHT:
                    selectionBottomRightX += dx;
                    selectionBottomRightY += dy;
                    break;
                case MOVE_HANDLE:
                    // Calculate selection width/height before moving
                    final double selectionWidth = selectionBottomRightX - selectionTopLeftX;
                    final double selectionHeight = selectionBottomRightY - selectionTopLeftY;

                    // Calculate new position
                    double newLeft = selectionTopLeftX + dx;
                    double newTop = selectionTopLeftY + dy;

                    // Clamp to image bounds
                    newLeft = Math.max(0, Math.min(newLeft, image.getWidth() - selectionWidth));
                    newTop = Math.max(0, Math.min(newTop, image.getHeight() - selectionHeight));

                    // Apply the clamped position
                    selectionTopLeftX = newLeft;
                    selectionTopLeftY = newTop;
                    selectionBottomRightX = newLeft + selectionWidth;
                    selectionBottomRightY = newTop + selectionHeight;
                    break;
                case NONE:
                    break;
            }

            if (selectedCorner != Corner.ROTATE && selectedCorner != Corner.MOVE_HANDLE) {
                // Normalize corners for corner dragging (swap if they cross each other)
                // Skip this for MOVE_HANDLE since it moves both corners together
                selectionTopLeftX = Math.min(selectionTopLeftX, selectionBottomRightX);
                selectionTopLeftY = Math.min(selectionTopLeftY, selectionBottomRightY);
                selectionBottomRightX = Math.max(selectionTopLeftX, selectionBottomRightX);
                selectionBottomRightY = Math.max(selectionTopLeftY, selectionBottomRightY);
                redraw();
            } else if (selectedCorner == Corner.MOVE_HANDLE) {
                // Just redraw for move handle (no normalization needed)
                redraw();
            }

            prevX = x;
            prevY = y;
            event.consume();
        } else if (onNavigationDrag != null && (event.isControlDown() || event.isSynthesized())) {
            // Not dragging a handle - trigger navigation
            // Only allow if Ctrl is held (mouse) OR event is synthesized (touch)
            // Convert from screen coordinates to canvas coordinates
            final double scale = getScaleX();
            onNavigationDrag.accept(event.getX() / scale);
        }
    }

    private void handleMouseReleased(MouseEvent event) {
        // Event coordinates are already in canvas's local coordinate system
        final double x = event.getX();
        final double y = event.getY();

        if (selectedCorner == Corner.ROTATE) {
            // Calculate distance moved to determine if it's a click or drag
            final double dx = x - mousePressX;
            final double dy = y - mousePressY;
            final double distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < CLICK_THRESHOLD) {
                // Single click - rotate by increment
                rotateImage(ROTATION_INCREMENT);
                if (onRotationComplete != null) {
                    onRotationComplete.accept(ROTATION_INCREMENT);
                }
            } else if (dragging && rotationDragging) {
                // Rotation drag complete - apply final high-quality rotation
                rotationDragging = false;
                applyRotation(); // Re-apply with high quality

                // Calculate total rotation from drag start
                final double totalRotation = cumulativeRotation - rotationDragStartCumulative;

                // Notify Window to save (even small rotations, since they're intentional drags)
                if (onRotationComplete != null && Math.abs(totalRotation) > 0.01) {
                    onRotationComplete.accept(totalRotation);
                }
            }
        }
        dragging = false;
        rotationDragging = false;
    }

    private void applyRotation() {
        double radians = Math.toRadians(cumulativeRotation);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

        int newWidth = (int) Math.floor(originalWidth * cos + originalHeight * sin);
        int newHeight = (int) Math.floor(originalHeight * cos + originalWidth * sin);

        BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = rotatedImage.createGraphics();

        // Use fast rendering during drag, high-quality after
        if (rotationDragging) {
            // Fastest rendering for interactive performance - nearest neighbor is much faster than bilinear
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                java.awt.RenderingHints.VALUE_RENDER_SPEED);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                java.awt.RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ALPHA_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        } else {
            // High-quality rendering for final result
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                                java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        }

        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, newWidth, newHeight);

        AffineTransform transform = new AffineTransform();
        transform.translate((newWidth - originalWidth) / 2.0, (newHeight - originalHeight) / 2.0);
        transform.rotate(radians, originalWidth / 2.0, originalHeight / 2.0);
        g2d.drawImage(originalImage, transform, null);
        g2d.dispose();

        this.setWidth(newWidth);
        this.setHeight(newHeight);
        this.image = rotatedImage;

        selectionTopLeftX = 0;
        selectionTopLeftY = 0;
        selectionBottomRightX = newWidth;
        selectionBottomRightY = newHeight;

        redraw();
    }

    private Corner getSelectedCorner(double x, double y) {
        // Mouse coordinates are in canvas space, and we draw handles with scaled sizes,
        // so we need to use scaled sizes for hit detection too
        final double scale = Math.max(getScaleX(), 0.1); // Prevent division by zero
        final double scaledRectSize = SELECTION_RECTANGLE_SIZE / scale;
        final double scaledMoveHandleSize = MOVE_HANDLE_SIZE / scale;
        final double scaledMoveHandlePadding = MOVE_HANDLE_PADDING / scale;
        final double scaledRotationHandleSize = ROTATION_HANDLE_SIZE / scale;
        final double scaledRotationHandleOffset = ROTATION_HANDLE_OFFSET / scale;

        // Get visible bounds for handle positioning (handles are drawn at visible positions)
        final double[] visibleSelection = getVisibleSelection();
        final double visibleLeft = visibleSelection[0];
        final double visibleTop = visibleSelection[1];
        final double visibleRight = visibleSelection[2];
        final double visibleBottom = visibleSelection[3];

        final double[] visibleBounds = getVisibleCanvasBounds();
        final double canvasVisibleLeft = visibleBounds[0];
        final double canvasVisibleTop = visibleBounds[1];
        final double canvasVisibleRight = visibleBounds[2];
        final double canvasVisibleBottom = visibleBounds[3];

        // Check for rotation handle first - positioned around viewport perimeter
        final double[] handlePos = calculateRotationHandlePosition(
                canvasVisibleLeft, canvasVisibleTop, canvasVisibleRight, canvasVisibleBottom,
                scaledRotationHandleSize, scale);
        final double rotationHandleX = handlePos[0];
        final double rotationHandleY = handlePos[1];
        if (isInsideRectangle(x, y, rotationHandleX, rotationHandleY, scaledRotationHandleSize, scaledRotationHandleSize)) {
            return Corner.ROTATE;
        }

        // Calculate move handle position (centered above visible selection, or inside if too close to top)
        moveHandleX = (visibleLeft + visibleRight) / 2 - scaledMoveHandleSize / 2;
        moveHandleY = visibleTop - scaledMoveHandlePadding - scaledMoveHandleSize;

        // If handle would be off visible canvas, position it inside the selection at the top
        if (moveHandleY < canvasVisibleTop) {
            moveHandleY = visibleTop + scaledMoveHandlePadding;
        }

        // Check for move handle
        if (isInsideRectangle(x, y, moveHandleX, moveHandleY, scaledMoveHandleSize, scaledMoveHandleSize)) {
            return Corner.MOVE_HANDLE;
        }

        // Then check selection corners (using visible bounds) - all 4 corners are now selection drag rectangles
        if (isInsideRectangle(x, y, visibleLeft, visibleTop, scaledRectSize, scaledRectSize)) {
            return Corner.TOP_LEFT;
        } else if (isInsideRectangle(x, y, visibleRight - scaledRectSize, visibleTop, scaledRectSize, scaledRectSize)) {
            return Corner.TOP_RIGHT;
        } else if (isInsideRectangle(x, y, visibleLeft, visibleBottom - scaledRectSize, scaledRectSize, scaledRectSize)) {
            return Corner.BOTTOM_LEFT;
        } else if (isInsideRectangle(x, y, visibleRight - scaledRectSize, visibleBottom - scaledRectSize, scaledRectSize, scaledRectSize)) {
            return Corner.BOTTOM_RIGHT;
        } else {
            // Removed interior check - clicking inside selection rectangle doesn't move it
            // Only the move handle can move the selection
            return Corner.NONE;
        }
    }

    private boolean isInsideRectangle(double x, double y, double rectX, double rectY, double rectWidth, double rectHeight) {
        return x >= rectX && x <= rectX + rectWidth && y >= rectY && y <= rectY + rectHeight;
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        java.awt.Graphics2D g = copy.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return copy;
    }

    private double[] calculateRotationHandlePosition(final double visibleLeft, final double visibleTop,
                                                      final double visibleRight, final double visibleBottom,
                                                      final double handleSize, final double scale) {
        // Position handle around the perimeter of the viewport as the image rotates
        // Strategy: 0° → top-right, 90° → bottom-right, 180° → bottom-left, 270° → top-left

        // Normalize rotation to 0-360 range
        double normalizedRotation = cumulativeRotation % 360;
        if (normalizedRotation < 0) {
            normalizedRotation += 360;
        }

        // Ensure entire handle is visible - account for full handle size plus margin
        final double margin = 15 / scale;  // Increased margin to keep handle well within bounds
        final double inset = handleSize + margin; // Full handle size plus margin from edge
        double handleX, handleY;

        if (normalizedRotation >= 0 && normalizedRotation < 90) {
            // First quadrant: Move from top-right down the right edge
            final double progress = normalizedRotation / 90.0;
            handleX = visibleRight - inset;
            handleY = visibleTop + margin + progress * (visibleBottom - visibleTop - handleSize - 2 * margin);
        } else if (normalizedRotation >= 90 && normalizedRotation < 180) {
            // Second quadrant: Move from bottom-right to bottom-left along bottom edge
            final double progress = (normalizedRotation - 90) / 90.0;
            handleX = visibleRight - inset - progress * (visibleRight - visibleLeft - handleSize - 2 * margin);
            handleY = visibleBottom - inset;
        } else if (normalizedRotation >= 180 && normalizedRotation < 270) {
            // Third quadrant: Move from bottom-left up the left edge
            final double progress = (normalizedRotation - 180) / 90.0;
            handleX = visibleLeft + margin;
            handleY = visibleBottom - inset - progress * (visibleBottom - visibleTop - handleSize - 2 * margin);
        } else {
            // Fourth quadrant: Move from top-left to top-right along top edge
            final double progress = (normalizedRotation - 270) / 90.0;
            handleX = visibleLeft + margin + progress * (visibleRight - visibleLeft - handleSize - 2 * margin);
            handleY = visibleTop + margin;
        }

        return new double[]{handleX, handleY};
    }

    private void drawRotationHandle(final GraphicsContext gc, final double x, final double y, final double size) {
        // Draw a clockwise circular arrow icon
        gc.setFill(ROTATION_HANDLE_COLOR);
        gc.setStroke(ROTATION_HANDLE_COLOR);
        final double scale = Math.max(getScaleX(), 0.1);
        gc.setLineWidth(3 / scale);

        final double centerX = x + size / 2;
        final double centerY = y + size / 2;
        final double radius = size * 0.35;

        // Draw circular arc (270 degrees starting from top, going clockwise)
        gc.strokeArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 90, 270, javafx.scene.shape.ArcType.OPEN);

        // Draw arrowhead at the end of the arc (pointing clockwise at top)
        final double arrowX = centerX;
        final double arrowY = centerY - radius;
        final double arrowSize = size * 0.25;

        // Arrow pointing right (clockwise direction at top of circle)
        gc.fillPolygon(
                new double[]{arrowX, arrowX + arrowSize, arrowX},
                new double[]{arrowY - arrowSize * 0.5, arrowY, arrowY + arrowSize * 0.5},
                3
        );
    }

    private void drawMoveHandle(final GraphicsContext gc, final double x, final double y, final double size) {
        // Draw a 4-way arrow icon (cross with arrows)
        gc.setFill(MOVE_HANDLE_COLOR);
        gc.setStroke(MOVE_HANDLE_COLOR);
        final double scale = Math.max(getScaleX(), 0.1);
        gc.setLineWidth(3 / scale);

        final double centerX = x + size / 2;
        final double centerY = y + size / 2;
        final double armLength = size * 0.35;
        final double arrowSize = size * 0.2;

        // Draw cross lines
        gc.strokeLine(centerX - armLength, centerY, centerX + armLength, centerY); // Horizontal
        gc.strokeLine(centerX, centerY - armLength, centerX, centerY + armLength); // Vertical

        // Draw arrowheads at the end of each arm
        // Right arrow
        gc.fillPolygon(
                new double[]{centerX + armLength, centerX + armLength - arrowSize, centerX + armLength - arrowSize},
                new double[]{centerY, centerY - arrowSize * 0.6, centerY + arrowSize * 0.6},
                3
        );
        // Left arrow
        gc.fillPolygon(
                new double[]{centerX - armLength, centerX - armLength + arrowSize, centerX - armLength + arrowSize},
                new double[]{centerY, centerY - arrowSize * 0.6, centerY + arrowSize * 0.6},
                3
        );
        // Up arrow
        gc.fillPolygon(
                new double[]{centerX, centerX - arrowSize * 0.6, centerX + arrowSize * 0.6},
                new double[]{centerY - armLength, centerY - armLength + arrowSize, centerY - armLength + arrowSize},
                3
        );
        // Down arrow
        gc.fillPolygon(
                new double[]{centerX, centerX - arrowSize * 0.6, centerX + arrowSize * 0.6},
                new double[]{centerY + armLength, centerY + armLength - arrowSize, centerY + armLength - arrowSize},
                3
        );
    }

    public void setOnRotationComplete(java.util.function.Consumer<Double> callback) {
        this.onRotationComplete = callback;
    }

    public void setOnDragStart(java.util.function.Consumer<Double> callback) {
        this.onDragStart = callback;
    }

    public void setOnNavigationDrag(java.util.function.Consumer<Double> callback) {
        this.onNavigationDrag = callback;
    }

    public void setOnSelectionChange(Runnable callback) {
        this.onSelectionChange = callback;
    }

    public BufferedImage getCurrentImage() {
        return image;
    }

    public int getImageWidth() {
        return image.getWidth();
    }

    public int getImageHeight() {
        return image.getHeight();
    }

    public int getSelectionWidth() {
        return (int) Math.round(selectionBottomRightX - selectionTopLeftX);
    }

    public int getSelectionHeight() {
        return (int) Math.round(selectionBottomRightY - selectionTopLeftY);
    }

    public double getSelectionLeft() {
        return selectionTopLeftX;
    }

    public double getSelectionTop() {
        return selectionTopLeftY;
    }

    public int getVisibleSelectionWidth() {
        final double[] visibleSelection = getVisibleSelection();
        return (int) Math.round(visibleSelection[2] - visibleSelection[0]);
    }

    public int getVisibleSelectionHeight() {
        final double[] visibleSelection = getVisibleSelection();
        return (int) Math.round(visibleSelection[3] - visibleSelection[1]);
    }

    // For testing - returns rotation handle bounds [x, y, width, height]
    public double[] getRotationHandleBounds() {
        final double scale = Math.max(getScaleX(), 0.1);
        final double scaledRotationHandleSize = ROTATION_HANDLE_SIZE / scale;

        final double[] visibleBounds = getVisibleCanvasBounds();
        final double[] handlePos = calculateRotationHandlePosition(
                visibleBounds[0], visibleBounds[1], visibleBounds[2], visibleBounds[3],
                scaledRotationHandleSize, scale);

        return new double[]{handlePos[0], handlePos[1], scaledRotationHandleSize, scaledRotationHandleSize};
    }

    // For testing - returns move handle bounds [x, y, width, height]
    public double[] getMoveHandleBounds() {
        final double scale = Math.max(getScaleX(), 0.1);
        final double scaledMoveHandleSize = MOVE_HANDLE_SIZE / scale;
        final double scaledMoveHandlePadding = MOVE_HANDLE_PADDING / scale;

        final double[] visibleSelection = getVisibleSelection();
        final double visibleLeft = visibleSelection[0];
        final double visibleTop = visibleSelection[1];
        final double visibleRight = visibleSelection[2];

        final double[] visibleBounds = getVisibleCanvasBounds();
        final double canvasVisibleTop = visibleBounds[1];

        double moveHandleX = (visibleLeft + visibleRight) / 2 - scaledMoveHandleSize / 2;
        double moveHandleY = visibleTop - scaledMoveHandlePadding - scaledMoveHandleSize;

        // If handle would be off visible canvas, position it inside the selection at the top
        if (moveHandleY < canvasVisibleTop) {
            moveHandleY = visibleTop + scaledMoveHandlePadding;
        }

        return new double[]{moveHandleX, moveHandleY, scaledMoveHandleSize, scaledMoveHandleSize};
    }

    private double[] getVisibleCanvasBounds() {
        final javafx.scene.Scene scene = getScene();
        final double canvasWidth = getWidth();
        final double canvasHeight = getHeight();

        if (scene == null || scene.getWidth() <= 0 || scene.getHeight() <= 0) {
            return new double[]{0, 0, canvasWidth, canvasHeight};
        }

        final double sceneWidth = scene.getWidth();
        final double sceneHeight = scene.getHeight();
        final double scale = getScaleX();

        final double visibleCanvasWidth = Math.min(sceneWidth / scale, canvasWidth);
        final double visibleCanvasHeight = Math.min(sceneHeight / scale, canvasHeight);

        final double visibleLeft = (canvasWidth - visibleCanvasWidth) / 2.0;
        final double visibleTop = (canvasHeight - visibleCanvasHeight) / 2.0;
        final double visibleRight = visibleLeft + visibleCanvasWidth;
        final double visibleBottom = visibleTop + visibleCanvasHeight;

        return new double[]{visibleLeft, visibleTop, visibleRight, visibleBottom};
    }

    private double[] getVisibleSelection() {
        final double[] visibleBounds = getVisibleCanvasBounds();
        final double visibleLeft = visibleBounds[0];
        final double visibleTop = visibleBounds[1];
        final double visibleRight = visibleBounds[2];
        final double visibleBottom = visibleBounds[3];

        final double clampedLeft = Math.max(selectionTopLeftX, visibleLeft);
        final double clampedTop = Math.max(selectionTopLeftY, visibleTop);
        final double clampedRight = Math.min(selectionBottomRightX, visibleRight);
        final double clampedBottom = Math.min(selectionBottomRightY, visibleBottom);

        return new double[]{clampedLeft, clampedTop, clampedRight, clampedBottom};
    }

    private enum Corner {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE_HANDLE, ROTATE, NONE
    }
}

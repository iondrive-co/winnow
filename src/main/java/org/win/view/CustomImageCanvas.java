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
    private static final Color MOVE_HANDLE_COLOR = Color.rgb(0, 200, 0, 0.5); // Semi-transparent green

    private BufferedImage image;
    private BufferedImage originalImage;  // Store unrotated original for quality preservation
    private double selectionTopLeftX, selectionTopLeftY, selectionBottomRightX, selectionBottomRightY;
    private double moveHandleX, moveHandleY;  // Position of move handle
    private double prevX, prevY;
    private boolean dragging;
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

        // Selection corners (using visible bounds)
        // Top-left corner
        gc.fillRect(visibleLeft, visibleTop, scaledRectSize, scaledRectSize);
        // Top-right corner
        gc.fillRect(visibleRight - scaledRectSize, visibleTop, scaledRectSize, scaledRectSize);
        // Bottom-left corner
        gc.fillRect(visibleLeft, visibleBottom - scaledRectSize, scaledRectSize, scaledRectSize);
        // Bottom-right corner
        gc.fillRect(visibleRight - scaledRectSize, visibleBottom - scaledRectSize, scaledRectSize, scaledRectSize);

        // Move handle (centered above visible selection, or inside if too close to top) - semi-transparent green
        double moveHandleX = (visibleLeft + visibleRight) / 2 - scaledMoveHandleSize / 2;
        double moveHandleY = visibleTop - scaledMoveHandlePadding - scaledMoveHandleSize;

        final double[] visibleBounds = getVisibleCanvasBounds();
        final double canvasVisibleTop = visibleBounds[1];

        // If handle would be off visible canvas, position it inside the selection at the top
        if (moveHandleY < canvasVisibleTop) {
            moveHandleY = visibleTop + scaledMoveHandlePadding;
        }

        gc.setFill(MOVE_HANDLE_COLOR);
        gc.fillRect(moveHandleX, moveHandleY, scaledMoveHandleSize, scaledMoveHandleSize);

        // Rotation handle (top-right corner of visible canvas) - use different color
        final double canvasVisibleRight = visibleBounds[2];
        gc.setFill(Color.RED);
        gc.fillRect(canvasVisibleRight - scaledRectSize, canvasVisibleTop, scaledRectSize, scaledRectSize);
    }

    private void handleMousePressed(MouseEvent event) {
        // Convert from screen coordinates to canvas coordinates
        final double scale = getScaleX();
        double x = event.getX() / scale;
        double y = event.getY() / scale;
        prevX = x;
        prevY = y;
        selectedCorner = getSelectedCorner(x, y);

        if (selectedCorner == Corner.ROTATE) {
            // Start rotation drag
            double centerX = getWidth() / 2.0;
            double centerY = getHeight() / 2.0;
            rotationDragStartAngle = Math.atan2(y - centerY, x - centerX);
            rotationDragStartCumulative = cumulativeRotation;
            dragging = true;
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
            // Convert from screen coordinates to canvas coordinates
            final double scale = getScaleX();
            double x = event.getX() / scale;
            double y = event.getY() / scale;
            double dx = x - prevX;
            double dy = y - prevY;

            switch (selectedCorner) {
                case ROTATE:
                    double centerX = getWidth() / 2.0;
                    double centerY = getHeight() / 2.0;
                    double currentAngle = Math.atan2(y - centerY, x - centerX);
                    double angleDelta = currentAngle - rotationDragStartAngle;
                    cumulativeRotation = rotationDragStartCumulative + Math.toDegrees(angleDelta);
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
                    selectionTopLeftX += dx;
                    selectionTopLeftY += dy;
                    selectionBottomRightX += dx;
                    selectionBottomRightY += dy;
                    break;
                case NONE:
                    break;
            }

            if (selectedCorner != Corner.ROTATE) {
                selectionTopLeftX = Math.min(selectionTopLeftX, selectionBottomRightX);
                selectionTopLeftY = Math.min(selectionTopLeftY, selectionBottomRightY);
                selectionBottomRightX = Math.max(selectionTopLeftX, selectionBottomRightX);
                selectionBottomRightY = Math.max(selectionTopLeftY, selectionBottomRightY);
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
        if (dragging && selectedCorner == Corner.ROTATE) {
            // Rotation drag complete - notify Window to save
            double totalRotation = cumulativeRotation - rotationDragStartCumulative;
            if (onRotationComplete != null && Math.abs(totalRotation) > 0.1) {
                onRotationComplete.accept(totalRotation);
            }
        }
        dragging = false;
    }

    private void applyRotation() {
        double radians = Math.toRadians(cumulativeRotation);
        double sin = Math.abs(Math.sin(radians));
        double cos = Math.abs(Math.cos(radians));

        int newWidth = (int) Math.floor(originalWidth * cos + originalHeight * sin);
        int newHeight = (int) Math.floor(originalHeight * cos + originalWidth * sin);

        BufferedImage rotatedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = rotatedImage.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                            java.awt.RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

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

        // Check for rotation handle first (top-right corner of canvas)
        final double canvasWidth = getWidth();
        final double canvasHeight = getHeight();
        if (isInsideRectangle(x, y, canvasWidth - scaledRectSize, 0, scaledRectSize, scaledRectSize)) {
            return Corner.ROTATE;
        }

        // Calculate move handle position (centered above selection, or inside if too close to top)
        moveHandleX = (selectionTopLeftX + selectionBottomRightX) / 2 - scaledMoveHandleSize / 2;
        moveHandleY = selectionTopLeftY - scaledMoveHandlePadding - scaledMoveHandleSize;

        // If handle would be off-screen, position it inside the selection at the top
        if (moveHandleY < 0) {
            moveHandleY = selectionTopLeftY + scaledMoveHandlePadding;
        }

        // Check for move handle
        if (isInsideRectangle(x, y, moveHandleX, moveHandleY, scaledMoveHandleSize, scaledMoveHandleSize)) {
            return Corner.MOVE_HANDLE;
        }

        // Then check selection corners
        if (isInsideRectangle(x, y, selectionTopLeftX, selectionTopLeftY, scaledRectSize, scaledRectSize)) {
            return Corner.TOP_LEFT;
        } else if (isInsideRectangle(x, y, selectionBottomRightX - scaledRectSize, selectionTopLeftY, scaledRectSize, scaledRectSize)) {
            return Corner.TOP_RIGHT;
        } else if (isInsideRectangle(x, y, selectionTopLeftX, selectionBottomRightY - scaledRectSize, scaledRectSize, scaledRectSize)) {
            return Corner.BOTTOM_LEFT;
        } else if (isInsideRectangle(x, y, selectionBottomRightX - scaledRectSize, selectionBottomRightY - scaledRectSize, scaledRectSize, scaledRectSize)) {
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

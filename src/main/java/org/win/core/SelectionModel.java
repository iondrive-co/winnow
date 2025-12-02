package org.win.core;

/**
 * Represents a rectangular selection region on an image.
 * Pure data model with no UI dependencies - compatible with WebFX for browser deployment.
 */
public final class SelectionModel {

    private double topLeftX;
    private double topLeftY;
    private double bottomRightX;
    private double bottomRightY;
    private int imageWidth;
    private int imageHeight;
    private Runnable onSelectionChange;

    public SelectionModel(final int imageWidth, final int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        resetToFullImage();
    }

    /**
     * Resets selection to cover the entire image.
     */
    public void resetToFullImage() {
        this.topLeftX = 0;
        this.topLeftY = 0;
        this.bottomRightX = imageWidth;
        this.bottomRightY = imageHeight;
        notifyChange();
    }

    /**
     * Updates the image dimensions and optionally resets selection.
     */
    public void setImageDimensions(final int width, final int height, final boolean resetSelection) {
        this.imageWidth = width;
        this.imageHeight = height;
        if (resetSelection) {
            resetToFullImage();
        } else {
            // Clamp selection to new bounds
            clampToBounds();
        }
    }

    /**
     * Sets the selection region explicitly.
     */
    public void setRegion(final double topLeftX, final double topLeftY,
                          final double bottomRightX, final double bottomRightY) {
        this.topLeftX = topLeftX;
        this.topLeftY = topLeftY;
        this.bottomRightX = bottomRightX;
        this.bottomRightY = bottomRightY;
        normalize();
        clampToBounds();
        notifyChange();
    }

    /**
     * Moves the selection by the given delta, keeping the same dimensions.
     */
    public void move(final double dx, final double dy) {
        final double width = getWidth();
        final double height = getHeight();

        double newLeft = topLeftX + dx;
        double newTop = topLeftY + dy;

        // Clamp to image bounds
        newLeft = Math.max(0, Math.min(newLeft, imageWidth - width));
        newTop = Math.max(0, Math.min(newTop, imageHeight - height));

        topLeftX = newLeft;
        topLeftY = newTop;
        bottomRightX = newLeft + width;
        bottomRightY = newTop + height;
        notifyChange();
    }

    // --- Right and Bottom edge manipulation ---

    public void expandRight(final int pixels) {
        bottomRightX = Math.min(bottomRightX + pixels, imageWidth);
        notifyChange();
    }

    public void reduceRight(final int pixels) {
        bottomRightX = Math.max(bottomRightX - pixels, topLeftX + 1);
        notifyChange();
    }

    public void expandBottom(final int pixels) {
        bottomRightY = Math.min(bottomRightY + pixels, imageHeight);
        notifyChange();
    }

    public void reduceBottom(final int pixels) {
        bottomRightY = Math.max(bottomRightY - pixels, topLeftY + 1);
        notifyChange();
    }

    // --- Left and Top edge manipulation ---

    public void expandLeft(final int pixels) {
        topLeftX = Math.max(topLeftX - pixels, 0);
        notifyChange();
    }

    public void reduceLeft(final int pixels) {
        topLeftX = Math.min(topLeftX + pixels, bottomRightX - 1);
        notifyChange();
    }

    public void expandTop(final int pixels) {
        topLeftY = Math.max(topLeftY - pixels, 0);
        notifyChange();
    }

    public void reduceTop(final int pixels) {
        topLeftY = Math.min(topLeftY + pixels, bottomRightY - 1);
        notifyChange();
    }

    // --- Corner dragging ---

    public void dragTopLeft(final double dx, final double dy) {
        topLeftX += dx;
        topLeftY += dy;
        normalize();
        clampToBounds();
        notifyChange();
    }

    public void dragTopRight(final double dx, final double dy) {
        bottomRightX += dx;
        topLeftY += dy;
        normalize();
        clampToBounds();
        notifyChange();
    }

    public void dragBottomLeft(final double dx, final double dy) {
        topLeftX += dx;
        bottomRightY += dy;
        normalize();
        clampToBounds();
        notifyChange();
    }

    public void dragBottomRight(final double dx, final double dy) {
        bottomRightX += dx;
        bottomRightY += dy;
        normalize();
        clampToBounds();
        notifyChange();
    }

    // --- Getters ---

    public double getTopLeftX() {
        return topLeftX;
    }

    public double getTopLeftY() {
        return topLeftY;
    }

    public double getBottomRightX() {
        return bottomRightX;
    }

    public double getBottomRightY() {
        return bottomRightY;
    }

    public double getWidth() {
        return bottomRightX - topLeftX;
    }

    public double getHeight() {
        return bottomRightY - topLeftY;
    }

    public int getWidthInt() {
        return (int) Math.round(getWidth());
    }

    public int getHeightInt() {
        return (int) Math.round(getHeight());
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * Returns selection bounds clamped to a visible viewport.
     *
     * @param viewLeft   Left edge of visible area in image coordinates
     * @param viewTop    Top edge of visible area in image coordinates
     * @param viewRight  Right edge of visible area in image coordinates
     * @param viewBottom Bottom edge of visible area in image coordinates
     * @return Array of [clampedLeft, clampedTop, clampedRight, clampedBottom]
     */
    public double[] getVisibleBounds(final double viewLeft, final double viewTop,
                                     final double viewRight, final double viewBottom) {
        return new double[]{
            Math.max(topLeftX, viewLeft),
            Math.max(topLeftY, viewTop),
            Math.min(bottomRightX, viewRight),
            Math.min(bottomRightY, viewBottom)
        };
    }

    /**
     * Sets a callback to be invoked when selection changes.
     */
    public void setOnSelectionChange(final Runnable callback) {
        this.onSelectionChange = callback;
    }

    // --- Private helpers ---

    /**
     * Ensures top-left is actually top-left (handles inverted selections from dragging).
     */
    private void normalize() {
        if (topLeftX > bottomRightX) {
            final double temp = topLeftX;
            topLeftX = bottomRightX;
            bottomRightX = temp;
        }
        if (topLeftY > bottomRightY) {
            final double temp = topLeftY;
            topLeftY = bottomRightY;
            bottomRightY = temp;
        }
    }

    /**
     * Clamps selection to image bounds while maintaining minimum size.
     */
    private void clampToBounds() {
        topLeftX = Math.max(0, topLeftX);
        topLeftY = Math.max(0, topLeftY);
        bottomRightX = Math.min(imageWidth, bottomRightX);
        bottomRightY = Math.min(imageHeight, bottomRightY);

        // Ensure minimum size of 1x1
        if (bottomRightX <= topLeftX) {
            bottomRightX = topLeftX + 1;
        }
        if (bottomRightY <= topLeftY) {
            bottomRightY = topLeftY + 1;
        }
    }

    private void notifyChange() {
        if (onSelectionChange != null) {
            onSelectionChange.run();
        }
    }
}

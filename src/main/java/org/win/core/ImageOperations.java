package org.win.core;

import javafx.scene.image.WritableImage;


/**
 * Pure JavaFX image manipulation operations using WritableImage and PixelReader/PixelWriter.
 * No AWT/Swing dependencies - compatible with WebFX for browser deployment.
 */
public final class ImageOperations {

    private ImageOperations() {
        // Utility class
    }

    /**
     * Creates a deep copy of the source image.
     */
    public static WritableImage copy(final WritableImage source) {
        return toWritableImage(PixelImageOps.copy(toPixelImage(source)));
    }

    /**
     * Crops a rectangular region from the source image.
     */
    public static WritableImage crop(final WritableImage source,
                                     final int cropX, final int cropY,
                                     final int cropWidth, final int cropHeight) {
        final PixelImage cropped = PixelImageOps.crop(toPixelImage(source), cropX, cropY, cropWidth, cropHeight);
        return toWritableImage(cropped);
    }

    /**
     * Rotates the image by the specified degrees around its center.
     * Uses bilinear interpolation for high-quality output.
     *
     * @param source  The source image
     * @param degrees Rotation angle in degrees (positive = clockwise)
     * @return New rotated image with expanded dimensions to fit the rotated content
     */
    public static WritableImage rotate(final WritableImage source, final double degrees) {
        return toWritableImage(PixelImageOps.rotate(toPixelImage(source), degrees));
    }

    /**
     * Fast rotation using nearest-neighbor interpolation.
     * Use during interactive dragging for better performance.
     */
    public static WritableImage rotateFast(final WritableImage source, final double degrees) {
        return toWritableImage(PixelImageOps.rotateFast(toPixelImage(source), degrees));
    }

    /**
     * Resizes the image to new dimensions using bilinear interpolation.
     */
    public static WritableImage resize(final WritableImage source,
                                       final int newWidth, final int newHeight) {
        return toWritableImage(PixelImageOps.resize(toPixelImage(source), newWidth, newHeight));
    }

    /**
     * Converts a JavaFX Image to WritableImage.
     * If the source is already a WritableImage, returns a copy.
     */
    public static WritableImage toWritableImage(final javafx.scene.image.Image source) {
        final PixelImage pixelImage = toPixelImage(source);
        return toWritableImage(pixelImage);
    }

    public static WritableImage toWritableImage(final PixelImage image) {
        final WritableImage writableImage = new WritableImage(image.getWidth(), image.getHeight());
        final javafx.scene.image.PixelWriter writer = writableImage.getPixelWriter();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                writer.setArgb(x, y, image.getArgb(x, y));
            }
        }
        return writableImage;
    }

    /**
     * Converts a JavaFX Image to a PixelImage.
     */
    public static PixelImage toPixelImage(final javafx.scene.image.Image source) {
        final int width = (int) source.getWidth();
        final int height = (int) source.getHeight();
        final PixelImage result = new PixelImage(width, height);
        final javafx.scene.image.PixelReader reader = source.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setArgb(x, y, reader.getArgb(x, y));
            }
        }
        return result;
    }
}

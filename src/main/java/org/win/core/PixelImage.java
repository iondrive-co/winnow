package org.win.core;

/**
 * Platform-neutral ARGB image representation for shared algorithms (browser + desktop).
 */
public final class PixelImage {
    private final int width;
    private final int height;
    private final int[] pixels; // ARGB packed

    public PixelImage(final int width, final int height) {
        this(width, height, new int[width * height]);
    }

    public PixelImage(final int width, final int height, final int[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int[] getPixels() {
        return pixels;
    }

    public int getArgb(final int x, final int y) {
        return pixels[y * width + x];
    }

    public void setArgb(final int x, final int y, final int argb) {
        pixels[y * width + x] = argb;
    }

    public PixelImage copy() {
        final int[] newPixels = new int[pixels.length];
        System.arraycopy(pixels, 0, newPixels, 0, pixels.length);
        return new PixelImage(width, height, newPixels);
    }
}

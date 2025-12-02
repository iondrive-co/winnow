package org.win.core;

/**
 * Platform-neutral image operations using {@link PixelImage}.
 */
public final class PixelImageOps {

    private PixelImageOps() {}

    public static PixelImage copy(final PixelImage source) {
        return source.copy();
    }

    public static PixelImage crop(final PixelImage source,
                                  final int cropX, final int cropY,
                                  final int cropWidth, final int cropHeight) {
        final int sourceWidth = source.getWidth();
        final int sourceHeight = source.getHeight();

        final int x = Math.max(0, Math.min(cropX, sourceWidth - 1));
        final int y = Math.max(0, Math.min(cropY, sourceHeight - 1));
        final int w = Math.max(1, Math.min(cropWidth, sourceWidth - x));
        final int h = Math.max(1, Math.min(cropHeight, sourceHeight - y));

        final PixelImage result = new PixelImage(w, h);
        for (int dy = 0; dy < h; dy++) {
            final int srcY = y + dy;
            for (int dx = 0; dx < w; dx++) {
                result.setArgb(dx, dy, source.getArgb(x + dx, srcY));
            }
        }
        return result;
    }

    public static PixelImage rotate(final PixelImage source, final double degrees) {
        return rotateInternal(source, degrees, true);
    }

    public static PixelImage rotateFast(final PixelImage source, final double degrees) {
        return rotateInternal(source, degrees, false);
    }

    public static PixelImage resize(final PixelImage source, final int newWidth, final int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("Dimensions must be positive");
        }

        final PixelImage result = new PixelImage(newWidth, newHeight);

        final double xRatio = (double) source.getWidth() / newWidth;
        final double yRatio = (double) source.getHeight() / newHeight;

        for (int dstY = 0; dstY < newHeight; dstY++) {
            final double srcY = dstY * yRatio;
            for (int dstX = 0; dstX < newWidth; dstX++) {
                final double srcX = dstX * xRatio;
                final int argb = bilinearInterpolate(source, srcX, srcY);
                result.setArgb(dstX, dstY, argb);
            }
        }
        return result;
    }

    private static PixelImage rotateInternal(final PixelImage source, final double degrees, final boolean bilinear) {
        final int srcWidth = source.getWidth();
        final int srcHeight = source.getHeight();

        final double radians = Math.toRadians(degrees);
        final double cos = Math.cos(radians);
        final double sin = Math.sin(radians);
        final double absCos = Math.abs(cos);
        final double absSin = Math.abs(sin);

        final int newWidth = Math.max(1, (int) Math.round(srcWidth * absCos + srcHeight * absSin));
        final int newHeight = Math.max(1, (int) Math.round(srcHeight * absCos + srcWidth * absSin));

        final PixelImage result = new PixelImage(newWidth, newHeight);

        final double srcCenterX = srcWidth / 2.0;
        final double srcCenterY = srcHeight / 2.0;
        final double dstCenterX = newWidth / 2.0;
        final double dstCenterY = newHeight / 2.0;

        final int[] destPixels = result.getPixels();
        for (int i = 0; i < destPixels.length; i++) {
            destPixels[i] = 0xFFFFFFFF; // white background
        }

        for (int dstY = 0; dstY < newHeight; dstY++) {
            for (int dstX = 0; dstX < newWidth; dstX++) {
                final double dx = dstX - dstCenterX;
                final double dy = dstY - dstCenterY;

                final double srcX = dx * cos + dy * sin + srcCenterX;
                final double srcY = -dx * sin + dy * cos + srcCenterY;

                if (srcX >= 0 && srcX < srcWidth - 1 && srcY >= 0 && srcY < srcHeight - 1) {
                    final int argb = bilinear ? bilinearInterpolate(source, srcX, srcY)
                                              : nearestNeighbor(source, srcX, srcY);
                    result.setArgb(dstX, dstY, argb);
                }
            }
        }
        return result;
    }

    private static int bilinearInterpolate(final PixelImage source, final double srcX, final double srcY) {
        final int x0 = (int) srcX;
        final int y0 = (int) srcY;
        final int x1 = Math.min(x0 + 1, source.getWidth() - 1);
        final int y1 = Math.min(y0 + 1, source.getHeight() - 1);

        final double xFrac = srcX - x0;
        final double yFrac = srcY - y0;

        final int c00 = source.getArgb(x0, y0);
        final int c10 = source.getArgb(x1, y0);
        final int c01 = source.getArgb(x0, y1);
        final int c11 = source.getArgb(x1, y1);

        final int a = interpolateChannel(c00, c10, c01, c11, xFrac, yFrac, 24);
        final int r = interpolateChannel(c00, c10, c01, c11, xFrac, yFrac, 16);
        final int g = interpolateChannel(c00, c10, c01, c11, xFrac, yFrac, 8);
        final int b = interpolateChannel(c00, c10, c01, c11, xFrac, yFrac, 0);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int nearestNeighbor(final PixelImage source, final double srcX, final double srcY) {
        final int x = (int) Math.round(srcX);
        final int y = (int) Math.round(srcY);
        return source.getArgb(Math.min(Math.max(x, 0), source.getWidth() - 1),
                              Math.min(Math.max(y, 0), source.getHeight() - 1));
    }

    private static int interpolateChannel(final int c00, final int c10, final int c01, final int c11,
                                          final double xFrac, final double yFrac, final int shift) {
        final int v00 = (c00 >> shift) & 0xFF;
        final int v10 = (c10 >> shift) & 0xFF;
        final int v01 = (c01 >> shift) & 0xFF;
        final int v11 = (c11 >> shift) & 0xFF;

        final double top = v00 + xFrac * (v10 - v00);
        final double bottom = v01 + xFrac * (v11 - v01);
        final double result = top + yFrac * (bottom - top);

        return Math.max(0, Math.min(255, (int) Math.round(result)));
    }
}

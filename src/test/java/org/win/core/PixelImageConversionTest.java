package org.win.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests pixel format conversion between ARGB (internal format) and RGBA (canvas format).
 * This validates the logic used in WebCanvasAdapter.redraw() for browser rendering.
 */
public final class PixelImageConversionTest {

    @Test
    public void testArgbToRgbaConversion() {
        // Create a 2x2 test image with known ARGB values
        final PixelImage image = new PixelImage(2, 2);
        final int[] pixels = image.getPixels();

        // Set test pixels: ARGB format (Alpha, Red, Green, Blue)
        pixels[0] = 0xFF_FF_00_00; // Opaque Red
        pixels[1] = 0xFF_00_FF_00; // Opaque Green
        pixels[2] = 0xFF_00_00_FF; // Opaque Blue
        pixels[3] = 0x80_FF_FF_FF; // 50% transparent White

        // Simulate the conversion done in WebCanvasAdapter.redraw()
        final byte[] rgba = convertArgbToRgba(pixels);

        // Verify pixel 0: Red
        assertThat(rgba[0] & 0xFF).isEqualTo(0xFF); // R
        assertThat(rgba[1] & 0xFF).isEqualTo(0x00); // G
        assertThat(rgba[2] & 0xFF).isEqualTo(0x00); // B
        assertThat(rgba[3] & 0xFF).isEqualTo(0xFF); // A

        // Verify pixel 1: Green
        assertThat(rgba[4] & 0xFF).isEqualTo(0x00); // R
        assertThat(rgba[5] & 0xFF).isEqualTo(0xFF); // G
        assertThat(rgba[6] & 0xFF).isEqualTo(0x00); // B
        assertThat(rgba[7] & 0xFF).isEqualTo(0xFF); // A

        // Verify pixel 2: Blue
        assertThat(rgba[8] & 0xFF).isEqualTo(0x00);  // R
        assertThat(rgba[9] & 0xFF).isEqualTo(0x00);  // G
        assertThat(rgba[10] & 0xFF).isEqualTo(0xFF); // B
        assertThat(rgba[11] & 0xFF).isEqualTo(0xFF); // A

        // Verify pixel 3: 50% White
        assertThat(rgba[12] & 0xFF).isEqualTo(0xFF); // R
        assertThat(rgba[13] & 0xFF).isEqualTo(0xFF); // G
        assertThat(rgba[14] & 0xFF).isEqualTo(0xFF); // B
        assertThat(rgba[15] & 0xFF).isEqualTo(0x80); // A (50% = 0x80)
    }

    @Test
    public void testLinearIndexingMatchesRowMajorOrder() {
        // Verify that linear indexing matches (y * width + x) for row-major order
        final int width = 3;
        final int height = 2;
        final PixelImage image = new PixelImage(width, height);

        // Set unique values using x,y coordinates
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int value = (y * 100) + x;
                image.setArgb(x, y, value);
            }
        }

        // Verify linear access matches
        final int[] pixels = image.getPixels();
        int linearIndex = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                assertThat(pixels[linearIndex]).isEqualTo(image.getArgb(x, y));
                assertThat(pixels[linearIndex]).isEqualTo((y * 100) + x);
                linearIndex++;
            }
        }
    }

    /**
     * Simulates the ARGB to RGBA conversion performed in WebCanvasAdapter.redraw().
     * This is the same logic used in the web version's pixel copying loop.
     */
    private byte[] convertArgbToRgba(final int[] argbPixels) {
        final byte[] rgba = new byte[argbPixels.length * 4];
        for (int i = 0; i < argbPixels.length; i++) {
            final int argb = argbPixels[i];
            final int idx = i * 4;
            rgba[idx] = (byte) ((argb >> 16) & 0xFF);      // R
            rgba[idx + 1] = (byte) ((argb >> 8) & 0xFF);   // G
            rgba[idx + 2] = (byte) (argb & 0xFF);          // B
            rgba[idx + 3] = (byte) ((argb >> 24) & 0xFF);  // A
        }
        return rgba;
    }
}

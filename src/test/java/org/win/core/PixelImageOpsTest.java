package org.win.core;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class PixelImageOpsTest {

    @Test
    public void copyCreatesIndependentImage() {
        final PixelImage source = filled(4, 4, 0xFF0000FF);
        final PixelImage copy = PixelImageOps.copy(source);

        source.setArgb(0, 0, 0xFFFF0000);
        assertThat(copy.getArgb(0, 0)).isEqualTo(0xFF0000FF);
    }

    @Test
    public void cropClampsToBounds() {
        final PixelImage source = gradient(10, 10);
        final PixelImage cropped = PixelImageOps.crop(source, 8, 8, 10, 10);

        assertThat(cropped.getWidth()).isEqualTo(2);
        assertThat(cropped.getHeight()).isEqualTo(2);
        assertThat(cropped.getArgb(1, 1)).isEqualTo(source.getArgb(9, 9));
    }

    @Test
    public void rotateExpandsDimensions() {
        final PixelImage source = filled(10, 20, 0xFF00FF00);
        final PixelImage rotated = PixelImageOps.rotate(source, 90);

        assertThat(rotated.getWidth()).isEqualTo(20);
        assertThat(rotated.getHeight()).isEqualTo(10);
    }

    @Test
    public void rotate45ExpandsSquare() {
        final PixelImage source = filled(100, 100, 0xFF00FF00);
        final PixelImage rotated = PixelImageOps.rotate(source, 45);

        final int expected = (int) Math.ceil(100 * Math.sqrt(2));
        assertThat(rotated.getWidth()).isCloseTo(expected, within(2));
        assertThat(rotated.getHeight()).isCloseTo(expected, within(2));
    }

    @Test
    public void resizeUsesBilinear() {
        final PixelImage source = filled(2, 2, 0xFF000000);
        source.setArgb(1, 1, 0xFFFFFFFF);

        final PixelImage resized = PixelImageOps.resize(source, 4, 4);

        assertThat(resized.getArgb(0, 0)).isEqualTo(0xFF000000);
        assertThat(resized.getArgb(3, 3)).isEqualTo(0xFFFFFFFF);

        final int mixed = resized.getArgb(1, 2);
        assertThat(mixed).isNotEqualTo(0xFF000000);
        assertThat(mixed).isNotEqualTo(0xFFFFFFFF);
    }

    private static PixelImage filled(final int w, final int h, final int argb) {
        final PixelImage image = new PixelImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setArgb(x, y, argb);
            }
        }
        return image;
    }

    private static PixelImage gradient(final int w, final int h) {
        final PixelImage image = new PixelImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setArgb(x, y, (0xFF << 24) | (x << 8) | y);
            }
        }
        return image;
    }
}

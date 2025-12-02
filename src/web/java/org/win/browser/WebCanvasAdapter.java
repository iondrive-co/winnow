package org.win.browser;

import org.teavm.jso.JSBody;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.win.core.InteractionController;
import org.win.core.PixelImage;

/**
 * Canvas adapter for the web that uses InteractionController to manage state.
 */
public final class WebCanvasAdapter {
    private final HTMLCanvasElement canvas;
    private final CanvasRenderingContext2D ctx;
    private InteractionController controller;

    public WebCanvasAdapter(final HTMLCanvasElement canvas, final CanvasRenderingContext2D ctx) {
        this.canvas = canvas;
        this.ctx = ctx;
    }

    public void setImage(final PixelImage image) {
        this.controller = new InteractionController(image);
        controller.selection().setRegion(0, 0, image.getWidth(), image.getHeight());
        redraw();
    }

    public void startSelection(final double x, final double y) {
        controller.startSelection(x, y);
    }

    public void dragSelection(final double x, final double y) {
        controller.dragSelection(x, y);
    }

    public void zoom(final double factor) {
        controller.zoom(factor);
        redraw();
    }

    public void rotate(final double degrees) {
        controller.rotate(degrees);
        redraw();
    }

    public void crop() {
        controller.crop();
        redraw();
    }

    public InteractionController controller() {
        return controller;
    }

    public void redraw() {
        if (controller == null) {
            return;
        }
        final PixelImage image = controller.getImage();
        final double zoom = controller.zoom();
        final int scaledWidth = (int) (image.getWidth() * zoom);
        final int scaledHeight = (int) (image.getHeight() * zoom);

        canvas.setWidth(scaledWidth);
        canvas.setHeight(scaledHeight);

        final HTMLCanvasElement sourceCanvas = createCanvas(image.getWidth(), image.getHeight());
        final CanvasRenderingContext2D sourceCtx = (CanvasRenderingContext2D) sourceCanvas.getContext("2d");
        final org.teavm.jso.canvas.ImageData imageData = sourceCtx.createImageData(image.getWidth(), image.getHeight());
        final org.teavm.jso.typedarrays.Uint8ClampedArray data = imageData.getData();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                final int argb = image.getArgb(x, y);
                final int index = (y * image.getWidth() + x) * 4;
                data.set(index, (argb >> 16) & 0xFF);
                data.set(index + 1, (argb >> 8) & 0xFF);
                data.set(index + 2, argb & 0xFF);
                data.set(index + 3, (argb >> 24) & 0xFF);
            }
        }
        sourceCtx.putImageData(imageData, 0, 0);
        ctx.clearRect(0, 0, scaledWidth, scaledHeight);
        ctx.save();
        ctx.scale(zoom, zoom);
        ctx.drawImage(sourceCanvas, 0, 0);
        ctx.restore();
    }

    public int[] getViewportSize() {
        return new int[]{(int) canvas.getWidth(), (int) canvas.getHeight()};
    }

    @JSBody(params = {"width", "height"}, script = """
            var c = document.createElement('canvas');
            c.width = width;
            c.height = height;
            return c;
            """)
    private static native HTMLCanvasElement createCanvas(int width, int height);
}

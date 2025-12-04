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
    private int viewportWidth = 900;
    private int viewportHeight = 700;

    public WebCanvasAdapter(final HTMLCanvasElement canvas, final CanvasRenderingContext2D ctx) {
        this.canvas = canvas;
        this.ctx = ctx;
    }

    public void setViewportSize(final int width, final int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    public void setImage(final PixelImage image) {
        this.controller = new InteractionController(image);
        controller.selection().setRegion(0, 0, image.getWidth(), image.getHeight());
        controller.zoomToFit(viewportWidth, viewportHeight);
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

        // Keep canvas at fixed viewport size to prevent layout shifts
        canvas.setWidth(viewportWidth);
        canvas.setHeight(viewportHeight);

        // Force layout reflow to ensure canvas dimensions are applied before rendering
        forceLayoutReflow(canvas);

        final HTMLCanvasElement sourceCanvas = createCanvas(image.getWidth(), image.getHeight());
        final CanvasRenderingContext2D sourceCtx = (CanvasRenderingContext2D) sourceCanvas.getContext("2d");
        final org.teavm.jso.canvas.ImageData imageData = sourceCtx.createImageData(image.getWidth(), image.getHeight());
        final org.teavm.jso.typedarrays.Uint8ClampedArray data = imageData.getData();

        // Use single linear loop for better performance and Firefox compatibility
        final int[] pixels = image.getPixels();
        final int pixelCount = pixels.length;
        for (int i = 0; i < pixelCount; i++) {
            final int argb = pixels[i];
            final int idx = i << 2; // i * 4
            data.set(idx, (argb >> 16) & 0xFF);
            data.set(idx + 1, (argb >> 8) & 0xFF);
            data.set(idx + 2, argb & 0xFF);
            data.set(idx + 3, (argb >> 24) & 0xFF);
        }
        sourceCtx.putImageData(imageData, 0, 0);

        // Clear the entire canvas
        ctx.clearRect(0, 0, viewportWidth, viewportHeight);

        // Center the image only if it's smaller than viewport
        // Otherwise draw at (0,0) and let it overflow
        final double offsetX = Math.max(0, (viewportWidth - scaledWidth) / 2.0);
        final double offsetY = Math.max(0, (viewportHeight - scaledHeight) / 2.0);

        ctx.save();
        ctx.translate(offsetX, offsetY);
        ctx.scale(zoom, zoom);
        ctx.drawImage(sourceCanvas, 0, 0);
        ctx.restore();
    }

    public int[] getViewportSize() {
        return new int[]{(int) canvas.getWidth(), (int) canvas.getHeight()};
    }

    public double[] getImageOffset() {
        if (controller == null) {
            return new double[]{0, 0};
        }
        final PixelImage image = controller.getImage();
        final double zoom = controller.zoom();
        final int scaledWidth = (int) (image.getWidth() * zoom);
        final int scaledHeight = (int) (image.getHeight() * zoom);
        // Center only if smaller than viewport, otherwise offset is 0
        final double offsetX = Math.max(0, (viewportWidth - scaledWidth) / 2.0);
        final double offsetY = Math.max(0, (viewportHeight - scaledHeight) / 2.0);
        return new double[]{offsetX, offsetY};
    }

    @JSBody(params = {"width", "height"}, script = """
            var c = document.createElement('canvas');
            c.width = width;
            c.height = height;
            return c;
            """)
    private static native HTMLCanvasElement createCanvas(int width, int height);

    @JSBody(params = {"element"}, script = "element.getBoundingClientRect();")
    private static native void forceLayoutReflow(HTMLCanvasElement element);
}

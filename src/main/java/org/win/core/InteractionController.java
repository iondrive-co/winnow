package org.win.core;

/**
 * Platform-neutral interaction controller for image operations and selection/zoom state.
 */
public final class InteractionController {
    public static final double MIN_ZOOM = 0.1;
    public static final double MAX_ZOOM = 8.0;
    public static final double DEFAULT_ZOOM = 1.0;

    private PixelImage image;
    private final SelectionModel selection;
    private double zoom = DEFAULT_ZOOM;

    public InteractionController(final PixelImage image) {
        this.image = image;
        this.selection = new SelectionModel(image.getWidth(), image.getHeight());
    }

    public PixelImage getImage() {
        return image;
    }

    public SelectionModel selection() {
        return selection;
    }

    public double zoom() {
        return zoom;
    }

    public void centerSelection() {
        final double insetX = image.getWidth() * 0.25;
        final double insetY = image.getHeight() * 0.25;
        selection.setRegion(insetX, insetY, image.getWidth() - insetX, image.getHeight() - insetY);
    }

    public void setSelectionSize(final int width, final int height) {
        final double left = selection.getTopLeftX();
        final double top = selection.getTopLeftY();
        final double newWidth = Math.max(1, Math.min(width, image.getWidth()));
        final double newHeight = Math.max(1, Math.min(height, image.getHeight()));
        selection.setRegion(left, top, left + newWidth, top + newHeight);
    }

    public void zoom(final double factor) {
        zoom = Math.max(MIN_ZOOM, Math.min(zoom * factor, MAX_ZOOM));
    }

    public void startSelection(final double x, final double y) {
        selection.setRegion(x, y, x, y);
    }

    public void dragSelection(final double x, final double y) {
        selection.setRegion(selection.getTopLeftX(), selection.getTopLeftY(), x, y);
    }

    public void endSelection() {
        // no-op for now; hook for future behaviors
    }

    public PixelImage crop() {
        final int x = (int) Math.round(selection.getTopLeftX());
        final int y = (int) Math.round(selection.getTopLeftY());
        final int w = selection.getWidthInt();
        final int h = selection.getHeightInt();

        image = PixelImageOps.crop(image, x, y, w, h);
        selection.setImageDimensions(image.getWidth(), image.getHeight(), true);
        return image;
    }

    public PixelImage rotate(final double degrees) {
        image = PixelImageOps.rotate(image, degrees);
        selection.setImageDimensions(image.getWidth(), image.getHeight(), true);
        return image;
    }
}

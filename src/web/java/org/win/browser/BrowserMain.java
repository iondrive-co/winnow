package org.win.browser;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MouseEvent;
import org.teavm.jso.dom.events.WheelEvent;
import org.teavm.jso.dom.html.*;
import org.win.core.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Browser entry point using TeaVM with maximal reuse of shared core state/algorithms.
 *
 * <h2>Web-Only Code Justification</h2>
 * This class contains the irreducible web-specific layer that cannot be shared with desktop:
 *
 * <ul>
 *   <li><b>File loading (handleFileSelect, htmlImageToPixelImage):</b> Browser File API, FileReader,
 *       and HTMLImageElement are platform-specific. Desktop uses standard Java file I/O.</li>
 *   <li><b>DOM structure (buildUI, createControlBar, etc.):</b> HTML/CSS layout is inherently
 *       different from JavaFX scene graph. This wiring layer is necessary but contains no
 *       business logic - all actions delegate to ControlViewModel and WebCanvasAdapter.</li>
 *   <li><b>CSS injection (injectStyles):</b> Inline styles are used for single-file deployment.
 *       Could be extracted to external CSS but would add build complexity.</li>
 *   <li><b>Event handlers (wheel, mouse, keyboard):</b> DOM event models differ from JavaFX.
 *       However, event handling logic delegates to shared SelectionAdjuster and InteractionController.</li>
 *   <li><b>Selection overlay (updateSelectionOverlay, positionSelectionHandles):</b> HTML div-based
 *       overlay positioning is web-specific. Desktop uses JavaFX Canvas drawing. Both drive from
 *       the same SelectionModel state.</li>
 * </ul>
 *
 * <h2>Shared Logic</h2>
 * All business logic resides in org.win.core:
 * <ul>
 *   <li>Image operations: PixelImageOps (crop, rotate, resize)</li>
 *   <li>Selection state: SelectionModel, SelectionAdjuster</li>
 *   <li>Interaction state: InteractionController (zoom, selection, image manipulation)</li>
 *   <li>Undo management: ImageUndo (InMemoryImageUndo for web, file-based undo on desktop)</li>
 *   <li>Control state: ControlViewModel (filename, navigation, action bindings)</li>
 * </ul>
 */
public final class BrowserMain {
    private final HTMLDocument doc = HTMLDocument.current();
    private final HTMLCanvasElement canvas = (HTMLCanvasElement) doc.createElement("canvas");
    private final CanvasRenderingContext2D ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
    private WebCanvasAdapter canvasAdapter;
    private HTMLElement canvasHost;
    private final HTMLElement selectionOverlay = (HTMLElement) doc.createElement("div");
    private final List<HTMLElement> selectionHandles = new ArrayList<>();

    private final List<ImageItem> images = new ArrayList<>();
    private final ImageUndo undoHistory = new InMemoryImageUndo(15);
    private int currentIndex = -1;
    private boolean dragging = false;
    private final ControlViewModel controls = new ControlViewModel();

    private HTMLInputElement dirInput;
    private HTMLElement filenameEditor;
    private final List<HTMLInputElement> filenameSegments = new ArrayList<>();
    private final List<HTMLElement> datalists = new ArrayList<>();
    private String filenameExtension = "";
    private boolean renderingFilename = false;
    private HTMLElement statusLabel;
    private HTMLElement dimensionLabel;
    private HTMLInputElement selectionWidthField;
    private HTMLInputElement selectionHeightField;
    private boolean syncingSelectionFields = false;
    private boolean draggingSelection = false;
    private double dragLastX;
    private double dragLastY;

    public static void main(final String[] args) {
        try {
            new BrowserMain().start();
            hideFallback();
        } catch (final Throwable t) {
            showFallback("App failed: " + t.getMessage());
        }
    }

    private void start() {
        buildUI();
        wireHandlers();
    }

    @JSBody(script = "var fb = document.getElementById('fallback'); if (fb) fb.style.display = 'none';")
    private static native void hideFallback();

    @JSBody(params = {"msg"}, script = "var fb = document.getElementById('fallback'); if (fb) { fb.innerText = msg; fb.style.display = 'block'; }")
    private static native void showFallback(String msg);

    private void buildUI() {
        injectStyles();
        final HTMLElement body = doc.getBody();
        body.setInnerHTML("");

        final HTMLElement shell = doc.createElement("div");
        shell.setClassName("winnow-shell");

        shell.appendChild(createHeader());
        shell.appendChild(createOpenRow());
        shell.appendChild(createCanvasStage());
        body.appendChild(shell);
    }

    private HTMLElement createHeader() {
        final HTMLElement header = doc.createElement("div");
        header.setClassName("header");

        final HTMLElement h1 = doc.createElement("h1");
        h1.setTextContent("Winnow");

        header.appendChild(h1);
        return header;
    }

    private HTMLElement createOpenRow() {
        final HTMLElement openRow = div("controls open-row");

        dirInput = (HTMLInputElement) doc.createElement("input");
        dirInput.setType("file");
        dirInput.setAttribute("webkitdirectory", "true");
        dirInput.setAttribute("directory", "true");
        dirInput.setAttribute("mozdirectory", "true");
        dirInput.setAttribute("multiple", "true");
        dirInput.addEventListener("change", evt -> handleFileSelect(getFiles(dirInput)));

        openRow.appendChild(button("Open Folder…", "open-folder", e -> dirInput.click()));
        return openRow;
    }

    private HTMLElement createCanvasStage() {
        final HTMLElement stage = div("canvas-stage");
        canvasHost = div("canvas-host");

        canvas.setWidth(900);
        canvas.setHeight(700);
        canvasHost.appendChild(canvas);
        canvasAdapter = new WebCanvasAdapter(canvas, ctx);

        selectionOverlay.setClassName("selection");
        selectionOverlay.getStyle().setProperty("display", "none");
        buildSelectionHandles();
        addSelectionDragHandlers();
        canvasHost.appendChild(selectionOverlay);

        canvasHost.addEventListener("wheel", (EventListener<WheelEvent>) evt -> {
            // Prevent default first to stop page zoom
            evt.preventDefault();
            evt.stopPropagation();

            // Only zoom picture if we have an image
            if (currentIndex < 0) return;

            final double factor = evt.getDeltaY() < 0 ? 1.1 : 0.9;
            adjustZoom(factor);
        });
        stage.appendChild(canvasHost);
        stage.appendChild(createControlBar());
        return stage;
    }

    private void wireHandlers() {
        // Prevent browser zoom on wheel events document-wide
        doc.addEventListener("wheel", (EventListener<WheelEvent>) evt -> {
            // Prevent default browser zoom behavior (Ctrl+scroll)
            // Check if ctrl key is pressed using getCtrlKey() method
            if (evt.getCtrlKey()) {
                evt.preventDefault();
            }
        });

        canvas.addEventListener("mousedown", (EventListener<MouseEvent>) evt -> {
            if (currentIndex < 0) return;
            dragging = true;
            final double x = canvasX(evt);
            final double y = canvasY(evt);
            canvasAdapter.startSelection(x, y);
            updateSelectionOverlay();
        });

        canvas.addEventListener("mousemove", (EventListener<MouseEvent>) evt -> {
            if (!dragging || currentIndex < 0) return;
            final double x = canvasX(evt);
            final double y = canvasY(evt);
            canvasAdapter.dragSelection(x, y);
            updateSelectionOverlay();
        });

        canvas.addEventListener("mouseup", evt -> dragging = false);
        canvas.addEventListener("mouseleave", evt -> dragging = false);

        doc.addEventListener("keydown", (EventListener<org.teavm.jso.dom.events.KeyboardEvent>) evt -> {
            final int code = evt.getKeyCode();
            final boolean ctrl = evt.isCtrlKey();
            final boolean shift = evt.isShiftKey();

            if (ctrl && code == 90) { // Ctrl+Z
                evt.preventDefault();
                controls.triggerUndo();
            } else if (code == 37) { // Left
                controls.triggerPrev();
            } else if (code == 39) { // Right
                controls.triggerNext();
            } else if (code == 189 || code == 109) { // Minus
                controls.triggerZoom(0.9);
            } else if (code == 187 || code == 107) { // Plus/equals
                controls.triggerZoom(1.1);
            } else if (ctrl && code == 81) { // Ctrl+Q rotate left
                controls.triggerRotate(-5);
            } else if (ctrl && code == 69) { // Ctrl+E rotate right
                controls.triggerRotate(5);
            } else if (canvasAdapter != null && canvasAdapter.controller() != null && SelectionAdjuster.applyShortcut(code, ctrl, shift, 10, canvasAdapter.controller().selection())) {
                updateSelectionOverlay();
            }
        });

        // Handle window resize (e.g., when dev tools open/close)
        getWindow().addEventListener("resize", evt -> {
            if (hasImage()) {
                updateViewportSize();
                canvasAdapter.setImage(currentImage());
                updateSelectionOverlay();
            }
        });
    }

    private void handleFileSelect(final JSObject files) {
        images.clear();
        currentIndex = -1;
        undoHistory.clear();

        final int len = filesLength(files);
        for (int i = 0; i < len; i++) {
            final JSObject file = fileAt(files, i);
            readFileAsDataURL(file, dataUrl -> {
                final HTMLImageElement img = (HTMLImageElement) doc.createElement("img");
                img.addEventListener("load", e -> {
                    final PixelImage pixelImage = htmlImageToPixelImage(img);
                    images.add(new ImageItem(getFileName(file), pixelImage));
                    if (currentIndex == -1) {
                        currentIndex = 0;
                        displayCurrentImage();
                    } else {
                        updateFilenameAndStatus();
                    }
                });
                img.setSrc(dataUrl);
            });
        }

        // Allow selecting the same folder again by clearing the input
        if (dirInput != null) {
            dirInput.setValue("");
        }
    }

    private void displayCurrentImage() {
        if (currentIndex < 0 || currentIndex >= images.size()) return;

        // Update viewport size based on available window space
        updateViewportSize();

        final PixelImage image = images.get(currentIndex).image();
        canvasAdapter.setImage(image);
        updateFilenameAndStatus();
        updateSelectionOverlay();
    }

    private void updateViewportSize() {
        // Get actual canvas-host dimensions to ensure canvas buffer matches displayed size
        final int hostWidth = (int) getElementClientWidth(canvasHost);
        final int hostHeight = (int) getElementClientHeight(canvasHost);

        // Use actual host dimensions if available, otherwise fall back to calculated viewport
        final int viewportWidth;
        final int viewportHeight;
        if (hostWidth > 0 && hostHeight > 0) {
            viewportWidth = hostWidth;
            viewportHeight = hostHeight;
        } else {
            // Fallback for initial load before layout is complete
            // Use conservative percentages to ensure controls remain visible without scrolling
            final int windowWidth = getWindowInnerWidth();
            final int windowHeight = getWindowInnerHeight();
            viewportWidth = Math.min(1200, (int) (windowWidth * 0.85));
            viewportHeight = Math.min(800, (int) (windowHeight * 0.55));
        }

        final int finalWidth = Math.max(400, viewportWidth);
        final int finalHeight = Math.max(300, viewportHeight);

        canvasAdapter.setViewportSize(finalWidth, finalHeight);
    }

    private void updateSelectionOverlay() {
        if (canvasAdapter == null || canvasAdapter.controller() == null) {
            hideSelectionOverlay();
            return;
        }

        // Force a layout reflow to ensure canvas is properly sized and positioned
        // before calculating selection overlay position
        forceLayoutReflow(canvas);
        forceLayoutReflow(canvasHost);

        final SelectionModel selection = canvasAdapter.controller().selection();
        final double zoom = canvasAdapter.controller().zoom();
        final double[] offset = canvasAdapter.getImageOffset();

        // Both canvas and selection overlay are children of canvas-host,
        // positioned in its content area, so they should align directly
        final double left = selection.getTopLeftX() * zoom + offset[0];
        final double top = selection.getTopLeftY() * zoom + offset[1];
        final double width = selection.getWidth() * zoom;
        final double height = selection.getHeight() * zoom;

        if (width <= 0 || height <= 0) {
            hideSelectionOverlay();
            return;
        }

        selectionOverlay.getStyle().setProperty("display", "block");
        selectionOverlay.getStyle().setProperty("left", left + "px");
        selectionOverlay.getStyle().setProperty("top", top + "px");
        selectionOverlay.getStyle().setProperty("width", width + "px");
        selectionOverlay.getStyle().setProperty("height", height + "px");
        positionSelectionHandles(width, height);

        if (dimensionLabel != null && hasImage()) {
            dimensionLabel.setTextContent(currentImage().getWidth() + " x " + currentImage().getHeight());
        }
        syncSelectionSizeFields(selection);
    }

    private void hideSelectionOverlay() {
        selectionOverlay.getStyle().setProperty("display", "none");
        if (dimensionLabel != null) {
            dimensionLabel.setTextContent("0 x 0");
        }
    }

    private void updateFilenameAndStatus() {
        if (hasImage()) {
            renderFilenameEditor(images.get(currentIndex).name());
        }
        if (statusLabel != null) {
            controls.syncFilename(images.get(currentIndex).name());
            controls.setPosition(currentIndex, images.size());
            statusLabel.setTextContent(controls.status());
        }
    }

    private void cropImage() {
        if (!hasImage()) return;
        final PixelImage current = currentImage();
        undoHistory.save(current, images.get(currentIndex).name());

        canvasAdapter.crop();
        final PixelImage updated = PixelImageOps.copy(canvasAdapter.controller().getImage());
        images.set(currentIndex, new ImageItem(images.get(currentIndex).name(), updated));
        updateSelectionOverlay();
    }

    private void rotateImage(final double degrees) {
        if (!hasImage()) return;
        undoHistory.save(currentImage(), images.get(currentIndex).name());

        canvasAdapter.rotate(degrees);
        final PixelImage rotated = PixelImageOps.copy(canvasAdapter.controller().getImage());
        images.set(currentIndex, new ImageItem(images.get(currentIndex).name(), rotated));
        updateSelectionOverlay();
    }

    private void adjustZoom(final double factor) {
        if (canvasAdapter != null) {
            canvasAdapter.zoom(factor);
        }
        if (hasImage()) {
            updateSelectionOverlay();
        }
    }

    private void navigate(final int delta) {
        if (images.isEmpty()) return;
        currentIndex = Math.floorMod(currentIndex + delta, images.size());
        displayCurrentImage();
    }

    private void renameCurrent(final String newName) {
        if (!hasImage() || newName == null || newName.isBlank()) {
            return;
        }
        images.set(currentIndex, new ImageItem(newName.trim(), currentImage()));
        updateFilenameAndStatus();
    }

    private void undo() {
        if (!undoHistory.canUndo()) return;
        final ImageUndo.UndoEntry result = undoHistory.undo();
        images.set(currentIndex, new ImageItem(result.name(), result.image()));
        canvasAdapter.setImage(result.image());
        updateSelectionOverlay();
        controls.syncFilename(result.name());
        updateFilenameAndStatus();
    }

    private PixelImage currentImage() {
        return images.get(currentIndex).image();
    }

    private boolean hasImage() {
        return currentIndex >= 0 && currentIndex < images.size();
    }

    private double canvasX(final MouseEvent evt) {
        final double zoom = canvasAdapter != null && canvasAdapter.controller() != null
            ? canvasAdapter.controller().zoom()
            : InteractionController.DEFAULT_ZOOM;
        final double[] offset = canvasAdapter != null ? canvasAdapter.getImageOffset() : new double[]{0, 0};
        return (evt.getClientX() - canvas.getBoundingClientRect().getLeft() - offset[0]) / zoom;
    }

    private double canvasY(final MouseEvent evt) {
        final double zoom = canvasAdapter != null && canvasAdapter.controller() != null
            ? canvasAdapter.controller().zoom()
            : InteractionController.DEFAULT_ZOOM;
        final double[] offset = canvasAdapter != null ? canvasAdapter.getImageOffset() : new double[]{0, 0};
        return (evt.getClientY() - canvas.getBoundingClientRect().getTop() - offset[1]) / zoom;
    }

    private PixelImage htmlImageToPixelImage(final HTMLImageElement img) {
        final HTMLCanvasElement tempCanvas = (HTMLCanvasElement) doc.createElement("canvas");
        final int width = img.getNaturalWidth();
        final int height = img.getNaturalHeight();
        tempCanvas.setWidth(width);
        tempCanvas.setHeight(height);
        final CanvasRenderingContext2D tempCtx = (CanvasRenderingContext2D) tempCanvas.getContext("2d");
        tempCtx.drawImage(img, 0, 0);

        final org.teavm.jso.canvas.ImageData data = tempCtx.getImageData(0, 0, width, height);
        final org.teavm.jso.typedarrays.Uint8ClampedArray arr = data.getData();

        final PixelImage result = new PixelImage(width, height);
        final int[] pixels = result.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            final int idx = i * 4;
            final int r = arr.get(idx) & 0xFF;
            final int g = arr.get(idx + 1) & 0xFF;
            final int b = arr.get(idx + 2) & 0xFF;
            final int a = arr.get(idx + 3) & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return result;
    }

    private HTMLElement button(final String text, final String slot, final EventListener<?> listener) {
        final HTMLButtonElement button = (HTMLButtonElement) doc.createElement("button");
        button.setTextContent(text);
        button.setAttribute("data-slot", slot);
        button.addEventListener("click", listener);
        return button;
    }

    private HTMLElement button(final String text, final EventListener<?> listener) {
        return button(text, "", listener);
    }

    private HTMLElement createControlBar() {
        final HTMLElement bar = div("control-bar");
        bar.setId("control-bar");
        bar.setAttribute("data-role", "control-bar");

        bar.appendChild(button("Undo", "undo", e -> controls.triggerUndo()));
        bar.appendChild(button("Crop", "crop", e -> controls.triggerCrop()));

        dimensionLabel = doc.createElement("span");
        dimensionLabel.setClassName("dimensions");
        dimensionLabel.setAttribute("data-slot", "dimensions");
        dimensionLabel.setTextContent("0 x 0");
        bar.appendChild(dimensionLabel);

        selectionWidthField = (HTMLInputElement) doc.createElement("input");
        selectionWidthField.setClassName("selection-size");
        selectionWidthField.setAttribute("type", "number");
        selectionWidthField.setAttribute("min", "1");
        selectionWidthField.setValue("0");
        selectionWidthField.addEventListener("change", e -> updateSelectionFromFields());

        final HTMLElement xLabel = doc.createElement("span");
        xLabel.setTextContent("x");
        xLabel.setClassName("dimensions-separator");

        selectionHeightField = (HTMLInputElement) doc.createElement("input");
        selectionHeightField.setClassName("selection-size");
        selectionHeightField.setAttribute("type", "number");
        selectionHeightField.setAttribute("min", "1");
        selectionHeightField.setValue("0");
        selectionHeightField.addEventListener("change", e -> updateSelectionFromFields());

        bar.appendChild(selectionWidthField);
        bar.appendChild(xLabel);
        bar.appendChild(selectionHeightField);

        bar.appendChild(button("←", "prev", e -> controls.triggerPrev()));

        statusLabel = doc.createElement("span");
        statusLabel.setClassName("status");
        statusLabel.setAttribute("data-slot", "status");
        statusLabel.setTextContent("0 / 0");
        bar.appendChild(statusLabel);

        bar.appendChild(button("→", "next", e -> controls.triggerNext()));

        filenameEditor = div("filename-editor");
        filenameEditor.setAttribute("data-slot", "filename");
        renderFilenameEditor("untitled.png");
        bar.appendChild(filenameEditor);

        bindControls();
        return bar;
    }

    private void renderFilenameEditor(final String filename) {
        if (filenameEditor == null) return;
        renderingFilename = true;
        filenameEditor.setInnerHTML("");
        filenameSegments.clear();
        clearDatalists();

        final int dot = filename.lastIndexOf('.');
        final String base = dot > 0 ? filename.substring(0, dot) : filename;
        filenameExtension = dot > 0 && dot < filename.length() - 1 ? filename.substring(dot + 1) : "";

        final List<String> currentSegments = splitSegments(base);
        final List<List<String>> segmentOptions = collectSegmentOptions();
        final int maxSegments = Math.max(currentSegments.size(), segmentOptions.size());

        for (int i = 0; i < maxSegments; i++) {
            final String value = i < currentSegments.size() ? currentSegments.get(i) : "";
            final List<String> options = i < segmentOptions.size() ? segmentOptions.get(i) : List.of();
            filenameEditor.appendChild(createSegmentCombo(value, options, i));
        }

        final HTMLElement plusButton = button("+", e -> {
            filenameEditor.insertBefore(createSegmentCombo("", List.of(), filenameSegments.size()), filenameEditor.getLastChild());
            propagateFilenameFromSegments();
        });
        plusButton.setClassName("segment-add");

        final HTMLElement ext = doc.createElement("span");
        ext.setClassName("extension");
        ext.setTextContent(filenameExtension.isBlank() ? "" : "." + filenameExtension);

        filenameEditor.appendChild(plusButton);
        filenameEditor.appendChild(ext);
        renderingFilename = false;
    }

    private HTMLInputElement createSegmentCombo(final String value, final List<String> options, final int index) {
        final HTMLInputElement input = (HTMLInputElement) doc.createElement("input");
        input.setClassName("filename-segment");
        input.setAttribute("list", "fn-options-" + index);
        input.setValue(value);
        input.addEventListener("input", e -> propagateFilenameFromSegments());
        input.addEventListener("keydown", (EventListener<org.teavm.jso.dom.events.KeyboardEvent>) e -> {
            if (e.getKeyCode() == 13) {
                e.preventDefault();
                propagateFilenameFromSegments();
            }
        });
        final HTMLElement datalist = doc.createElement("datalist");
        datalist.setId("fn-options-" + index);
        for (String option : options) {
            final HTMLElement opt = doc.createElement("option");
            opt.setAttribute("value", option);
            datalist.appendChild(opt);
        }
        datalists.add(datalist);
        doc.getBody().appendChild(datalist);
        filenameSegments.add(input);
        return input;
    }

    private void clearDatalists() {
        for (final HTMLElement dl : datalists) {
            final var parent = dl.getParentNode();
            if (parent != null) {
                parent.removeChild(dl);
            }
        }
        datalists.clear();
    }

    private void propagateFilenameFromSegments() {
        if (renderingFilename) {
            return;
        }
        final String newName = composeFilename();
        controls.setFilename(newName);
    }

    private String composeFilename() {
        final StringBuilder base = new StringBuilder();
        for (int i = 0; i < filenameSegments.size(); i++) {
            final String segment = filenameSegments.get(i).getValue().trim();
            if (segment.isEmpty()) continue;
            if (!base.isEmpty()) {
                base.append('_');
            }
            base.append(segment);
        }
        if (base.isEmpty()) {
            base.append("image");
        }
        if (!filenameExtension.isBlank()) {
            base.append('.').append(filenameExtension);
        }
        return base.toString();
    }

    private List<String> splitSegments(final String base) {
        final String[] parts = base.split("[_\\- ]+");
        if (parts.length == 0) {
            return List.of(base);
        }
        final List<String> result = new ArrayList<>();
        for (final String p : parts) {
            if (!p.isBlank()) {
                result.add(p);
            }
        }
        return result.isEmpty() ? List.of(base) : result;
    }

    private List<List<String>> collectSegmentOptions() {
        final List<List<String>> options = new ArrayList<>();
        for (final ImageItem item : images) {
            final int dot = item.name().lastIndexOf('.');
            final String base = dot > 0 ? item.name().substring(0, dot) : item.name();
            final List<String> segments = splitSegments(base);
            for (int i = 0; i < segments.size(); i++) {
                while (options.size() <= i) {
                    options.add(new ArrayList<>());
                }
                final List<String> list = options.get(i);
                final String seg = segments.get(i);
                if (!list.contains(seg)) {
                    list.add(seg);
                }
            }
        }
        return options;
    }

    private void updateSelectionFromFields() {
        if (canvasAdapter == null || canvasAdapter.controller() == null || syncingSelectionFields) {
            return;
        }
        try {
            final int width = Integer.parseInt(selectionWidthField.getValue());
            final int height = Integer.parseInt(selectionHeightField.getValue());
            canvasAdapter.controller().setSelectionSize(width, height);
            updateSelectionOverlay();
        } catch (NumberFormatException ignored) {
            // keep existing selection
        }
    }

    private void syncSelectionSizeFields(final SelectionModel selection) {
        if (selectionWidthField == null || selectionHeightField == null) {
            return;
        }
        syncingSelectionFields = true;
        selectionWidthField.setValue(String.valueOf(selection.getWidthInt()));
        selectionHeightField.setValue(String.valueOf(selection.getHeightInt()));
        syncingSelectionFields = false;
    }

    private void buildSelectionHandles() {
        selectionHandles.clear();
        selectionOverlay.setAttribute("data-role", "selection-overlay");
        selectionOverlay.getStyle().setProperty("pointer-events", "none");

        selectionHandles.add(createHandle("tl"));
        selectionHandles.add(createHandle("tr"));
        selectionHandles.add(createHandle("bl"));
        selectionHandles.add(createHandle("br"));
        selectionHandles.add(createHandle("move"));
    }

    private void addSelectionDragHandlers() {
        if (selectionHandles.isEmpty()) {
            return;
        }
        final HTMLElement moveHandle = selectionHandles.get(4);

        moveHandle.addEventListener("mousedown", (EventListener<MouseEvent>) evt -> {
            if (!hasImage() || canvasAdapter == null || canvasAdapter.controller() == null) {
                return;
            }
            draggingSelection = true;
            dragLastX = evt.getClientX();
            dragLastY = evt.getClientY();
            evt.preventDefault();
        });

        doc.addEventListener("mousemove", (EventListener<MouseEvent>) evt -> {
            if (!draggingSelection || canvasAdapter == null || canvasAdapter.controller() == null) {
                return;
            }
            final double zoom = canvasAdapter.controller().zoom();
            final double dx = (evt.getClientX() - dragLastX) / zoom;
            final double dy = (evt.getClientY() - dragLastY) / zoom;
            canvasAdapter.controller().selection().move(dx, dy);
            dragLastX = evt.getClientX();
            dragLastY = evt.getClientY();
            updateSelectionOverlay();
        });

        doc.addEventListener("mouseup", evt -> draggingSelection = false);
    }

    private HTMLElement createHandle(final String position) {
        final HTMLElement handle = div("selection-handle " + position);
        handle.getStyle().setProperty("position", "absolute");
        handle.getStyle().setProperty("pointer-events", "move".equals(position) ? "auto" : "none");
        selectionOverlay.appendChild(handle);
        return handle;
    }

    private void positionSelectionHandles(final double width, final double height) {
        if (selectionHandles.isEmpty()) {
            return;
        }
        final double handleSize = 10.0;
        setHandle(selectionHandles.get(0), -handleSize / 2, -handleSize / 2);
        setHandle(selectionHandles.get(1), width - handleSize / 2, -handleSize / 2);
        setHandle(selectionHandles.get(2), -handleSize / 2, height - handleSize / 2);
        setHandle(selectionHandles.get(3), width - handleSize / 2, height - handleSize / 2);
        setHandle(selectionHandles.get(4), width / 2 - handleSize / 2, -handleSize * 1.5);
    }

    private void setHandle(final HTMLElement handle, final double left, final double top) {
        handle.getStyle().setProperty("left", left + "px");
        handle.getStyle().setProperty("top", top + "px");
    }

    private HTMLElement div(final String className) {
        final HTMLElement element = doc.createElement("div");
        element.setClassName(className);
        return element;
    }

    private record ImageItem(String name, PixelImage image) {}

    @JSFunctor
    private interface StringCallback extends JSObject {
        void accept(String value);
    }

    private void bindControls() {
        controls.bindPrev(() -> navigate(-1));
        controls.bindNext(() -> navigate(1));
        controls.bindCrop(this::cropImage);
        controls.bindRotate(this::rotateImage);
        controls.bindUndo(this::undo);
        controls.bindZoom(this::adjustZoom);
        controls.bindRename(this::renameCurrent);
    }

    // JS interop helpers
    @JSBody(params = {"input"}, script = "return input.files;")
    private static native JSObject getFiles(HTMLInputElement input);

    @JSBody(params = {"files"}, script = "return files.length;")
    private static native int filesLength(JSObject files);

    @JSBody(params = {"files", "index"}, script = "return files[index];")
    private static native JSObject fileAt(JSObject files, int index);

    @JSBody(params = {"file"}, script = "return file.name || 'untitled';")
    private static native String getFileName(JSObject file);

    @JSBody(params = {"file", "callback"}, script = """
            var reader = new FileReader();
            reader.onload = function(e) { callback(e.target.result); };
            reader.readAsDataURL(file);
            """)
    private static native void readFileAsDataURL(JSObject file, StringCallback callback);

    @JSBody(script = "return window.innerWidth || document.documentElement.clientWidth;")
    private static native int getWindowInnerWidth();

    @JSBody(script = "return window.innerHeight || document.documentElement.clientHeight;")
    private static native int getWindowInnerHeight();

    @JSBody(params = {"element"}, script = "element.getBoundingClientRect();")
    private static native void forceLayoutReflow(HTMLElement element);

    @JSBody(params = {"element"}, script = "return element.clientWidth;")
    private static native double getElementClientWidth(HTMLElement element);

    @JSBody(params = {"element"}, script = "return element.clientHeight;")
    private static native double getElementClientHeight(HTMLElement element);

    @JSBody(script = "return window;")
    private static native org.teavm.jso.dom.events.EventTarget getWindow();

    @JSBody(script = ""
            + "var style = document.createElement('style');"
            + "style.textContent = '"
            + "  body { font-family: \"JetBrains Mono\", \"Segoe UI\", sans-serif; background: #0f1117; color: #e2e8f0; margin: 0; }"
            + "  .winnow-shell { max-width: 1200px; margin: 16px auto; padding: 12px; background: #0b0e15; border: 1px solid #222733; border-radius: 12px; box-shadow: 0 16px 40px rgba(0,0,0,0.45); display: flex; flex-direction: column; gap: 12px; }"
            + "  .header { display: flex; justify-content: space-between; align-items: flex-end; }"
            + "  .header h1 { margin: 0; font-size: 20px; letter-spacing: 0.3px; }"
            + "  .subtitle { margin: 0; color: #a5b4c3; font-size: 12px; }"
            + "  .open-row { display: flex; gap: 8px; }"
            + "  .canvas-stage { display: flex; flex-direction: column; gap: 8px; align-items: center; }"
            + "  .canvas-host { position: relative; background: #000; border: 1px solid #1f2430; border-radius: 10px; overflow: hidden; display: inline-block; }"
            + "  canvas { display: block; max-width: 100%; background: #05080f; }"
            + "  .control-bar { display: flex; align-items: center; gap: 12px; background: #111624; border: 1px solid #1f2430; border-radius: 10px; padding: 10px 12px; }"
            + "  .control-bar button, .open-row button { background: #1a2233; color: #e2e8f0; border: 1px solid #2b3445; border-radius: 8px; padding: 10px 14px; cursor: pointer; font-size: 14px; min-width: 72px; }"
            + "  .control-bar button:hover, .open-row button:hover { background: #212b3d; }"
            + "  .status { min-width: 72px; text-align: center; font-weight: 700; }"
            + "  .dimensions { min-width: 80px; text-align: center; color: #93c5fd; font-weight: 600; }"
            + "  .dimensions-separator { color: #93c5fd; }"
            + "  .selection-size { width: 70px; background: #0d1220; color: #e2e8f0; border: 1px solid #2b3445; border-radius: 6px; padding: 6px 8px; }"
            + "  .filename-editor { display: flex; align-items: center; gap: 4px; flex: 1; }"
            + "  .filename-segment { background: #0d1220; color: #e2e8f0; border: 1px solid #2b3445; border-radius: 6px; padding: 8px 10px; min-width: 80px; }"
            + "  .segment-add { min-width: 32px; padding: 8px 10px; }"
            + "  .extension { color: #cbd5e1; min-width: 50px; text-align: right; }"
            + "  .selection { position: absolute; border: 2px solid #10b981; box-shadow: 0 0 0 1px rgba(16,185,129,0.4); pointer-events: none; border-radius: 2px; }"
            + "  .selection-handle { width: 10px; height: 10px; background: #059669; border: 1px solid #6ee7b7; border-radius: 2px; }"
            + "';"
            + "document.head.appendChild(style);")
    private static native void injectStyles();
}

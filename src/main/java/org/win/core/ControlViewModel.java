package org.win.core;

import java.util.function.Consumer;

/**
 * Minimal shared control view model for filename/status and actions.
 */
public final class ControlViewModel {
    private String filename = "";
    private int index = 0;
    private int total = 0;

    private Consumer<String> onRename;
    private Runnable onPrev;
    private Runnable onNext;
    private Runnable onCrop;
    private Consumer<Double> onRotate;
    private Runnable onUndo;
    private Consumer<Double> onZoom;

    public String filename() {
        return filename;
    }

    public void setFilename(final String filename) {
        final String safeName = filename == null ? "" : filename;
        if (safeName.equals(this.filename)) return;
        this.filename = safeName;
        if (onRename != null) onRename.accept(safeName);
    }

    public String status() {
        return total == 0 ? "0 / 0" : (index + 1) + " / " + total;
    }

    public void syncFilename(final String filename) {
        this.filename = filename == null ? "" : filename;
    }

    public void setPosition(final int index, final int total) {
        this.index = index;
        this.total = total;
    }

    public void bindRename(final Consumer<String> handler) {
        this.onRename = handler;
    }

    public void bindPrev(final Runnable handler) {
        this.onPrev = handler;
    }

    public void bindNext(final Runnable handler) {
        this.onNext = handler;
    }

    public void bindCrop(final Runnable handler) {
        this.onCrop = handler;
    }

    public void bindRotate(final Consumer<Double> handler) {
        this.onRotate = handler;
    }

    public void bindUndo(final Runnable handler) {
        this.onUndo = handler;
    }

    public void bindZoom(final Consumer<Double> handler) {
        this.onZoom = handler;
    }

    public void triggerPrev() {
        if (onPrev != null) onPrev.run();
    }

    public void triggerNext() {
        if (onNext != null) onNext.run();
    }

    public void triggerCrop() {
        if (onCrop != null) onCrop.run();
    }

    public void triggerRotate(final double degrees) {
        if (onRotate != null) onRotate.accept(degrees);
    }

    public void triggerUndo() {
        if (onUndo != null) onUndo.run();
    }

    public void triggerZoom(final double factor) {
        if (onZoom != null) onZoom.accept(factor);
    }
}

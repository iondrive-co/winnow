package org.win.model;

import javafx.scene.image.WritableImage;

import java.io.File;

/**
 * Result of an undo operation including the restored file reference and optional image snapshot.
 */
public record UndoSnapshot(File file, WritableImage image, File renamedFile) {
}

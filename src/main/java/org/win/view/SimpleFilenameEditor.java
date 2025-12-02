package org.win.view;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.File;

/**
 * Simple filename editor with a single text field for the filename
 * (without extension) and a non-editable label for the extension.
 */
public final class SimpleFilenameEditor extends HBox implements FilenameEditor {

    private final TextField filenameField;
    private final Label extensionLabel;

    public SimpleFilenameEditor(final File currentFile) {
        super(5);

        final String fullName = currentFile.getName();
        final int lastDotIndex = fullName.lastIndexOf('.');

        final String nameWithoutExt;
        final String extension;

        if (lastDotIndex > 0) {
            nameWithoutExt = fullName.substring(0, lastDotIndex);
            extension = fullName.substring(lastDotIndex);
        } else {
            nameWithoutExt = fullName;
            extension = "";
        }

        filenameField = new TextField(nameWithoutExt);
        filenameField.setPrefWidth(160);
        filenameField.setMinWidth(120);
        filenameField.setMaxWidth(200);

        extensionLabel = new Label(extension);
        extensionLabel.setStyle("-fx-text-fill: gray; -fx-padding: 5 0 0 5;");
        extensionLabel.setDisable(true);
        extensionLabel.setMinWidth(40);
        extensionLabel.setPrefWidth(60);
        extensionLabel.setMaxWidth(80);

        getChildren().addAll(filenameField, extensionLabel);
    }

    /**
     * Gets the complete filename with extension.
     *
     * @return the filename with extension
     */
    public String getFilename() {
        return filenameField.getText() + extensionLabel.getText();
    }

    @Override
    public void requestFocus() {
        filenameField.requestFocus();
    }
}

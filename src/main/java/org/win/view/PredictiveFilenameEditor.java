package org.win.view;

import clojure.lang.IPersistentMap;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.win.util.FilenameClassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Filename editor that parses the current filename into components
 * and displays each as an editable ComboBox with suggestions.
 */
public final class PredictiveFilenameEditor extends HBox implements FilenameEditor {

    private final FilenameClassifier.ParsedFilename parsedFilename;
    private final List<ComboBox<String>> componentFields = new ArrayList<>();
    private final List<TextField> separatorFields = new ArrayList<>();
    private final Label extensionLabel;

    public PredictiveFilenameEditor(final File currentFile, final IPersistentMap model) {
        super(2);

        final String filename = currentFile.getName();
        this.parsedFilename = FilenameClassifier.parseCurrentFilename(model, filename);

        // Create ComboBox for each component with separator TextField between them
        for (int i = 0; i < parsedFilename.components.size(); i++) {
            final FilenameClassifier.FilenameComponent component = parsedFilename.components.get(i);

            // Add ComboBox for component
            final ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setEditable(true);
            comboBox.setValue(component.currentValue);

            if (component.suggestions != null && !component.suggestions.isEmpty()) {
                comboBox.getItems().addAll(component.suggestions);
            }

            comboBox.setPrefWidth(150);
            componentFields.add(comboBox);
            getChildren().add(comboBox);

            // Add separator TextField if there's a separator for this component
            if (i < parsedFilename.separators.size()) {
                final String separator = parsedFilename.separators.get(i);
                final TextField separatorField = new TextField();
                separatorField.setText(separator != null ? separator : "");
                separatorField.setPrefWidth(30);
                separatorField.setStyle("-fx-padding: 5 2 0 2;");
                separatorFields.add(separatorField);
                getChildren().add(separatorField);
            }
        }

        // Add button to insert new component
        final Button addButton = new Button("+");
        addButton.setStyle("-fx-font-size: 12px; -fx-padding: 2 8 2 8;");
        addButton.setOnAction(e -> addNewComponent());
        getChildren().add(addButton);

        // Add uneditable extension label
        extensionLabel = new Label(parsedFilename.extension);
        extensionLabel.setStyle("-fx-text-fill: gray; -fx-padding: 5 0 0 5;");
        extensionLabel.setDisable(true);
        getChildren().add(extensionLabel);
    }

    private void addNewComponent() {
        // Create separator field with default separator
        final TextField separatorField = new TextField("-");
        separatorField.setPrefWidth(30);
        separatorField.setStyle("-fx-padding: 5 2 0 2;");
        separatorFields.add(separatorField);

        // Create new component combobox
        final ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setEditable(true);
        comboBox.setValue("");
        comboBox.setPrefWidth(150);
        componentFields.add(comboBox);

        // Insert before the last two children (+ button and extension label)
        final int insertIndex = getChildren().size() - 2;
        getChildren().add(insertIndex, separatorField);
        getChildren().add(insertIndex + 1, comboBox);

        // Focus the new component field
        comboBox.requestFocus();
    }

    /**
     * Reconstructs the filename from the current component values and separators.
     *
     * @return the reconstructed filename with extension
     */
    public String getFilename() {
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < componentFields.size(); i++) {
            result.append(componentFields.get(i).getValue());

            // Add separator if available
            if (i < separatorFields.size()) {
                result.append(separatorFields.get(i).getText());
            }
        }

        result.append(extensionLabel.getText());
        return result.toString();
    }

    @Override
    public void requestFocus() {
        if (!componentFields.isEmpty()) {
            componentFields.get(0).requestFocus();
        }
    }
}

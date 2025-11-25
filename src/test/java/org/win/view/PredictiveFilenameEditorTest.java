package org.win.view;

import clojure.lang.IPersistentMap;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.util.FilenameClassifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public final class PredictiveFilenameEditorTest {

    private static Path tempDir;
    private IPersistentMap model;

    @BeforeClass
    public static void initJavaFX() {
        new JFXPanel();
    }

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("editor-test");

        // Create test files to build model
        final List<String> testFiles = Arrays.asList(
            "1.small.jpg",
            "2.medium.jpg",
            "3.large.jpg"
        );

        for (final String filename : testFiles) {
            Files.createFile(tempDir.resolve(filename));
        }

        model = FilenameClassifier.buildModel(testFiles);
    }

    @Test
    public void testCreatesComboBoxesForComponents() throws Exception {
        final File testFile = tempDir.resolve("1.small.jpg").toFile();
        final CountDownLatch latch = new CountDownLatch(1);
        final PredictiveFilenameEditor[] editorHolder = new PredictiveFilenameEditor[1];

        Platform.runLater(() -> {
            editorHolder[0] = new PredictiveFilenameEditor(testFile, model);
            latch.countDown();
        });

        latch.await();

        final PredictiveFilenameEditor editor = editorHolder[0];

        // Should have ComboBoxes for components plus extension label
        assertThat(editor.getChildren().size()).isGreaterThan(1);

        // Last child should be the extension label
        assertThat(editor.getChildren().get(editor.getChildren().size() - 1))
            .isInstanceOf(Label.class);
    }

    @Test
    public void testExtensionDisplayedAsLabel() throws Exception {
        final File testFile = tempDir.resolve("2.medium.jpg").toFile();
        final CountDownLatch latch = new CountDownLatch(1);
        final PredictiveFilenameEditor[] editorHolder = new PredictiveFilenameEditor[1];

        Platform.runLater(() -> {
            editorHolder[0] = new PredictiveFilenameEditor(testFile, model);
            latch.countDown();
        });

        latch.await();

        final PredictiveFilenameEditor editor = editorHolder[0];
        final Label extensionLabel = (Label) editor.getChildren()
            .get(editor.getChildren().size() - 1);

        assertThat(extensionLabel.getText()).isEqualTo(".jpg");
        assertThat(extensionLabel.isDisabled()).isTrue();
    }

    @Test
    public void testComboBoxesAreEditable() throws Exception {
        final File testFile = tempDir.resolve("3.large.jpg").toFile();
        final CountDownLatch latch = new CountDownLatch(1);
        final PredictiveFilenameEditor[] editorHolder = new PredictiveFilenameEditor[1];

        Platform.runLater(() -> {
            editorHolder[0] = new PredictiveFilenameEditor(testFile, model);
            latch.countDown();
        });

        latch.await();

        final PredictiveFilenameEditor editor = editorHolder[0];

        // Check that ComboBoxes (all children except the last Label) are editable
        for (int i = 0; i < editor.getChildren().size() - 1; i++) {
            if (editor.getChildren().get(i) instanceof ComboBox) {
                final ComboBox<?> comboBox = (ComboBox<?>) editor.getChildren().get(i);
                assertThat(comboBox.isEditable()).isTrue();
            }
        }
    }

    @Test
    public void testReconstructsFilename() throws Exception {
        final File testFile = tempDir.resolve("1.small.jpg").toFile();
        final CountDownLatch latch = new CountDownLatch(1);
        final PredictiveFilenameEditor[] editorHolder = new PredictiveFilenameEditor[1];

        Platform.runLater(() -> {
            editorHolder[0] = new PredictiveFilenameEditor(testFile, model);
            latch.countDown();
        });

        latch.await();

        final PredictiveFilenameEditor editor = editorHolder[0];

        // Initial reconstruction should match original filename
        assertThat(editor.getFilename()).isEqualTo("1.small.jpg");
    }

    @Test
    public void testReconstructsWithModifiedValues() throws Exception {
        final File testFile = tempDir.resolve("1.small.jpg").toFile();
        final CountDownLatch latch = new CountDownLatch(1);
        final PredictiveFilenameEditor[] editorHolder = new PredictiveFilenameEditor[1];

        Platform.runLater(() -> {
            final PredictiveFilenameEditor editor = new PredictiveFilenameEditor(testFile, model);
            editorHolder[0] = editor;

            // Modify first ComboBox value
            if (editor.getChildren().get(0) instanceof ComboBox) {
                @SuppressWarnings("unchecked")
                final ComboBox<String> firstComboBox = (ComboBox<String>) editor.getChildren().get(0);
                firstComboBox.setValue("4");
            }

            latch.countDown();
        });

        latch.await();

        final PredictiveFilenameEditor editor = editorHolder[0];
        final String reconstructed = editor.getFilename();

        // Should reconstruct with modified value while keeping structure
        assertThat(reconstructed).startsWith("4");
        assertThat(reconstructed).endsWith(".jpg");
    }

    @Test
    public void testAddButtonInsertsNewComponent() throws Exception {
        final File testFile = tempDir.resolve("1.small.jpg").toFile();
        final CountDownLatch latch = new CountDownLatch(1);
        final PredictiveFilenameEditor[] editorHolder = new PredictiveFilenameEditor[1];
        final int[] initialChildCount = new int[1];

        Platform.runLater(() -> {
            final PredictiveFilenameEditor editor = new PredictiveFilenameEditor(testFile, model);
            editorHolder[0] = editor;
            initialChildCount[0] = editor.getChildren().size();

            // Find and click the "+" button (second to last child, before extension label)
            final Button addButton = (Button) editor.getChildren().get(editor.getChildren().size() - 2);
            assertThat(addButton.getText()).isEqualTo("+");
            addButton.fire();

            latch.countDown();
        });

        latch.await();

        final PredictiveFilenameEditor editor = editorHolder[0];

        // Should have added 2 more children (separator TextField + component ComboBox)
        assertThat(editor.getChildren().size()).isEqualTo(initialChildCount[0] + 2);

        // Verify second to last child is still the "+" button
        assertThat(editor.getChildren().get(editor.getChildren().size() - 2)).isInstanceOf(Button.class);

        // Verify last child is still the extension label
        assertThat(editor.getChildren().get(editor.getChildren().size() - 1)).isInstanceOf(Label.class);
    }

    @Test
    public void testAddedComponentAppearsInFilename() throws Exception {
        final File testFile = tempDir.resolve("1.small.jpg").toFile();
        final CountDownLatch latch = new CountDownLatch(1);
        final PredictiveFilenameEditor[] editorHolder = new PredictiveFilenameEditor[1];

        Platform.runLater(() -> {
            final PredictiveFilenameEditor editor = new PredictiveFilenameEditor(testFile, model);
            editorHolder[0] = editor;

            // Click the "+" button
            final Button addButton = (Button) editor.getChildren().get(editor.getChildren().size() - 2);
            addButton.fire();

            // Set value in the new component (find the newly added ComboBox)
            final int newComboBoxIndex = editor.getChildren().size() - 3; // Before button and label
            @SuppressWarnings("unchecked")
            final ComboBox<String> newComboBox = (ComboBox<String>) editor.getChildren().get(newComboBoxIndex);
            newComboBox.setValue("extra");

            // Find the separator TextField before the new component
            final TextField separatorField = (TextField) editor.getChildren().get(newComboBoxIndex - 1);
            separatorField.setText("_");

            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        final PredictiveFilenameEditor editor = editorHolder[0];
        final String reconstructed = editor.getFilename();

        // Should include the new component with custom separator
        assertThat(reconstructed).contains("_extra");
        assertThat(reconstructed).endsWith(".jpg");
    }
}

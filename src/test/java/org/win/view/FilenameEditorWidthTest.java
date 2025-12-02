package org.win.view;

import clojure.lang.PersistentArrayMap;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class FilenameEditorWidthTest {

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    @Test
    public void simpleEditorUsesCompactWidths() {
        final SimpleFilenameEditor editor = new SimpleFilenameEditor(new File("example-name.jpg"));
        final TextField field = (TextField) editor.getChildren().get(0);
        assertThat(field.getPrefWidth()).isLessThanOrEqualTo(260);
    }

    @Test
    public void predictiveEditorUsesCompactWidths() {
        final PredictiveFilenameEditor editor = new PredictiveFilenameEditor(
            new File("tokenized-name-01.jpg"),
            PersistentArrayMap.EMPTY
        );
        final ComboBox<?> first = (ComboBox<?>) editor.getChildren().get(0);
        assertThat(first.getPrefWidth()).isLessThanOrEqualTo(160);
    }
}

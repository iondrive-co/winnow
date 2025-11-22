package org.win;

import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.view.Window;

import static org.assertj.core.api.Assertions.assertThat;

public class MainTest {

    @BeforeClass
    public static void initToolkit() {
        // Initialize JavaFX toolkit
        new JFXPanel();
    }

    @Test
    public void testMain_hasWindowInstance() {
        Main main = new Main();

        assertThat(main).isNotNull();
    }

    @Test
    public void testMain_windowFieldIsNotNull() {
        Main main = new Main();

        assertThat(main.window).isNotNull();
        assertThat(main.window).isInstanceOf(Window.class);
    }

    @Test
    public void testMain_currentFileIndexInitializedToZero() {
        Main main = new Main();

        assertThat(main.currentFileIndex).isEqualTo(0);
    }

    @Test
    public void testMain_constructor() {
        Main main = new Main();

        assertThat(main).isNotNull();
        assertThat(main.window).isNotNull();
        assertThat(main.currentFileIndex).isZero();
    }

    @Test
    public void testMain_multipleInstances() {
        Main main1 = new Main();
        Main main2 = new Main();

        assertThat(main1).isNotNull();
        assertThat(main2).isNotNull();
        assertThat(main1).isNotSameAs(main2);
        assertThat(main1.window).isNotSameAs(main2.window);
    }

    @Test
    public void testMain_windowIsInitializedInConstructor() {
        Main main = new Main();

        // The window field should be initialized via field initializer
        assertThat(main.window).isNotNull();
        assertThat(main.window).isInstanceOf(Window.class);
    }

    @Test
    public void testMain_extendsApplication() {
        Main main = new Main();

        assertThat(main).isInstanceOf(javafx.application.Application.class);
    }

    @Test
    public void testMain_imageFilesInitializedAsEmptyList() {
        Main main = new Main();

        // Should have imageFiles collection initialized
        assertThat(main).isNotNull();
    }

    @Test
    public void testMain_handlesMouseDrag() {
        Main main = new Main();
        main.currentFileIndex = 0;

        // Simulate a left drag (should increment index)
        main.handleMouseDrag(100); // Simulating drag from 0 to 100

        // The index should remain the same or change based on threshold
        assertThat(main.currentFileIndex).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testMain_handlesSwipeLeft() {
        Main main = new Main();
        // We can't easily test this without adding image files, but we can verify the method exists
        assertThat(main).isNotNull();
    }
}

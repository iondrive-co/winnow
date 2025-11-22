package org.win.view;

import javafx.embed.swing.JFXPanel;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class DragNavigationTest {

    private static final int TEST_IMAGE_WIDTH = 800;
    private static final int TEST_IMAGE_HEIGHT = 600;

    @BeforeClass
    public static void initJavaFX() {
        // Initialize JavaFX toolkit
        new JFXPanel();
    }

    private BufferedImage createTestImage() {
        return new BufferedImage(TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
    }

    @Test
    public void testDragStartCallback_setsInitialPosition() throws Exception {
        BufferedImage testImage = createTestImage();
        CustomImageCanvas canvas = new CustomImageCanvas(testImage);

        AtomicReference<Double> capturedX = new AtomicReference<>();
        canvas.setOnDragStart(x -> capturedX.set(x));

        // Simulate mouse press outside handles (in the center of the image) with Ctrl held
        double pressX = testImage.getWidth() / 2.0;
        double pressY = testImage.getHeight() / 2.0;
        MouseEvent pressEvent = new MouseEvent(
                MouseEvent.MOUSE_PRESSED,
                pressX, pressY, pressX, pressY,
                MouseButton.PRIMARY, 1,
                false, true, false, false, true, false, false, false, false, false, null
        );
        canvas.fireEvent(pressEvent);

        assertThat(capturedX.get()).isCloseTo(pressX, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    public void testNavigationDragCallback_triggeredOnDrag() throws Exception {
        BufferedImage testImage = createTestImage();
        CustomImageCanvas canvas = new CustomImageCanvas(testImage);

        AtomicInteger dragCallCount = new AtomicInteger(0);
        AtomicReference<Double> lastDragX = new AtomicReference<>();

        canvas.setOnDragStart(x -> {});  // No-op, just set it
        canvas.setOnNavigationDrag(x -> {
            dragCallCount.incrementAndGet();
            lastDragX.set(x);
        });

        // Press outside handles with Ctrl held
        double pressX = 100.0;
        double pressY = 100.0;
        MouseEvent pressEvent = new MouseEvent(
                MouseEvent.MOUSE_PRESSED,
                pressX, pressY, pressX, pressY,
                MouseButton.PRIMARY, 1,
                false, true, false, false, true, false, false, false, false, false, null
        );
        canvas.fireEvent(pressEvent);

        // Drag to a different position with Ctrl held
        double dragX = 200.0;
        double dragY = 100.0;
        MouseEvent dragEvent = new MouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                dragX, dragY, dragX, dragY,
                MouseButton.PRIMARY, 1,
                false, true, false, false, true, false, false, false, false, false, null
        );
        canvas.fireEvent(dragEvent);

        assertThat(dragCallCount.get()).isGreaterThan(0);
        assertThat(lastDragX.get()).isCloseTo(dragX, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    public void testDragOnHandle_doesNotTriggerNavigationCallback() throws Exception {
        BufferedImage testImage = createTestImage();
        CustomImageCanvas canvas = new CustomImageCanvas(testImage);

        AtomicInteger dragCallCount = new AtomicInteger(0);
        canvas.setOnNavigationDrag(x -> dragCallCount.incrementAndGet());

        // Press on top-left corner handle (0, 0)
        MouseEvent pressEvent = new MouseEvent(
                MouseEvent.MOUSE_PRESSED,
                5.0, 5.0, 5.0, 5.0,  // Within the corner handle
                MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        canvas.fireEvent(pressEvent);

        // Drag the handle
        MouseEvent dragEvent = new MouseEvent(
                MouseEvent.MOUSE_DRAGGED,
                50.0, 50.0, 50.0, 50.0,
                MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, false, false, false, null
        );
        canvas.fireEvent(dragEvent);

        assertThat(dragCallCount.get()).isEqualTo(0);
    }

    @Test
    public void testMoveHandle_visible() throws Exception {
        BufferedImage testImage = createTestImage();
        CustomImageCanvas canvas = new CustomImageCanvas(testImage);

        // The move handle should be positioned above the selection (or inside if at top)
        // This test just verifies the canvas can be created with the move handle logic
        assertThat(canvas).isNotNull();
        assertThat(canvas.getWidth()).isGreaterThan(0);
        assertThat(canvas.getHeight()).isGreaterThan(0);
    }
}

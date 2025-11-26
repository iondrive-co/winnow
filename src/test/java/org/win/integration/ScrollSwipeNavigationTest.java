package org.win.integration;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.input.ScrollEvent;
import org.junit.BeforeClass;
import org.junit.Test;
import org.win.control.InputDispatcher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for scroll and swipe navigation gestures.
 *
 * Tests cover:
 * - Scroll event handling for zoom (non-inertia scroll)
 * - Inertia scroll for swipe navigation (touchpad)
 * - Swipe left/right navigation
 * - Zoom in/out with scroll wheel
 */
public class ScrollSwipeNavigationTest {

    @BeforeClass
    public static void initToolkit() {
        new JFXPanel();
    }

    private void waitForFXThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testScrollEvent_nonInertia_triggersZoom() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Double> capturedZoomFactor = new AtomicReference<>();

        Platform.runLater(() -> {
            final Scene scene = new Scene(new javafx.scene.layout.StackPane(), 400, 300);
            final InputDispatcher dispatcher = new InputDispatcher()
                    .zoomAction(capturedZoomFactor::set)
                    .makeSceneBindings(scene);

            // Simulate scroll up (zoom in)
            final ScrollEvent scrollUpEvent = new ScrollEvent(
                    ScrollEvent.SCROLL,
                    100, 100, 100, 100,
                    false, false, false, false,
                    false, false,
                    0, -10, // deltaX, deltaY (negative = up = zoom in)
                    0, 0,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                    0,
                    null
            );

            scene.getOnScroll().handle(scrollUpEvent);

            // Should trigger zoom in (factor 1.1)
            assertThat(capturedZoomFactor.get()).isEqualTo(1.1);

            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testScrollEvent_scrollDown_triggersZoomOut() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Double> capturedZoomFactor = new AtomicReference<>();

        Platform.runLater(() -> {
            final Scene scene = new Scene(new javafx.scene.layout.StackPane(), 400, 300);
            final InputDispatcher dispatcher = new InputDispatcher()
                    .zoomAction(capturedZoomFactor::set)
                    .makeSceneBindings(scene);

            // Simulate scroll down (zoom out)
            final ScrollEvent scrollDownEvent = new ScrollEvent(
                    ScrollEvent.SCROLL,
                    100, 100, 100, 100,
                    false, false, false, false,
                    false, false,
                    0, 10, // deltaX, deltaY (positive = down = zoom out)
                    0, 0,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                    0,
                    null
            );

            scene.getOnScroll().handle(scrollDownEvent);

            // Should trigger zoom out (factor 0.9)
            assertThat(capturedZoomFactor.get()).isEqualTo(0.9);

            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testScrollEvent_inertia_triggersSwipe() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Double> capturedSwipeDelta = new AtomicReference<>();

        Platform.runLater(() -> {
            final Scene scene = new Scene(new javafx.scene.layout.StackPane(), 400, 300);
            final InputDispatcher dispatcher = new InputDispatcher();
            dispatcher.addSwipeHandler(scene, capturedSwipeDelta::set, null);

            // Simulate inertia scroll (swipe)
            final ScrollEvent inertiaEvent = new ScrollEvent(
                    ScrollEvent.SCROLL,
                    100, 100, 100, 100,
                    false, false, false, false,
                    false, true, // inertia = true
                    50, 0, // deltaX, deltaY (horizontal swipe)
                    0, 0,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                    0,
                    null
            );

            scene.getOnScroll().handle(inertiaEvent);

            // Should trigger swipe handler with deltaX
            assertThat(capturedSwipeDelta.get()).isEqualTo(50.0);

            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testScrollEvent_withZeroDelta_ignored() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger zoomCallCount = new AtomicInteger(0);

        Platform.runLater(() -> {
            final Scene scene = new Scene(new javafx.scene.layout.StackPane(), 400, 300);
            final InputDispatcher dispatcher = new InputDispatcher()
                    .zoomAction(factor -> zoomCallCount.incrementAndGet())
                    .makeSceneBindings(scene);

            // Simulate scroll with zero deltaY
            final ScrollEvent zeroEvent = new ScrollEvent(
                    ScrollEvent.SCROLL,
                    100, 100, 100, 100,
                    false, false, false, false,
                    false, false,
                    0, 0, // deltaX, deltaY (zero)
                    0, 0,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                    0,
                    null
            );

            scene.getOnScroll().handle(zeroEvent);

            // Should not trigger zoom
            assertThat(zoomCallCount.get()).isEqualTo(0);

            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testSwipeLeftRight_navigationDirection() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Double> capturedSwipeDelta = new AtomicReference<>();

        Platform.runLater(() -> {
            final Scene scene = new Scene(new javafx.scene.layout.StackPane(), 400, 300);
            final InputDispatcher dispatcher = new InputDispatcher();
            dispatcher.addSwipeHandler(scene, capturedSwipeDelta::set, null);

            // Test swipe right (positive deltaX) - should go to previous image
            final ScrollEvent swipeRightEvent = new ScrollEvent(
                    ScrollEvent.SCROLL,
                    100, 100, 100, 100,
                    false, false, false, false,
                    false, true, // inertia = true
                    100, 0,
                    0, 0,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                    0,
                    null
            );

            scene.getOnScroll().handle(swipeRightEvent);
            assertThat(capturedSwipeDelta.get()).isEqualTo(100.0);

            // Test swipe left (negative deltaX) - should go to next image
            final ScrollEvent swipeLeftEvent = new ScrollEvent(
                    ScrollEvent.SCROLL,
                    100, 100, 100, 100,
                    false, false, false, false,
                    false, true, // inertia = true
                    -100, 0,
                    0, 0,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                    0,
                    null
            );

            scene.getOnScroll().handle(swipeLeftEvent);
            assertThat(capturedSwipeDelta.get()).isEqualTo(-100.0);

            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testInputDispatcher_supportsZoomAndSwipe() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Double> zoomFactor = new AtomicReference<>();
        final AtomicReference<Double> swipeDelta = new AtomicReference<>();

        Platform.runLater(() -> {
            final Scene scene = new Scene(new javafx.scene.layout.StackPane(), 400, 300);
            final InputDispatcher dispatcher = new InputDispatcher()
                    .zoomAction(zoomFactor::set);
            dispatcher.addSwipeHandler(scene, swipeDelta::set, null);

            // Test zoom
            final ScrollEvent scrollEvent = new ScrollEvent(
                    ScrollEvent.SCROLL,
                    100, 100, 100, 100,
                    false, false, false, false,
                    false, false,
                    0, -10,
                    0, 0,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                    0,
                    null
            );
            scene.getOnScroll().handle(scrollEvent);
            assertThat(zoomFactor.get()).isEqualTo(1.1);

            // Test swipe
            final ScrollEvent swipeEvent = new ScrollEvent(
                    ScrollEvent.SCROLL,
                    100, 100, 100, 100,
                    false, false, false, false,
                    false, true,
                    50, 0,
                    0, 0,
                    ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                    ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                    0,
                    null
            );
            scene.getOnScroll().handle(swipeEvent);
            assertThat(swipeDelta.get()).isEqualTo(50.0);

            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }
}

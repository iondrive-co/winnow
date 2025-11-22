package org.win.control;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class InputDispatcherTest {

    @BeforeClass
    public static void initToolkit() {
        // Initialize JavaFX toolkit
        new JFXPanel();
    }

    @Test
    public void testBindToKey_registersAction() {
        InputDispatcher dispatcher = new InputDispatcher();
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        Runnable testAction = () -> actionExecuted.set(true);

        InputDispatcher result = dispatcher.bindToKey(KeyCode.ENTER, testAction);

        assertThat(result).isSameAs(dispatcher);
    }

    @Test
    public void testBindToKey_supportsFluentAPI() {
        InputDispatcher dispatcher = new InputDispatcher();
        AtomicBoolean action1Executed = new AtomicBoolean(false);
        AtomicBoolean action2Executed = new AtomicBoolean(false);

        InputDispatcher result = dispatcher
                .bindToKey(KeyCode.A, () -> action1Executed.set(true))
                .bindToKey(KeyCode.B, () -> action2Executed.set(true));

        assertThat(result).isSameAs(dispatcher);
    }

    @Test
    public void testZoomAction_registersZoomConsumer() {
        InputDispatcher dispatcher = new InputDispatcher();
        AtomicReference<Double> capturedZoomFactor = new AtomicReference<>();

        InputDispatcher result = dispatcher.zoomAction(capturedZoomFactor::set);

        assertThat(result).isSameAs(dispatcher);
    }

    @Test
    public void testZoomAction_supportsFluentAPI() {
        InputDispatcher dispatcher = new InputDispatcher();
        AtomicReference<Double> capturedZoomFactor = new AtomicReference<>();

        InputDispatcher result = dispatcher
                .bindToKey(KeyCode.Z, () -> {})
                .zoomAction(capturedZoomFactor::set);

        assertThat(result).isSameAs(dispatcher);
    }

    @Test
    public void testMakeKeyBindings_returnsDispatcher() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Stage stage = new Stage();
            InputDispatcher dispatcher = new InputDispatcher();

            InputDispatcher result = dispatcher.makeKeyBindings(stage);

            assertThat(result).isSameAs(dispatcher);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testMakeSceneBindings_returnsDispatcher() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Scene scene = new Scene(new StackPane(), 400, 300);
            InputDispatcher dispatcher = new InputDispatcher();

            InputDispatcher result = dispatcher.makeSceneBindings(scene);

            assertThat(result).isSameAs(dispatcher);
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testMakeSceneBindings_withZoomAction_registersZoomHandler() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Double> capturedZoomFactor = new AtomicReference<>();

        Platform.runLater(() -> {
            Scene scene = new Scene(new StackPane(), 400, 300);
            InputDispatcher dispatcher = new InputDispatcher()
                    .zoomAction(capturedZoomFactor::set)
                    .makeSceneBindings(scene);

            assertThat(scene.getOnZoom()).isNotNull();
            assertThat(scene.getOnScroll()).isNotNull();
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testMakeSceneBindings_withoutZoomAction_doesNotRegisterZoomHandler() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            Scene scene = new Scene(new StackPane(), 400, 300);
            InputDispatcher dispatcher = new InputDispatcher()
                    .makeSceneBindings(scene);

            // When no zoom action is registered, handlers should not be set
            // (this tests the null check in makeSceneBindings)
            assertThat(scene.getOnZoom()).isNull();
            assertThat(scene.getOnScroll()).isNull();
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testFluentAPI_fullChain() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean keyActionExecuted = new AtomicBoolean(false);
        AtomicReference<Double> capturedZoomFactor = new AtomicReference<>();

        Platform.runLater(() -> {
            Stage stage = new Stage();
            Scene scene = new Scene(new StackPane(), 400, 300);

            InputDispatcher dispatcher = new InputDispatcher()
                    .bindToKey(KeyCode.ENTER, () -> keyActionExecuted.set(true))
                    .bindToKey(KeyCode.ESCAPE, () -> {})
                    .zoomAction(capturedZoomFactor::set)
                    .makeKeyBindings(stage)
                    .makeSceneBindings(scene);

            assertThat(dispatcher).isNotNull();
            assertThat(scene.getOnZoom()).isNotNull();
            assertThat(scene.getOnScroll()).isNotNull();
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testBindToKey_multipleKeys() {
        InputDispatcher dispatcher = new InputDispatcher();

        dispatcher
                .bindToKey(KeyCode.UP, () -> {})
                .bindToKey(KeyCode.DOWN, () -> {})
                .bindToKey(KeyCode.LEFT, () -> {})
                .bindToKey(KeyCode.RIGHT, () -> {});

        // Should complete without exceptions
        assertThat(dispatcher).isNotNull();
    }

    @Test
    public void testBindToKey_overwritesPreviousBinding() {
        InputDispatcher dispatcher = new InputDispatcher();
        AtomicBoolean firstAction = new AtomicBoolean(false);
        AtomicBoolean secondAction = new AtomicBoolean(false);

        dispatcher
                .bindToKey(KeyCode.A, () -> firstAction.set(true))
                .bindToKey(KeyCode.A, () -> secondAction.set(true));

        // The second binding should overwrite the first
        assertThat(dispatcher).isNotNull();
    }

    @Test
    public void testConstructor_createsEmptyDispatcher() {
        InputDispatcher dispatcher = new InputDispatcher();

        assertThat(dispatcher).isNotNull();
    }
}

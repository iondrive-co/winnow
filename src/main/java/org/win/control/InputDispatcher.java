package org.win.control;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleConsumer;

public class InputDispatcher {

    private final EnumMap<KeyCode, Runnable> keyActions;
    private final Map<KeyCombination, Runnable> keyCombinationActions;
    private DoubleConsumer zoomConsumer;
    private DoubleConsumer swipeHandler;

    public InputDispatcher() {
        this.keyActions = new EnumMap<>(KeyCode.class);
        this.keyCombinationActions = new HashMap<>();
    }

    public InputDispatcher bindToKey(final KeyCode key, final Runnable action) {
        keyActions.put(key, action);
        return this;
    }

    public InputDispatcher bindToKeyCombination(final KeyCode key, final boolean ctrl, final boolean shift, final boolean alt, final Runnable action) {
        keyCombinationActions.put(new KeyCombination(key, ctrl, shift, alt), action);
        return this;
    }

    public InputDispatcher zoomAction(final DoubleConsumer zoomConsumer) {
        this.zoomConsumer = zoomConsumer;
        return this;
    }

    public InputDispatcher addSwipeHandler(final Scene scene, final DoubleConsumer handler, final Stage stage) {
        this.swipeHandler = handler;
        // Only set scroll handler here if it hasn't been set already by makeSceneBindings
        if (scene.getOnScroll() == null) {
            scene.setOnScroll(this::handleScroll);
        }
        return this;
    }

    private void handleScroll(final ScrollEvent event) {
        if (event.isInertia()) {
            // Touchpad swipe detected (inertia scroll)
            double deltaX = event.getDeltaX();
            if (swipeHandler != null) {
                swipeHandler.accept(deltaX);
            }
            event.consume();
        } else if (zoomConsumer != null) {
            double deltaY = event.getDeltaY();

            // Ignore scroll events with deltaY == 0 (they don't carry direction information)
            if (deltaY == 0) {
                event.consume();
                return;
            }

            // Positive deltaY = scroll down (away from user) = zoom out (smaller)
            // Negative deltaY = scroll up (toward user) = zoom in (larger)
            double zoomFactor = deltaY < 0 ? 1.1 : 0.9;
            zoomConsumer.accept(zoomFactor);
            event.consume();
        }
    }

    public InputDispatcher makeKeyBindings(final Stage primaryStage) {
        primaryStage.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Check key combinations first (more specific)
            KeyCombination combo = new KeyCombination(event.getCode(), event.isControlDown(), event.isShiftDown(), event.isAltDown());
            Runnable comboAction = keyCombinationActions.get(combo);
            if (comboAction != null) {
                comboAction.run();
                event.consume();
                return;
            }

            // Then check simple key bindings (only if no modifiers are pressed)
            if (!event.isControlDown() && !event.isShiftDown() && !event.isAltDown()) {
                Runnable action = keyActions.get(event.getCode());
                if (action != null) {
                    action.run();
                    event.consume();
                }
            }
        });
        return this;
    }

    public InputDispatcher makeSceneBindings(final Scene scene) {
        if (zoomConsumer != null) {
            scene.setOnZoom(event -> {
                double zoomFactor = event.getZoomFactor();
                zoomConsumer.accept(zoomFactor);
                event.consume();
            });
            scene.setOnScroll(this::handleScroll);
        }
        return this;
    }

    private static class KeyCombination {
        private final KeyCode keyCode;
        private final boolean ctrl;
        private final boolean shift;
        private final boolean alt;

        public KeyCombination(KeyCode keyCode, boolean ctrl, boolean shift, boolean alt) {
            this.keyCode = keyCode;
            this.ctrl = ctrl;
            this.shift = shift;
            this.alt = alt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyCombination that = (KeyCombination) o;
            return ctrl == that.ctrl && shift == that.shift && alt == that.alt && keyCode == that.keyCode;
        }

        @Override
        public int hashCode() {
            int result = keyCode.hashCode();
            result = 31 * result + (ctrl ? 1 : 0);
            result = 31 * result + (shift ? 1 : 0);
            result = 31 * result + (alt ? 1 : 0);
            return result;
        }
    }
}

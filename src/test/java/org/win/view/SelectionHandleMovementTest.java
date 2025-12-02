package org.win.view;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class SelectionHandleMovementTest {

    @BeforeClass
    public static void initJavaFX() {
        new JFXPanel();
    }

    @Test
    public void moveHandleDragsSelection() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Double> newLeft = new AtomicReference<>(0.0);

        Platform.runLater(() -> {
            final BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
            final CustomImageCanvas canvas = new CustomImageCanvas(img);
            canvas.setSelectionRegion(100, 100, 400, 350);
            final Stage stage = new Stage();
            stage.setScene(new Scene(new Group(canvas), 600, 400));
            stage.show();

            final double handleX = canvas.getWidth() / 2.0;
            final double handleY = 20.0;

            canvas.fireEvent(new MouseEvent(MouseEvent.MOUSE_PRESSED, handleX, handleY, handleX, handleY,
                    MouseButton.PRIMARY, 1, false, false, false, false, true,
                    false, false, false, false, false, null));

            final double dragX = handleX + 60;
            final double dragY = handleY + 30;
            canvas.fireEvent(new MouseEvent(MouseEvent.MOUSE_DRAGGED, dragX, dragY, dragX, dragY,
                    MouseButton.PRIMARY, 1, false, false, false, false, true,
                    false, false, false, false, false, null));

            newLeft.set(canvas.getSelectionLeft());
            stage.close();
            latch.countDown();
        });

        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(newLeft.get()).isGreaterThan(0.0);
    }
}

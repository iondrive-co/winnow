package org.win.view;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleFilenameEditorTest {

    @BeforeClass
    public static void initToolkit() {
        Assume.assumeFalse("Skipping JavaFX test in headless environment",
            GraphicsEnvironment.isHeadless());
        new JFXPanel();
    }

    private void waitForFXThread() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    public void testConstructor_parsesFilename() throws Exception {
        final File testFile = new File("/test/path/test-image.jpg");
        final AtomicReference<String> filename = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final SimpleFilenameEditor editor = new SimpleFilenameEditor(testFile);
            filename.set(editor.getFilename());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(filename.get()).isEqualTo("test-image.jpg");
    }

    @Test
    public void testGetFilename_returnsNameWithExtension() throws Exception {
        final File testFile = new File("/test/path/photo.png");
        final AtomicReference<String> filename = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final SimpleFilenameEditor editor = new SimpleFilenameEditor(testFile);
            filename.set(editor.getFilename());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(filename.get()).isEqualTo("photo.png");
    }

    @Test
    public void testGetFilename_handlesFileWithoutExtension() throws Exception {
        final File testFile = new File("/test/path/noextension");
        final AtomicReference<String> filename = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final SimpleFilenameEditor editor = new SimpleFilenameEditor(testFile);
            filename.set(editor.getFilename());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(filename.get()).isEqualTo("noextension");
    }

    @Test
    public void testGetFilename_handlesMultipleDots() throws Exception {
        final File testFile = new File("/test/path/my.photo.final.jpg");
        final AtomicReference<String> filename = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final SimpleFilenameEditor editor = new SimpleFilenameEditor(testFile);
            filename.set(editor.getFilename());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(filename.get()).isEqualTo("my.photo.final.jpg");
    }

    @Test
    public void testGetFilename_withDotAndSpace() throws Exception {
        final File testFile = new File("/test/path/1. small.jpg");
        final AtomicReference<String> filename = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final SimpleFilenameEditor editor = new SimpleFilenameEditor(testFile);
            filename.set(editor.getFilename());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(filename.get()).isEqualTo("1. small.jpg");
    }

    @Test
    public void testGetFilename_withDash() throws Exception {
        final File testFile = new File("/test/path/photo-001.jpg");
        final AtomicReference<String> filename = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final SimpleFilenameEditor editor = new SimpleFilenameEditor(testFile);
            filename.set(editor.getFilename());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(filename.get()).isEqualTo("photo-001.jpg");
    }

    @Test
    public void testGetFilename_withUnderscore() throws Exception {
        final File testFile = new File("/test/path/image_001.jpg");
        final AtomicReference<String> filename = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final SimpleFilenameEditor editor = new SimpleFilenameEditor(testFile);
            filename.set(editor.getFilename());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(filename.get()).isEqualTo("image_001.jpg");
    }

    @Test
    public void testGetFilename_withSpaces() throws Exception {
        final File testFile = new File("/test/path/My Photo 001.jpg");
        final AtomicReference<String> filename = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final SimpleFilenameEditor editor = new SimpleFilenameEditor(testFile);
            filename.set(editor.getFilename());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(filename.get()).isEqualTo("My Photo 001.jpg");
    }

    @Test
    public void testGetFilename_withMixedSeparators() throws Exception {
        final File testFile = new File("/test/path/2024-01-15_photo_001.jpg");
        final AtomicReference<String> filename = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            final SimpleFilenameEditor editor = new SimpleFilenameEditor(testFile);
            filename.set(editor.getFilename());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(filename.get()).isEqualTo("2024-01-15_photo_001.jpg");
    }
}

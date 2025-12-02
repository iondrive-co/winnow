package org.win.web;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards key layout decisions for the web UI so selection overlays stay aligned.
 */
public class WebLayoutConfigTest {

    @Test
    public void canvasHostIsInlineAndMoveHandleCapturesEvents() throws Exception {
        final String source = Files.readString(Path.of("src/web/java/org/win/browser/BrowserMain.java"));
        assertThat(source).contains(".canvas-host { position: relative; background: #000; border: 1px solid #1f2430; border-radius: 10px; overflow: hidden; display: inline-block;");
        assertThat(source).contains("selectionHandles.add(createHandle(\"move\"))");
        assertThat(source).contains("handle.getStyle().setProperty(\"pointer-events\", \"move\".equals(position) ? \"auto\" : \"none\")");
    }
}

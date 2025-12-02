package org.win.build;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against regressing the README screenshot task configuration.
 */
public class GenerateScreenshotConfigTest {

    @Test
    public void generateScreenshotUsesOnlyJavafxModulePath() throws Exception {
        final String buildGradle = Files.readString(Path.of("build.gradle"));
        assertThat(buildGradle).contains("tasks.register('generateScreenshot'");
        assertThat(buildGradle).contains("--module-path");
        assertThat(buildGradle).contains("fxJars = configurations.runtimeClasspath.filter { it.name.startsWith('javafx-') }");
    }
}

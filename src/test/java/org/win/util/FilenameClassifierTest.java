package org.win.util;

import clojure.lang.IPersistentMap;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FilenameClassifierTest {

    @Test
    public void testParseFilename_withDotSpaceSeparator() {
        final List<String> filenames = Arrays.asList("1. small.jpg", "2. medium.jpg", "3. large.jpg");
        final IPersistentMap model = FilenameClassifier.buildModel(filenames);

        final FilenameClassifier.ParsedFilename parsed = FilenameClassifier.parseCurrentFilename(model, "1. small.jpg");

        System.out.println("Components: " + parsed.components.size());
        for (int i = 0; i < parsed.components.size(); i++) {
            System.out.println("  [" + i + "]: '" + parsed.components.get(i).currentValue + "'");
        }

        System.out.println("Separators: " + parsed.separators.size());
        for (int i = 0; i < parsed.separators.size(); i++) {
            System.out.println("  [" + i + "]: '" + parsed.separators.get(i) + "'");
        }

        System.out.println("Extension: '" + parsed.extension + "'");

        assertThat(parsed.components).hasSize(2);
        assertThat(parsed.components.get(0).currentValue).isEqualTo("1");
        assertThat(parsed.components.get(1).currentValue).isEqualTo("small");
        assertThat(parsed.separators).hasSize(1);
        assertThat(parsed.separators.get(0)).isEqualTo(". ");
        assertThat(parsed.extension).isEqualTo(".jpg");
    }

    @Test
    public void testParseFilename_withDashSeparator() {
        final List<String> filenames = Arrays.asList("photo-001.jpg", "photo-002.jpg", "photo-003.jpg");
        final IPersistentMap model = FilenameClassifier.buildModel(filenames);

        final FilenameClassifier.ParsedFilename parsed = FilenameClassifier.parseCurrentFilename(model, "photo-001.jpg");

        System.out.println("Dash test - Components: " + parsed.components.size());
        for (int i = 0; i < parsed.components.size(); i++) {
            System.out.println("  [" + i + "]: '" + parsed.components.get(i).currentValue + "'");
        }

        System.out.println("Dash test - Separators: " + parsed.separators.size());
        for (int i = 0; i < parsed.separators.size(); i++) {
            System.out.println("  [" + i + "]: '" + parsed.separators.get(i) + "'");
        }

        assertThat(parsed.components).hasSize(2);
        assertThat(parsed.separators).hasSize(1);
        assertThat(parsed.separators.get(0)).isEqualTo("-");
    }

    @Test
    public void testParseFilename_withUnderscoreSeparator() {
        final List<String> filenames = Arrays.asList("image_001.jpg", "image_002.jpg", "image_003.jpg");
        final IPersistentMap model = FilenameClassifier.buildModel(filenames);

        final FilenameClassifier.ParsedFilename parsed = FilenameClassifier.parseCurrentFilename(model, "image_001.jpg");

        assertThat(parsed.components).hasSize(2);
        assertThat(parsed.separators).hasSize(1);
        assertThat(parsed.separators.get(0)).isEqualTo("_");
    }

    @Test
    public void testParseFilename_withMultipleSeparators() {
        final List<String> filenames = Arrays.asList("2024-01-15_photo.jpg", "2024-01-16_photo.jpg");
        final IPersistentMap model = FilenameClassifier.buildModel(filenames);

        final FilenameClassifier.ParsedFilename parsed = FilenameClassifier.parseCurrentFilename(model, "2024-01-15_photo.jpg");

        System.out.println("Multi-separator test - Components: " + parsed.components.size());
        for (int i = 0; i < parsed.components.size(); i++) {
            System.out.println("  [" + i + "]: '" + parsed.components.get(i).currentValue + "'");
        }

        System.out.println("Multi-separator test - Separators: " + parsed.separators.size());
        for (int i = 0; i < parsed.separators.size(); i++) {
            System.out.println("  [" + i + "]: '" + parsed.separators.get(i) + "'");
        }

        assertThat(parsed.components.size()).isGreaterThan(0);
        assertThat(parsed.separators.size()).isGreaterThan(0);
    }

    @Test
    public void testReconstructWith_preservesSeparators() {
        final List<String> filenames = Arrays.asList("1. small.jpg", "2. medium.jpg");
        final IPersistentMap model = FilenameClassifier.buildModel(filenames);

        final FilenameClassifier.ParsedFilename parsed = FilenameClassifier.parseCurrentFilename(model, "1. small.jpg");

        final List<String> newValues = Arrays.asList("5", "large");
        final String reconstructed = parsed.reconstructWith(newValues);

        assertThat(reconstructed).isEqualTo("5. large.jpg");
    }
}

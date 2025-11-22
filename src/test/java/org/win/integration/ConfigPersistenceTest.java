package org.win.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.win.ConfigManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ConfigManager persistence across multiple operations.
 * These tests verify that configuration changes are properly saved and loaded
 * in realistic usage scenarios.
 */
public class ConfigPersistenceTest {

    private Path tempConfigFile;
    private String originalUserHome;

    @Before
    public void setUp() throws IOException {
        // Create a temporary directory to act as home
        Path tempDir = Files.createTempDirectory("winnow-integration-test");
        tempConfigFile = tempDir.resolve(".winnow.conf");

        // Override user.home system property
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());
    }

    @After
    public void tearDown() throws IOException {
        // Restore original user.home
        System.setProperty("user.home", originalUserHome);

        // Clean up temp config file and directory
        if (Files.exists(tempConfigFile)) {
            Files.delete(tempConfigFile);
        }
        Path tempDir = tempConfigFile.getParent();
        if (Files.exists(tempDir)) {
            Files.delete(tempDir);
        }
    }

    @Test
    public void testFullWorkflow_selectDirectory_navigateImages_persist() throws IOException {
        // Simulate user workflow: select directory, navigate images, restart app

        // Step 1: First launch - user selects directory
        ConfigManager config1 = new ConfigManager();
        config1.setLastDirectory("C:\\Users\\test\\Pictures\\Vacation");
        config1.setLastPosition(0);

        // Step 2: User navigates to image 5
        config1.setLastPosition(5);

        // Step 3: User closes app and reopens - config should be persisted
        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEqualTo("C:\\Users\\test\\Pictures\\Vacation");
        assertThat(config2.getLastPosition()).isEqualTo(5);

        // Step 4: User navigates to image 10
        config2.setLastPosition(10);

        // Step 5: User closes and reopens again
        ConfigManager config3 = new ConfigManager();
        assertThat(config3.getLastDirectory()).isEqualTo("C:\\Users\\test\\Pictures\\Vacation");
        assertThat(config3.getLastPosition()).isEqualTo(10);
    }

    @Test
    public void testDirectoryChange_resetsPosition() throws IOException {
        // When user switches directories, position should start from what's saved

        ConfigManager config1 = new ConfigManager();
        config1.setLastDirectory("/home/user/photos/summer");
        config1.setLastPosition(15);

        // User switches to different directory
        config1.setLastDirectory("/home/user/photos/winter");

        // Position should still be 15 (global position)
        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(15);
    }

    @Test
    public void testConfigFile_survivesMalformedPosition() throws IOException {
        // Create valid config
        ConfigManager config1 = new ConfigManager();
        config1.setLastDirectory("/test/path");
        config1.setLastPosition(5);

        // Manually corrupt the position value
        List<String> lines = Files.readAllLines(tempConfigFile);
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("lastPosition=")) {
                lines.set(i, "lastPosition=notanumber");
            }
        }
        Files.write(tempConfigFile, lines);

        // Load config - should handle gracefully
        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEqualTo("/test/path");
        assertThat(config2.getLastPosition()).isEqualTo(0); // Default value
    }

    @Test
    public void testConfigFile_handlesComplexPaths() throws IOException {
        // Test various complex path scenarios
        String[] complexPaths = {
            "C:\\Users\\test\\Documents\\My Pictures\\Summer 2024",
            "/home/user/Photos/Vacation (2024)/Day 1",
            "\\\\network\\share\\images",
            "/mnt/storage/photos & videos"
        };

        for (String path : complexPaths) {
            ConfigManager config1 = new ConfigManager();
            config1.setLastDirectory(path);

            ConfigManager config2 = new ConfigManager();
            assertThat(config2.getLastDirectory())
                .as("Path should be preserved: " + path)
                .isEqualTo(path);
        }
    }

    @Test
    public void testConfigFile_handlesMultipleUpdatesInSession() throws IOException {
        ConfigManager config = new ConfigManager();

        // Simulate rapid position changes as user navigates
        for (int i = 0; i < 20; i++) {
            config.setLastPosition(i);
        }

        // Config should have latest value
        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(19);

        // File should only have one position entry
        List<String> lines = Files.readAllLines(tempConfigFile);
        long positionCount = lines.stream()
            .filter(line -> line.startsWith("lastPosition="))
            .count();
        assertThat(positionCount).isEqualTo(1);
    }

    @Test
    public void testConfigFile_maintainsComments_afterMultipleUpdates() throws IOException {
        ConfigManager config = new ConfigManager();
        config.setLastDirectory("/path1");
        config.setLastPosition(1);

        // Update multiple times
        config.setLastDirectory("/path2");
        config.setLastPosition(2);
        config.setLastDirectory("/path3");
        config.setLastPosition(3);

        // Comments should still be present
        List<String> lines = Files.readAllLines(tempConfigFile);
        long commentCount = lines.stream()
            .filter(line -> line.trim().startsWith("#"))
            .count();
        assertThat(commentCount).isGreaterThanOrEqualTo(2); // At least 2 comments
    }

    @Test
    public void testConfigFile_formatStaysConsistent() throws IOException {
        ConfigManager config1 = new ConfigManager();
        config1.setLastDirectory("/initial/path");
        config1.setLastPosition(0);

        List<String> initialLines = Files.readAllLines(tempConfigFile);

        // Make multiple updates
        for (int i = 0; i < 10; i++) {
            ConfigManager config = new ConfigManager();
            config.setLastDirectory("/path/" + i);
            config.setLastPosition(i);
        }

        List<String> finalLines = Files.readAllLines(tempConfigFile);

        // File structure should remain consistent (same number of sections)
        long initialComments = initialLines.stream()
            .filter(line -> line.trim().startsWith("#"))
            .count();
        long finalComments = finalLines.stream()
            .filter(line -> line.trim().startsWith("#"))
            .count();

        assertThat(finalComments).isEqualTo(initialComments);
    }

    @Test
    public void testConfigManager_handlesUnixPaths() throws IOException {
        ConfigManager config1 = new ConfigManager();
        String unixPath = "/home/user/Documents/Photos/2024/Summer";
        config1.setLastDirectory(unixPath);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEqualTo(unixPath);
    }

    @Test
    public void testConfigManager_handlesWindowsUNCPaths() throws IOException {
        ConfigManager config1 = new ConfigManager();
        String uncPath = "\\\\server\\share\\photos\\family";
        config1.setLastDirectory(uncPath);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEqualTo(uncPath);

        // Verify escaping in file
        List<String> lines = Files.readAllLines(tempConfigFile);
        boolean hasEscapedBackslashes = lines.stream()
            .anyMatch(line -> line.contains("\\\\"));
        assertThat(hasEscapedBackslashes).isTrue();
    }

    @Test
    public void testConfigManager_emptyDirectory_handledCorrectly() throws IOException {
        ConfigManager config1 = new ConfigManager();
        config1.setLastDirectory("");

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEmpty();
    }

    @Test
    public void testConfigManager_zeroPosition_persistsCorrectly() throws IOException {
        ConfigManager config1 = new ConfigManager();
        config1.setLastPosition(0);

        // Move to different position
        config1.setLastPosition(10);

        // Back to zero
        config1.setLastPosition(0);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(0);
    }

    @Test
    public void testConfigManager_largePositionValues() throws IOException {
        ConfigManager config1 = new ConfigManager();
        int largePosition = 999999;
        config1.setLastPosition(largePosition);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(largePosition);
    }

    @Test
    public void testConfigManager_negativePosition_persistsCorrectly() throws IOException {
        // Although negative positions may not make sense in practice,
        // the config should still handle them
        ConfigManager config1 = new ConfigManager();
        config1.setLastPosition(-1);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(-1);
    }
}

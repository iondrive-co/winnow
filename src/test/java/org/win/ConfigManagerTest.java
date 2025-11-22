package org.win;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigManagerTest {

    private Path tempConfigFile;
    private String originalUserHome;

    @Before
    public void setUp() throws IOException {
        // Create a temporary directory to act as home
        Path tempDir = Files.createTempDirectory("winnow-test-home");
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
    public void testNewConfig_createsEmptyProperties() throws IOException {
        ConfigManager config = new ConfigManager();

        assertThat(config.getLastDirectory()).isNull();
        assertThat(config.getLastPosition()).isEqualTo(0);
    }

    @Test
    public void testSetLastDirectory_savesToFile() throws IOException {
        ConfigManager config = new ConfigManager();
        String testPath = "/path/to/images";

        config.setLastDirectory(testPath);

        assertThat(Files.exists(tempConfigFile)).isTrue();
        List<String> lines = Files.readAllLines(tempConfigFile);
        assertThat(lines).anyMatch(line -> line.contains("lastDirectory=" + testPath));
    }

    @Test
    public void testSetLastDirectory_persistsAcrossInstances() throws IOException {
        ConfigManager config1 = new ConfigManager();
        String testPath = "/path/to/images";
        config1.setLastDirectory(testPath);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEqualTo(testPath);
    }

    @Test
    public void testSetLastDirectory_windowsPath_escapesBackslashes() throws IOException {
        ConfigManager config = new ConfigManager();
        String windowsPath = "C:\\Users\\test\\Documents\\Images";

        config.setLastDirectory(windowsPath);

        // Read file directly to check escaping
        List<String> lines = Files.readAllLines(tempConfigFile);
        assertThat(lines).anyMatch(line -> line.contains("lastDirectory=C:\\\\Users\\\\test\\\\Documents\\\\Images"));

        // Verify it loads back correctly
        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEqualTo(windowsPath);
    }

    @Test
    public void testSetLastPosition_savesToFile() throws IOException {
        ConfigManager config = new ConfigManager();
        int testPosition = 42;

        config.setLastPosition(testPosition);

        assertThat(Files.exists(tempConfigFile)).isTrue();
        List<String> lines = Files.readAllLines(tempConfigFile);
        assertThat(lines).anyMatch(line -> line.contains("lastPosition=" + testPosition));
    }

    @Test
    public void testSetLastPosition_persistsAcrossInstances() throws IOException {
        ConfigManager config1 = new ConfigManager();
        config1.setLastPosition(15);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(15);
    }

    @Test
    public void testGetLastPosition_defaultsToZero_whenNotSet() throws IOException {
        ConfigManager config = new ConfigManager();
        assertThat(config.getLastPosition()).isEqualTo(0);
    }

    @Test
    public void testGetLastPosition_defaultsToZero_whenInvalid() throws IOException {
        // Manually write invalid config
        Files.write(tempConfigFile, "lastPosition=invalid".getBytes());

        ConfigManager config = new ConfigManager();
        assertThat(config.getLastPosition()).isEqualTo(0);
    }

    @Test
    public void testGetLastPosition_handlesNegativeValues() throws IOException {
        ConfigManager config1 = new ConfigManager();
        config1.setLastPosition(-5);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(-5);
    }

    @Test
    public void testConfigFile_includesComments() throws IOException {
        ConfigManager config = new ConfigManager();
        config.setLastDirectory("/test/path");
        config.setLastPosition(10);

        List<String> lines = Files.readAllLines(tempConfigFile);

        assertThat(lines).anyMatch(line -> line.contains("# The last directory that was opened in Winnow"));
        assertThat(lines).anyMatch(line -> line.contains("# The last image position (index) in the directory"));
    }

    @Test
    public void testConfigFile_hasCorrectFormat() throws IOException {
        ConfigManager config = new ConfigManager();
        config.setLastDirectory("/test/path");
        config.setLastPosition(5);

        List<String> lines = Files.readAllLines(tempConfigFile);

        // Check that comments come before their respective properties
        int lastDirCommentIndex = -1;
        int lastDirPropertyIndex = -1;
        int lastPosCommentIndex = -1;
        int lastPosPropertyIndex = -1;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("# The last directory that was opened")) {
                lastDirCommentIndex = i;
            } else if (line.startsWith("lastDirectory=")) {
                lastDirPropertyIndex = i;
            } else if (line.contains("# The last image position")) {
                lastPosCommentIndex = i;
            } else if (line.startsWith("lastPosition=")) {
                lastPosPropertyIndex = i;
            }
        }

        assertThat(lastDirCommentIndex).isLessThan(lastDirPropertyIndex);
        assertThat(lastPosCommentIndex).isLessThan(lastPosPropertyIndex);
        assertThat(lastDirPropertyIndex).isLessThan(lastPosCommentIndex);
    }

    @Test
    public void testSetLastDirectory_updatesExistingValue() throws IOException {
        ConfigManager config = new ConfigManager();
        config.setLastDirectory("/first/path");
        config.setLastDirectory("/second/path");

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEqualTo("/second/path");

        // Verify only one lastDirectory entry exists
        List<String> lines = Files.readAllLines(tempConfigFile);
        long count = lines.stream().filter(line -> line.startsWith("lastDirectory=")).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testSetBothProperties_maintainsBothValues() throws IOException {
        ConfigManager config = new ConfigManager();
        String testDir = "/images/vacation";
        int testPos = 25;

        config.setLastDirectory(testDir);
        config.setLastPosition(testPos);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEqualTo(testDir);
        assertThat(config2.getLastPosition()).isEqualTo(testPos);
    }

    @Test
    public void testSetLastPosition_updatesExistingValue() throws IOException {
        ConfigManager config = new ConfigManager();
        config.setLastPosition(10);
        config.setLastPosition(20);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(20);

        // Verify only one lastPosition entry exists
        List<String> lines = Files.readAllLines(tempConfigFile);
        long count = lines.stream().filter(line -> line.startsWith("lastPosition=")).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testEscapePropertyValue_handlesSpecialCharacters() throws IOException {
        ConfigManager config = new ConfigManager();

        // Test path with various special characters
        String pathWithBackslashes = "C:\\Program Files\\Winnow\\images";
        config.setLastDirectory(pathWithBackslashes);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastDirectory()).isEqualTo(pathWithBackslashes);
    }

    @Test
    public void testConfigFile_locatedInUserHome() throws IOException {
        ConfigManager config = new ConfigManager();
        config.setLastDirectory("/test");

        Path expectedPath = Paths.get(System.getProperty("user.home"), ".winnow.conf");
        assertThat(Files.exists(expectedPath)).isTrue();
        assertThat(expectedPath).isEqualTo(tempConfigFile);
    }

    @Test
    public void testGetLastPosition_handlesZeroValue() throws IOException {
        ConfigManager config1 = new ConfigManager();
        config1.setLastPosition(0);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(0);
    }

    @Test
    public void testGetLastPosition_handlesLargeValues() throws IOException {
        ConfigManager config1 = new ConfigManager();
        int largePosition = Integer.MAX_VALUE;
        config1.setLastPosition(largePosition);

        ConfigManager config2 = new ConfigManager();
        assertThat(config2.getLastPosition()).isEqualTo(largePosition);
    }
}

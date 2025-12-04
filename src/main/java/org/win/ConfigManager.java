package org.win;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages application configuration, storing settings in the user's home directory.
 */
public class ConfigManager {
    private static final String CONFIG_FILE = ".winnow.conf";
    private static final String LAST_DIRECTORY_KEY = "lastDirectory";
    private static final String LAST_POSITION_KEY = "lastPosition";
    private static final String USE_SIMPLE_FILENAME_EDITOR_KEY = "useSimpleFilenameEditor";

    private final Path configPath;
    private final Properties properties;

    public ConfigManager() throws IOException {
        final String overrideHome = System.getProperty("winnow.configHome");
        final String home = (overrideHome != null && !overrideHome.isBlank())
                ? overrideHome
                : System.getProperty("user.home");

        Path homeDir = Paths.get(home);
        this.configPath = homeDir.resolve(CONFIG_FILE);
        this.properties = new Properties();

        // Load existing config if it exists
        if (Files.exists(configPath)) {
            try (InputStream input = Files.newInputStream(configPath)) {
                properties.load(input);
            }
        }
    }

    /**
     * Gets the last used directory path, or null if none is saved.
     */
    public String getLastDirectory() {
        return properties.getProperty(LAST_DIRECTORY_KEY);
    }

    /**
     * Saves the directory path to the configuration file.
     */
    public void setLastDirectory(String directoryPath) throws IOException {
        properties.setProperty(LAST_DIRECTORY_KEY, directoryPath);
        save();
    }

    /**
     * Gets the last image position in the directory, or 0 if none is saved.
     */
    public int getLastPosition() {
        String position = properties.getProperty(LAST_POSITION_KEY);
        if (position != null) {
            try {
                return Integer.parseInt(position);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Saves the image position to the configuration file.
     */
    public void setLastPosition(int position) throws IOException {
        properties.setProperty(LAST_POSITION_KEY, String.valueOf(position));
        save();
    }

    /**
     * Gets whether to use simple filename editor instead of combo box classifier.
     * Defaults to false (use combo box classifier).
     */
    public boolean isUseSimpleFilenameEditor() {
        final String value = properties.getProperty(USE_SIMPLE_FILENAME_EDITOR_KEY);
        return value != null && Boolean.parseBoolean(value);
    }

    /**
     * Sets whether to use simple filename editor instead of combo box classifier.
     */
    public void setUseSimpleFilenameEditor(boolean useSimple) throws IOException {
        properties.setProperty(USE_SIMPLE_FILENAME_EDITOR_KEY, String.valueOf(useSimple));
        save();
    }

    private void save() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            writer.write("# The last directory that was opened in Winnow\n");
            if (properties.containsKey(LAST_DIRECTORY_KEY)) {
                writer.write(LAST_DIRECTORY_KEY + "=" + escapePropertyValue(properties.getProperty(LAST_DIRECTORY_KEY)) + "\n");
            }
            writer.write("\n");
            writer.write("# The last image position (index) in the directory\n");
            if (properties.containsKey(LAST_POSITION_KEY)) {
                writer.write(LAST_POSITION_KEY + "=" + escapePropertyValue(properties.getProperty(LAST_POSITION_KEY)) + "\n");
            }
            writer.write("\n");
            writer.write("# Use simple filename editor (true) or combo box classifier (false)\n");
            if (properties.containsKey(USE_SIMPLE_FILENAME_EDITOR_KEY)) {
                writer.write(USE_SIMPLE_FILENAME_EDITOR_KEY + "=" + escapePropertyValue(properties.getProperty(USE_SIMPLE_FILENAME_EDITOR_KEY)) + "\n");
            }
        }
    }

    /**
     * Escapes special characters in property values for the Java Properties format.
     * Backslashes must be escaped so they're not interpreted as escape sequences.
     */
    private String escapePropertyValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}

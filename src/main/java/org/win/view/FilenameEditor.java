package org.win.view;

/**
 * Interface for filename editor components.
 */
public interface FilenameEditor {
    /**
     * Gets the complete filename with extension.
     *
     * @return the filename with extension
     */
    String getFilename();

    /**
     * Requests focus on the primary input field.
     */
    void requestFocus();
}

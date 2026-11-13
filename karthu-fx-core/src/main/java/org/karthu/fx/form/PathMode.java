package org.karthu.fx.form;

/**
 * What a {@link PathInput} chooses: a file to open, a file to save, or a directory. The mode decides the dialog the
 * browse icon opens, the default prompt text and where the dialog starts.
 */
public enum PathMode {

    /** Open an existing file: the dialog for opening a file. */
    OPEN,

    /** Save a file, which may not exist yet: the dialog for saving a file. */
    SAVE,

    /** Choose a directory: the dialog for choosing a folder. */
    DIRECTORY
}

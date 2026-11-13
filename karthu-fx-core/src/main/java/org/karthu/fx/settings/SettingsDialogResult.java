package org.karthu.fx.settings;

/**
 * Result of a {@link SettingsDialog} interaction.
 */
public enum SettingsDialogResult {

    /** User clicked OK — edited values are committed. */
    OK,

    /** User clicked Cancel — values have been reverted to the pre-dialog state. */
    CANCEL
}

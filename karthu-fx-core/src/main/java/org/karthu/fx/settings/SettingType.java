package org.karthu.fx.settings;

/**
 * Defines the data type of a setting entry, which determines the UI control
 * rendered in the settings dialog.
 */
public enum SettingType {

    /** Plain text input — rendered as {@code TextField}. */
    STRING,

    /** On/off toggle — rendered as {@code CheckBox}. */
    BOOLEAN,

    /** Numeric input with bounds — rendered as {@code Spinner<Integer>}. */
    INTEGER,

    /** Single selection from a fixed list — rendered as {@code ComboBox<String>}. */
    CHOICE,

    /** Masked text input — rendered as {@code PasswordField}. */
    SECRET,

    /** Date input — rendered as {@code DatePicker}. */
    DATETIME,

    /**
     * A file or directory path — rendered as {@link org.karthu.fx.form.PathInput}, with its <i>Browse</i> icon, in
     * the entry's {@linkplain SettingEntry#getPathMode() mode} and with its
     * {@linkplain SettingEntry#getPathFilters() filters}.
     *
     * @since 1.1.0-RC2
     */
    PATH
}
